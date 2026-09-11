# F4 — Infraestrutura SSE

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F4 |
| Perfil | Infraestrutura (Fundação) — base para HU08 (e, opcionalmente, HU06/HU07) |
| Prioridade MoSCoW | Must have |
| Dependências | F1 (Harness) |
| Referência no TCC | Cap. 6.1 "Arquitetura Macro" — Figura 7 (bloco `SSE_Engine`) |

## 2. Objetivo

Como desenvolvedor implementando HU08, quero um motor de eventos SSE
genérico e reutilizável no backend, para que a atualização em tempo real
da tela de acompanhamento do cliente (e, futuramente, do dashboard do
fornecedor) seja apenas "publicar um evento num canal", sem reimplementar
gerenciamento de conexão a cada história.

## 3. Divergência/decisão técnica não coberta pelo TCC

O TCC descreve o mecanismo SSE na visão macro (Figura 7), mas não trata um
problema prático conhecido de SSE via `EventSource` nativo do navegador:
**a API `EventSource` não permite enviar headers customizados**, então o
JWT (hoje lido exclusivamente do header `Authorization: Bearer` em
`SecurityFilter.java`) não pode ser anexado por essa via. A solução
adotada aqui — token via query string apenas para o endpoint de SSE — é
uma decisão de engenharia preenchendo essa lacuna, documentada porque
altera (pontualmente) o filtro de segurança global.

## 4. Critérios de Aceitação (BDD)

- **Cenário 1 — Inscrição em canal**: Dado um cliente autenticado, Quando
  ele abre uma conexão `GET /api/sse/subscribe/{canal}`, Então recebe uma
  resposta `text/event-stream` mantida aberta (sem timeout automático
  antes de 30 minutos de inatividade).

- **Cenário 2 — Publicação de evento**: Dado um canal com pelo menos um
  assinante conectado, Quando o backend chama
  `sseEmitterService.publish(canal, evento, payload)`, Então todos os
  emitters daquele canal recebem o evento no mesmo instante.

- **Cenário 3 — Limpeza de conexão morta**: Dado um assinante que
  desconecta (fecha aba, perde rede), Quando o `SseEmitter` dispara
  `onCompletion`/`onTimeout`/`onError`, Então ele é removido do registro
  interno, evitando vazamento de memória em conexões acumuladas.

- **Cenário 4 — Autenticação compatível com `EventSource`**: Dado que o
  navegador não envia header customizado em `EventSource`, Quando a
  requisição de subscribe chega com `?token=<jwt>` na query string (sem
  header `Authorization`), Então o filtro de segurança aceita esse token
  como equivalente, exclusivamente para rotas `/api/sse/**`.

- **Cenário 5 — Heartbeat**: Dado uma conexão SSE aberta sem eventos de
  negócio, Quando 15 segundos se passam, Então o servidor envia um
  comentário SSE vazio (`:heartbeat\n\n`) para evitar que proxies/load
  balancers encerrem a conexão por inatividade.

## 5. Contrato Técnico

### 5.1 Novo pacote `com.h2gas.sse`

**`SseEmitterService.java`** — componente central (o "SSE_Engine" da
Figura 7):

```java
@Component
public class SseEmitterService {
    private final Map<String, List<SseEmitter>> emittersByChannel = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String channel) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 min
        emittersByChannel.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> emittersByChannel.getOrDefault(channel, List.of()).remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    public void publish(String channel, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByChannel.getOrDefault(channel, List.of());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
}
```

**`SseHeartbeatScheduler.java`** — envia heartbeat periódico:

```java
@Component
public class SseHeartbeatScheduler {
    @Autowired
    private SseEmitterService sseEmitterService;

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        sseEmitterService.publishHeartbeatToAllChannels(); // implementação envia comentário SSE vazio
    }
}
```

Requer `@EnableScheduling` na classe de configuração principal (verificar
se já existe — se não, adicionar em `BackendApplication.java` ou classe de
config dedicada).

**`SseController.java`** (`com.h2gas.controller`):

```java
@RestController
@RequestMapping("/api/sse")
public class SseController {
    @Autowired
    private SseEmitterService sseEmitterService;

    @GetMapping(value = "/subscribe/{channel}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String channel) {
        return sseEmitterService.subscribe(channel);
    }
}
```

Convenção de nome de canal (fixada aqui para HU08 usar depois):
`order:{orderId}` para acompanhamento de um pedido específico.

### 5.2 Ajuste de segurança (`SecurityFilter.java`)

```java
private String recoverToken(HttpServletRequest request) {
    var authHeader = request.getHeader("Authorization");
    if (authHeader != null) {
        return authHeader.replace("Bearer ", "");
    }
    // EventSource nativo não envia headers customizados — fallback por query param,
    // restrito às rotas de SSE.
    if (request.getRequestURI().startsWith("/h2gas/api/sse/")) {
        return request.getParameter("token");
    }
    return null;
}
```

`SecurityConfigurations.java`: nenhuma rota de SSE deve ser `permitAll` —
o token continua obrigatório, só muda o transporte (query em vez de
header), exclusivamente para `/api/sse/**`.

## 6. Plano de Testes (TDD)

1. `SseEmitterServiceTest` — `deveRegistrarEmitterAoInscrever`,
   `devePublicarEventoParaTodosOsEmittersDoCanal` (Mockito spy em
   `SseEmitter` para verificar `send()` chamado com o payload certo),
   `deveRemoverEmitterAoCompletar` (simula `onCompletion` e verifica que o
   canal fica vazio).
2. `SseControllerTest` (`@WebMvcTest` ou `MockMvc` com
   `asyncDispatch`) — `deveRetornarContentTypeEventStream` ao chamar
   `GET /api/sse/subscribe/order:123`.
3. `SecurityFilterTest` (novo ou estendendo teste de segurança existente)
   — `deveAceitarTokenViaQueryParamApenasParaRotaSse` e
   `deveRejeitarQueryParamParaRotaQueNaoESse` (garante que o fallback não
   vaza para outros endpoints).

## 7. Fora de Escopo

- Qualquer chamada real a `sseEmitterService.publish(...)` a partir de
  `OrderController`/`OrderService` — isso é o "disparo do evento
  logístico" que pertence à HU08 (e opcionalmente HU06/HU07).
- Frontend (`EventSource` no Angular) — também pertence à HU08.
- Autenticação alternativa via WebSocket ou long-polling — o TCC define
  SSE explicitamente, não há necessidade de suporte a outro transporte.
- Escalonamento horizontal (múltiplas instâncias do backend
  compartilhando estado de emitters via Redis pub/sub, por exemplo) — fora
  do escopo de um TCC de uma única instância; documentado como limitação
  conhecida, não resolvido.

## 8. Definição de Pronto

- [ ] `SseEmitterService`, `SseHeartbeatScheduler`, `SseController`
      criados em `com.h2gas.sse` / `com.h2gas.controller`.
- [ ] `SecurityFilter.recoverToken` aceita query param `token` apenas para
      `/api/sse/**`.
- [ ] `@EnableScheduling` habilitado no projeto.
- [ ] Todos os testes da seção 6 passando.
- [ ] Testado manualmente com `curl -N` (ou navegador) que a conexão
      permanece aberta e recebe heartbeat a cada 15s.
- [ ] Revisado e aprovado antes de F5 começar.
