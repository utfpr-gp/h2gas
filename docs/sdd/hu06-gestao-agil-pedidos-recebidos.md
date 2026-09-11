# HU06 — Gestão Ágil de Pedidos Recebidos

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU06 |
| Perfil | Fornecedor (SUPPLIER) |
| Prioridade MoSCoW | Must have |
| Dependências | Nenhuma nova |
| Referência no TCC | Apêndice A.1 — HU06 |

> **Nota de correção**: a versão anterior deste script tinha um Cenário 2
> fabricado ("confirmação ágil com 1 clique") que na verdade é só uma
> repetição de parte do Cenário 1 real. O Cenário 2 **de verdade** é sobre
> **cancelamento com justificativa obrigatória** — uma funcionalidade que
> eu não tinha desenhado. Reescrito abaixo com o texto real do Apêndice A
> (pág. 52).

## 2. História de Usuário

Como fornecedor, quero visualizar os novos pedidos recebidos em uma
interface de fila dinâmica, para confirmar rapidamente a solicitação do
cliente antes de repassá-la ao entregador.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Aceite na fila**: Dado o painel administrativo ativo,
  Quando entra um pedido `PENDING`, Então o sistema emite alerta visual
  **e** permite mudar para "Confirmado" num clique.
- **Cenário 2 — Cancelamento**: Dado a impossibilidade de atendimento,
  Quando o fornecedor aciona o cancelamento, Então exige-se uma
  justificativa antes de efetuar o estorno e avisar o cliente.

## 4. Estado atual (achados)

**Cenário 1 — já quase pronto:**
- A rota `dashboard-fornecedor` → `ReceivedOrdersComponent` **já é** o
  painel correto (não confundir com `historico-pedidos` →
  `OrderHistoryComponent`, tela separada só para pedidos
  `DELIVERED`/`CANCELED` — as duas existem de propósito).
- Já carrega por padrão todos os pedidos exceto `DELIVERED`/`CANCELED`
  (inclui `PENDING`), ordenado por `createdAt DESC` — já testado em
  `OrderServiceTest`.
- A transição PENDING→CONFIRMED em um clique **já existe e já é
  testada** (`changeStatus()` → `PATCH /update-order-status`,
  `actionsByStatus[PENDING]`).
- **O que falta**: "alerta visual" — hoje `PENDING` mistura-se
  visualmente com `CONFIRMED`/`IN_PREPARATION`/`OUT_FOR_DELIVERY` numa
  lista só ordenada por data, sem destaque nem agrupamento por urgência.

**Cenário 2 — não existe hoje:**
- O botão de cancelar em `received-orders.component.ts` chama
  `changeStatus(order, 'CANCELED')` — o mesmo endpoint genérico de
  transição de status (`PATCH /update-order-status`), **sem pedir
  justificativa, sem registrar motivo, sem notificar o cliente**.
- Não existe coluna para armazenar o motivo do cancelamento.
- **Interpretação de "estorno"**: o método de pagamento hoje é
  selecionado no checkout mas cobrado **na entrega** (dinheiro/Pix/cartão
  "na entrega", conforme Figura 12a do TCC) — não há captura de
  pagamento antecipada nem integração com gateway nesta fase do projeto
  (Pix dinâmico só chega na HU13, Should-have). Por isso, "efetuar o
  estorno" não pode significar uma chamada real a um gateway de
  pagamento — ainda não existe nenhuma. A leitura possível dentro do
  escopo Must-have é: **a transição para `CANCELED` só pode acontecer
  depois que a justificativa for capturada** — isso é o que se implementa
  aqui. Uma integração real de estorno fica fora de escopo (ver §7).
- **Infra reaproveitável**: `MailService.sendMail(to, subject, text)` já
  existe e já é usado para redefinição de senha — reaproveitado aqui para
  "avisar o cliente", em vez de criar um canal novo de notificação.
- **Achado colateral (tela diferente, fora do escopo)**:
  `order-history.component.ts` tem um botão de cancelar com
  `action: 'cancel'` (minúsculo) que quebra
  `OrderStatusEnum.valueOf(newStatus)` em runtime. Não corrigido aqui —
  ver §7.

## 5. Contrato Técnico

### 5.1 Migração `V4__order_cancellation_reason.sql`

```sql
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(500) NULL;
```

### 5.2 Backend

**`Order.java`** — adicionar `private String cancellationReason;`.

**Novo DTO `CancelOrderRequestDTO { private String reason; }`**.

**`OrderService.java`** — novo método dedicado (substitui o uso do
`updateOrderStatus` genérico para cancelamento):
```java
public String cancelOrder(String orderId, String reason) {
    if (reason == null || reason.isBlank()) {
        throw new BusinessRuleException("Justificativa é obrigatória para cancelar um pedido.");
    }

    Order order = findEntityById(orderId);
    order.setStatus(OrderStatusEnum.CANCELED);
    order.setCancellationReason(reason);
    orderRepository.save(order);

    mailService.sendMail(
        order.getCustomer().getEmail(),
        "Seu pedido foi cancelado",
        "Olá " + order.getCustomer().getName() + ", seu pedido #" + order.getId()
            + " foi cancelado pelo fornecedor. Motivo: " + reason
    );

    return order.getStatus().getStatus();
}
```

**`OrderController.java`** — novo endpoint dedicado:
```java
@PatchMapping("/{id}/cancel")
@ResponseBody
@RolesAllowed({"ADMIN", "SUPPLIER"})
public ResponseEntity<Map<String, String>> cancelOrder(
        @PathVariable String id,
        @RequestBody CancelOrderRequestDTO request) {
    String status = orderService.cancelOrder(id, request.getReason());
    return ResponseEntity.ok(Map.of("status", status));
}
```

### 5.3 Frontend

**`received-orders.component.ts`** — particionamento por prioridade
(Cenário 1):
```ts
get pendingOrders(): OrderResponse[] {
  return this.orders?.filter(o => o.status === 'Pendente') ?? [];
}
get otherOrders(): OrderResponse[] {
  return this.orders?.filter(o => o.status !== 'Pendente') ?? [];
}
```
Template: seção "Novos Pedidos" com badge de contagem, acima de
`otherOrders`, reaproveitando a classe `.status-pendente` já existente.

**Cancelamento (Cenário 2)** — o botão "Cancelar" deixa de chamar
`changeStatus(order, 'CANCELED')` diretamente; abre um modal simples
pedindo a justificativa (`<textarea required>`), só então chama:
```ts
cancelOrder(order: OrderResponse, reason: string) {
  this.orderService.cancelOrder(order.id, reason).subscribe({
    next: () => {
      order.status = 'Cancelado';
      this.orders = this.orders?.filter(o => o.id !== order.id);
      this.toastr.success('Pedido cancelado e cliente notificado.');
    },
    error: (err) => this.toastr.error(`Erro ao cancelar: ${err.error.message}`)
  });
}
```

**`order.service.ts`** — novo método `cancelOrder(orderId: string, reason: string)`
apontando para `PATCH /{id}/cancel`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `OrderServiceTest` — `deveRejeitarCancelamentoSemJustificativa`
   (`BusinessRuleException`), `deveCancelarPedidoComJustificativaValida`,
   `devePersistirMotivoDoCancelamento`,
   `deveEnviarEmailDeNotificacaoAoCancelar` (mock de `MailService`,
   verifica chamada).
2. `OrderControllerTest` — `deveRetornar400SeJustificativaAusente`,
   `deveRetornar200AoCancelarComJustificativa`.

**Frontend (a cobertura hoje é só o boilerplate `should create`):**
3. `received-orders.component.spec.ts` —
   `deveAgruparPedidosPendentesSeparadamenteDosDemais`,
   `deveConfirmarPedidoPendenteComUmaUnicaChamadaAoServico`,
   `deveExibirContagemDePedidosPendentesNoBadge`,
   `naoDevePermitirCancelarSemPreencherJustificativa`,
   `deveChamarCancelOrderComMotivoInformado`.

## 7. Fora de Escopo

- Integração real com gateway de pagamento/estorno — não existe captura
  de pagamento antecipada no sistema hoje; "estorno" é tratado aqui só
  como "não cancelar sem justificativa registrada" (ver §4).
- Corrigir o bug de `action: 'cancel'` (minúsculo) em
  `order-history.component.ts` — pertence a outra tela; mencionado só
  porque foi encontrado durante a investigação desta HU.
- Push em tempo real (SSE) de novos pedidos para o dashboard do
  fornecedor — nenhuma HU do Apêndice A exige isso explicitamente para o
  painel do fornecedor (só HU08, do lado do cliente).
- Notificação ao cliente por outro canal além de e-mail (push, SMS,
  WhatsApp) — reaproveita só a infra de e-mail já existente.

## 8. Definição de Pronto

- [ ] `received-orders` agrupa/destaca pedidos `PENDING` visualmente.
- [ ] Cancelamento exige justificativa (400 sem ela), persiste o motivo,
      dispara e-mail ao cliente.
- [ ] Confirmação com um clique continua funcionando (regressão).
- [ ] Testes da seção 6 passando (`mvn test` + `ng test`).
- [ ] Revisado e aprovado antes de HU09 começar.
