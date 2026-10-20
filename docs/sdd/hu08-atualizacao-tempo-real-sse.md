# HU08 — Atualização em Tempo Real via SSE

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU08 |
| Perfil | Cliente (CLIENT) |
| Prioridade MoSCoW | Must have |
| Dependências | F4 (motor SSE genérico), HU06/HU07 (pontos onde o status do pedido muda), F2 (tokens) |
| Referência no TCC | Apêndice A.1 — HU08 |

## 2. História de Usuário

Como cliente, quero que a tela do meu pedido atualize automaticamente
sem precisar recarregá-la, para acompanhar a evolução da entrega de
forma instantânea.

## 3. Critérios de Aceitação (BDD)

> Revisado em 2026-10-18 sobre a `develop` com HU01–HU07 mergeadas. A
> versão anterior foi escrita antes da HU07 mudar o fluxo: o despacho
> (`assignDeliverer`) não muda mais o status, e surgiram `startDelivery`
> e `cancelDelivery`. Achados da revisão: (1) qualquer usuário logado
> assina qualquer canal no F4 — com o canal por cliente, isso vazaria os
> pedidos de outro cliente; (2) publicar dentro da transação pode
> anunciar um status que sofre rollback; (3) o nginx de produção não
> repassa stream (buffer ligado). Decisões do autor: (a) a **recusa do
> entregador não é publicada** (o pedido volta a "Em preparação" sem
> aviso em tempo real; o cliente vê ao recarregar — coerente com a
> decisão (d) da HU07 de não avisar o cliente da recusa); (b) além do
> badge, o card atualizado ganha um **destaque rápido**, sem toast.

- **Cenário 1 — Sincronismo**: Dado a página de acompanhamento aberta,
  Quando há mudança no status no back-end, Então a tela do cliente
  atualiza instantaneamente via SSE, sem refresh manual.

Este é o único cenário oficial da HU08 no Apêndice A. Detalhamento
operacional (não cria cenário novo):

- **1a — Quais mudanças**: confirmar/preparar/pronto para retirada/
  entregue na retirada (`updateOrderStatus`), cancelamento pelo
  fornecedor (`cancelOrder`), entregador iniciou (`startDelivery` → "Saiu
  para entrega") e entregou (`completeDelivery` → "Entregue"). Despachar
  (só atribui) e a recusa do entregador **não** publicam.
- **1b — Privacidade**: o cliente só recebe eventos dos próprios pedidos;
  assinar o canal de outro cliente (ou canal fora da convenção) é 403.
- **1c — Na tela**: o badge do card troca e o card pisca um destaque
  curto; nada é rebuscado do servidor. Evento de pedido que não está na
  página atual é ignorado.

## 4. Estado atual (achados)

- O acompanhamento do cliente é `order-list` (rota `meus-pedidos`, só
  `CUSTOMER`/`ADMIN`): lista **paginada** (5 por página) com filtro por
  status; não existe página de um pedido isolado. "A página de
  acompanhamento aberta" do Cenário 1 é essa tela.
- F4 (na `develop`): `SseEmitterService.subscribe/publish` por canal,
  heartbeat agendado, `SseController` em `GET /api/sse/subscribe/{channel}`
  e o `SecurityFilter` aceita `?token=` só em `/h2gas/api/sse/**` (o
  `EventSource` nativo não manda header). Ninguém usa SSE ainda; o
  controller documenta a convenção antiga `order:{orderId}`.
- **Canal por cliente**: como a tela mostra vários pedidos, um
  `EventSource` por pedido seria desperdício; adota-se um canal por
  cliente, `customer:{customerId}`, com o `orderId` no payload — revisão
  da convenção do F4 (`order:{orderId}` deixa de existir).
- **Sem checagem de dono no canal** (achado 1): `subscribe` aceita
  qualquer `channel` de qualquer usuário autenticado.
- **Pontos que mudam `Order.status` hoje** (`OrderService`):
  `updateOrderStatus` (genérico, **sem** `@Transactional`), `cancelOrder`
  (HU06), `startDelivery`, `cancelDelivery` e `completeDelivery` (HU07).
  `createOrder` nasce `PENDING` (o próprio cliente criou; não publica).
  `assignDeliverer` não muda status desde a 2ª revisão da HU07.
- `OrderServiceTest` é `@SpringBootTest` + `@Transactional` (rollback):
  um listener `AFTER_COMMIT` nunca dispara nele — por isso o teste do
  serviço verifica o **evento de domínio** publicado, e o envio SSE é
  testado no listener isoladamente.
- `frontend/nginx.conf`: `location /h2gas-api/api` sem
  `proxy_buffering off`, sem HTTP/1.1 persistente e com timeout padrão de
  60 s (achado 3).

## 5. Contrato Técnico

### 5.1 Backend — evento de domínio e publicação após o commit

- `events/OrderStatusChangedEvent` (record): `orderId`, `customerId`,
  `status` (rótulo de `OrderStatusEnum.getStatus()`, o mesmo que o
  `OrderResponseDTO.status` devolve, ex.: "Saiu para entrega").
- `OrderService` recebe `ApplicationEventPublisher` (injeção por
  construtor) e chama um único auxiliar `publishStatusChange(order)` ao
  final de `updateOrderStatus`, `cancelOrder`, `startDelivery` e
  `completeDelivery`. **Não** chamar em `cancelDelivery` (decisão (a)),
  `assignDeliverer` nem `createOrder`. `updateOrderStatus` passa a ser
  `@Transactional`.
- `sse/OrderStatusSseListener`: `@TransactionalEventListener(phase =
  AFTER_COMMIT, fallbackExecution = true)` → `SseEmitterService.publish(
  "customer:" + customerId, "order-status-changed", Map.of("orderId", …,
  "status", …))`. Sem commit, sem evento (achado 2).

### 5.2 Backend — dono do canal

- `SseController.subscribe` recebe o usuário autenticado e só aceita
  `customer:{id}` com `id` igual ao do usuário do token; qualquer outro
  canal (de outro cliente ou fora da convenção) → **403**, sem abrir o
  stream. Sem token → 401 (já é assim).
- OpenAPI: descrição e exemplo do canal passam a `customer:{customerId}`;
  `@ApiResponse 403`.
- `SseEmitterService` não muda.

### 5.3 Frontend

- `services/order-tracking.service.ts` (`providedIn: 'root'`):
  `connect(customerId)` fecha a conexão anterior e abre
  `new EventSource("<apiUrl>/sse/subscribe/customer:<id>?token=<jwt>")`
  (token do `AuthService.getToken()`); escuta `order-status-changed` e
  emite `{ orderId, status }` em `onStatusChange`; `disconnect()` fecha.
  A criação do `EventSource` fica num método próprio para o spec
  substituir por um falso.
- `order-list`: conecta ao abrir a tela (`userLogado.id`); a cada evento,
  se o pedido estiver na página atual troca `order.status` e marca o card
  com destaque (classe com animação curta, cores dos tokens F2) por ~2 s;
  pedido fora da página é ignorado; **não** chama `getOrders()` de novo.
  Com filtro ativo, o card continua visível com o status novo até a
  próxima busca (sem refetch, por 1c). `ngOnDestroy` desconecta e
  cancela a inscrição.

### 5.4 Infra

- `frontend/nginx.conf`: `location /h2gas-api/api/sse` antes da geral,
  com `proxy_http_version 1.1`, `proxy_set_header Connection ""`,
  `proxy_buffering off`, `proxy_cache off` e `proxy_read_timeout 1h`
  (acima do heartbeat e do timeout de 30 min do emitter).

## 6. Plano de Testes (TDD)

**Backend:**
1. `OrderServiceTest` (`@RecordApplicationEvents`) —
   `devePublicarEventoAoAtualizarStatus`,
   `devePublicarEventoAoCancelarPedido`,
   `devePublicarEventoAoIniciarEntrega`,
   `devePublicarEventoAoConcluirEntrega`,
   `naoDevePublicarEventoAoRecusarEntrega`,
   `naoDevePublicarEventoAoAtribuirEntregador`.
2. `OrderStatusSseListenerTest` —
   `devePublicarNoCanalDoClienteComOrderIdEStatus`.
3. `SseControllerTest` — `devePermitirAssinarCanalDoProprioCliente`
   (substitui o teste com `order:123`),
   `deveRetornar403AoAssinarCanalDeOutroCliente`,
   `deveRetornar403ParaCanalForaDaConvencao`.

**Frontend:**
4. `order-tracking.service.spec.ts` —
   `deveAbrirEventSourceComCanalDoClienteEToken`,
   `deveEmitirEventoQuandoStatusMudar`,
   `deveFecharConexaoAnteriorAoReconectar`,
   `deveFecharConexaoAoDesconectar`.
5. `order-list.component.spec.ts` —
   `deveConectarAoCanalDoClienteAoAbrirATela`,
   `deveAtualizarStatusDoPedidoEmTempoRealSemRefetchCompleto`,
   `deveDestacarCardAtualizado`,
   `deveIgnorarEventoDePedidoForaDaPagina`,
   `deveFecharEventSourceAoDestruirComponente`.

Regra herdada: specs antigos falhando no frontend não aumentam (22).

## 7. Fora de Escopo

- Notificar outras telas (dashboard/painel do fornecedor, rotas do
  entregador): só a tela do cliente tem critério de aceitação.
- Aviso em tempo real da recusa do entregador (decisão (a)).
- Toast/notificação push ao mudar o status (decisão (b): só destaque).
- Reconexão customizada: o `EventSource` nativo já reconecta. Token
  expirado durante a conexão faz a reconexão receber 401 até o cliente
  reabrir a tela/logar de novo — sem renovação automática aqui.
- Backfill de eventos perdidos offline: o `getOrders()` ao abrir a tela
  resolve.
- Remover o pedido do card/lista quando o status novo sai do filtro
  ativo (resolvido na próxima busca).

## 8. Definição de Pronto

- [x] `updateOrderStatus`, `cancelOrder`, `startDelivery` e
      `completeDelivery` publicam, **após o commit**, `order-status-changed`
      no canal `customer:{id}`; recusa e despacho não publicam.
- [x] Assinar canal de outro cliente ou fora da convenção devolve 403;
      Swagger do SSE atualizado.
- [x] `order-list` troca o status e destaca o card afetado sem recarregar
      a página nem refazer a busca; conexão fechada ao sair da tela.
- [x] nginx de produção repassa o stream sem buffer.
- [x] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 22 falhas
      antigas).
- [x] Testado de ponta a ponta (cliente com a tela aberta enquanto o
      fornecedor e o entregador mudam o status).
- [x] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [ ] Revisado e aprovado — fecha os 10 Must-have (HU01-HU10) e a
      Fundação (F1-F5). Próximo é HU11, início do Should-have.

> Verificado em 2026-10-19: backend 187 testes (único erro: `BackendApplicationTests.contextLoads`, que precisa do MySQL local);
> frontend 229 testes com as 22 falhas antigas; E2E com o cliente em `/meus-pedidos` enquanto fornecedor e entregador mudavam o status
> (16 checagens: troca em ~40 ms sem refresh nem nova busca, destaque do card, despacho e recusa sem evento, canal alheio 403).
