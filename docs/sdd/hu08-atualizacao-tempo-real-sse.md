# HU08 — Atualização em Tempo Real via SSE

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU08 |
| Perfil | Cliente (CLIENT) |
| Prioridade MoSCoW | Must have |
| Dependências | F4 (motor SSE genérico), HU06/HU07 (pontos onde o status do pedido muda) |
| Referência no TCC | Apêndice A.1 — HU08 |

## 2. História de Usuário

Como cliente, quero que a tela do meu pedido atualize automaticamente
sem precisar recarregá-la, para acompanhar a evolução da entrega de
forma instantânea.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Sincronismo**: Dado a página de acompanhamento aberta,
  Quando há mudança no status no back-end, Então a tela do cliente
  atualiza instantaneamente via SSE, sem refresh manual.

Este é o único cenário oficial da HU08 no Apêndice A.

## 4. Estado atual e refinamento da convenção do F4

- Não existe "página de acompanhamento de um pedido" isolada — o
  acompanhamento acontece dentro de `order-list.component.ts` (rota
  `meus-pedidos`), que lista **vários** pedidos simultaneamente, cada um
  expansível via `toggleAccordion`. "A página de acompanhamento aberta"
  do Cenário 1 é essa tela.
- O F4 fixou a convenção de canal `order:{orderId}` (um canal por
  pedido). Como o cliente pode ter vários pedidos ativos visíveis ao
  mesmo tempo na mesma tela, abrir uma conexão `EventSource` por pedido
  seria desperdício de conexões e mais frágil de limpar. **Refinamento
  adotado aqui**: um único canal por **cliente** —
  `customer:{customerId}` — carregando o `orderId` afetado no payload do
  evento. O front-end filtra localmente qual card atualizar. Isso é uma
  revisão da convenção do F4, feita agora que o caso de uso real
  (múltiplos pedidos na mesma tela) ficou claro.
- Os pontos que mudam `Order.status` hoje estão espalhados: o
  `updateOrderStatus` genérico (já existente), o `cancelOrder` (HU06) e o
  `assignDeliverer`/`completeDelivery` (HU07). Cada um precisa disparar o
  evento — centralizado num único método auxiliar para não duplicar a
  lógica de publicação em 4 lugares.

## 5. Contrato Técnico

### 5.1 Backend

**`OrderService.java`** — novo método privado, chamado ao final de toda
mudança de status:
```java
@Autowired
private SseEmitterService sseEmitterService;

private void notifyStatusChange(Order order) {
    sseEmitterService.publish(
        "customer:" + order.getCustomer().getId(),
        "order-status-changed",
        Map.of("orderId", order.getId(), "status", order.getStatus().getStatus())
    );
}
```

Chamar `notifyStatusChange(order)` ao final de:
- `updateOrderStatus` (já existente).
- `cancelOrder` (HU06).
- `assignDeliverer` e `completeDelivery` (HU07).

Nenhuma mudança é necessária em `SseEmitterService`/`SseController`
(F4) — só no uso do canal, já genérico por design.

### 5.2 Frontend

**Novo serviço `OrderTrackingService`** (`services/order-tracking.service.ts`):
```ts
@Injectable({ providedIn: 'root' })
export class OrderTrackingService {
  private eventSource?: EventSource;
  private statusChange$ = new Subject<{ orderId: string; status: string }>();
  readonly onStatusChange = this.statusChange$.asObservable();

  connect(customerId: string, token: string) {
    this.disconnect();
    this.eventSource = new EventSource(
      `${environment.apiUrl}/sse/subscribe/customer:${customerId}?token=${token}`
    );
    this.eventSource.addEventListener('order-status-changed', (event: MessageEvent) => {
      this.statusChange$.next(JSON.parse(event.data));
    });
  }

  disconnect() {
    this.eventSource?.close();
    this.eventSource = undefined;
  }
}
```
(o token via query param usa o fallback de segurança já implementado no
F4, exclusivo para `/api/sse/**`.)

**`order-list.component.ts`** — conecta ao entrar na tela, atualiza o
card correspondente sem refetch completo, desconecta ao sair:
```ts
ngOnInit(): void {
  this.userLogado = this.userService.getUserLocalStorage();
  this.getOrders();

  this.orderTracking.connect(this.userLogado!.id!, this.authService.getToken());
  this.orderTracking.onStatusChange.subscribe(({ orderId, status }) => {
    const order = this.orders?.find(o => o.id === orderId);
    if (order) order.status = status;
  });
}

ngOnDestroy(): void {
  this.orderTracking.disconnect();
}
```

## 6. Plano de Testes (TDD)

**Backend:**
1. `OrderServiceTest` — `devePublicarEventoSseAoAtualizarStatus`,
   `devePublicarEventoSseAoCancelar`,
   `devePublicarEventoSseAoAtribuirEntregador`,
   `devePublicarEventoSseAoConcluirEntrega` (mock de `SseEmitterService`,
   verifica canal `customer:{id}` e payload corretos em cada ponto de
   mudança de status).

**Frontend:**
2. `order-tracking.service.spec.ts` —
   `deveAbrirEventSourceComCanalDoCliente`,
   `deveEmitirEventoQuandoStatusMudar`,
   `deveFecharConexaoAnteriorAoReconectar`.
3. `order-list.component.spec.ts` —
   `deveAtualizarStatusDoPedidoEmTempoRealSemRefetchCompleto`,
   `deveFecharEventSourceAoDestruirComponente` (regressão do vazamento de
   conexão coberto pelo F4).

## 7. Fora de Escopo

- Notificar outras telas (dashboard do fornecedor, rotas do entregador)
  — só a tela do cliente tem esse critério de aceitação; extensão
  natural futura, não parte desta história.
- Reconexão automática após queda de rede — a API `EventSource` nativa
  já reconecta sozinha por padrão; não precisa de lógica adicional.
- Recuperar eventos perdidos enquanto a conexão estava fechada — se o
  cliente reabrir a tela depois de ficar offline, o `getOrders()` normal
  (já existente) resolve via busca completa; não há backfill de eventos
  SSE perdidos.

## 8. Definição de Pronto

- [ ] Toda mudança de status de pedido (`updateOrderStatus`,
      `cancelOrder`, `assignDeliverer`, `completeDelivery`) publica
      evento SSE no canal `customer:{id}`.
- [ ] `order-list` atualiza o status do card afetado sem recarregar a
      página nem refazer a busca completa.
- [ ] Conexão SSE é fechada corretamente ao sair da tela.
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado — fecha os 10 Must-have (HU01-HU10) e a
      Fundação (F1-F5). Próximo é HU11, início do Should-have.
