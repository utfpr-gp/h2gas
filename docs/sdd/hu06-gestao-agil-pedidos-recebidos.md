# HU06 — Gestão Ágil de Pedidos Recebidos

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU06 |
| Perfil | Fornecedor (SUPPLIER) |
| Prioridade MoSCoW | Must have |
| Dependências | HU03/HU05 (`deliveryMode`, retirada sem endereço); F7 (visual, Figura 14) |
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

> Revisado em 2026-10-18 sobre a `develop` com HU01–HU05 e F7 (fatias
> 1 e 2) mergeados. Decisões do autor nesta revisão: (a) o cancelamento
> **devolve o estoque** dos itens ("estorno" de estoque; não há gateway de
> pagamento); (b) o painel entrega o layout completo da **Figura 14**.

- **Cenário 1 — Aceite na fila**: Dado o painel administrativo ativo,
  Quando entra um pedido `PENDING`, Então o sistema emite alerta visual
  **e** permite mudar para "Confirmado" num clique.
- **Cenário 2 — Cancelamento**: Dado a impossibilidade de atendimento,
  Quando o fornecedor aciona o cancelamento, Então exige-se uma
  justificativa antes de efetuar o estorno e avisar o cliente.

## 4. Estado atual (achados)

**Cenário 1:**
- `dashboard-fornecedor` → `ReceivedOrdersComponent` é o painel certo
  (`historico-pedidos` → `OrderHistoryComponent` é outra tela, só
  `DELIVERED`/`CANCELED`). Carrega tudo menos `DELIVERED`/`CANCELED`
  (`GET /api/orders/{supplierId}/exclude-status`), mais recente primeiro.
- Os cards são do `OrderWidgetComponent` (`orders-widget`), com botões
  por status via `actionsByStatus`; PENDING→CONFIRMED já é um clique
  (`changeStatus()` → `PATCH /{id}/update-order-status`).
- **Falta**: alerta visual — `PENDING` se mistura com os demais. Também
  não há atualização sozinha: um pedido novo só aparece recarregando a
  página. A HU08 (SSE) exclui explicitamente o painel do fornecedor
  (§ Fora de Escopo dela), então aqui o "quando entra um pedido" é
  atendido com **atualização periódica** (polling).
- A cada busca o componente mostra toast "Pedidos buscado com sucesso";
  com polling isso vira spam — sai aqui (a F7 fatia 3 já previa).

**Cenário 2:**
- "Cancelar" chama `changeStatus(order, 'CANCELED')` no endpoint
  genérico — sem justificativa, sem motivo salvo, sem aviso ao cliente,
  e **sem devolver o estoque** baixado na criação do pedido (HU05 tornou a
  criação atômica, mas nada repõe estoque no cancelamento).
- Nenhuma checagem de dono: qualquer `SUPPLIER` pode mudar status de
  pedido de outro fornecedor pelo endpoint genérico (dívida já conhecida
  de rotas antigas; a rota nova de cancelamento nasce com checagem, via
  `ForbiddenResourceException` → 403, criada na HU05).
- `MailService.sendMail(to, subject, text)` existe e é reaproveitado.
- **Estorno**: o pagamento é na entrega (sem gateway; Pix dinâmico só na
  HU13). Estorno aqui = **devolver o estoque dos itens na mesma transação
  do cancelamento** (decisão do autor), além de exigir a justificativa.
- Migração: V3–V5 já usadas; a desta HU é a **`V6`** (a HU11, revisada
  antes prevendo V6, passa para V7 quando começar).
- `OrderResponseDTO` não traz o telefone do cliente, que a Figura 14
  mostra no detalhe.
- **Achado colateral (fora do escopo)**: `order-history.component.ts`
  usa `action: 'cancel'` (minúsculo), que quebra
  `OrderStatusEnum.valueOf` em runtime.

**Visual (F7 §5.8 → Figura 14):** mestre-detalhe. À esquerda, "Pedidos
Recebidos" em lista compacta (número, itens, horário, modalidade). À
direita, o pedido selecionado: cabeçalho "Pedido #…", **linha do tempo de
status** (Pendente → Confirmado → Em preparo → Saiu para entrega /
Pronto para retirada → Entregue), ações do status atual, cards de
cliente (nome, telefone) e entrega (endereço ou "Retirada na loja"),
itens e total. O botão "Atribuir Entregador" da figura é da **HU07**.

## 5. Contrato Técnico

### 5.1 Migração `V6__order_cancellation_reason.sql`

```sql
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(500) NULL;
```

### 5.2 Backend

- `Order.cancellationReason` (`String`, coluna `cancellation_reason`).
- `OrderResponseDTO` ganha `customerPhone` e `cancellationReason`
  (preenchidos no `OrderMapper`).
- `CancelOrderRequestDTO { @NotBlank @Size(max = 500) String reason; }`
  com `@Schema`.
- `CancelOrderResponseDTO { String status; boolean customerNotified; }`.
- **`OrderService.cancelOrder(String orderId, String reason, User actor)`**
  — `@Transactional`:
  - pedido inexistente → `ResourceNotFoundException` (404);
  - `actor` SUPPLIER que não é o fornecedor do pedido →
    `ForbiddenResourceException` (403); ADMIN pode;
  - status `DELIVERED` ou `CANCELED` → `BusinessRuleException` (400);
  - devolve `quantity` de cada item ao `stockQuantity` do produto (item
    com produto nulo é ignorado), grava `CANCELED` + motivo;
  - devolve o `Order` para a notificação.
- **`OrderNotificationService.notifyCancellation(Order)`** — novo, fora
  da transação: chama `MailService.sendMail` para o e-mail do cliente
  (assunto "Seu pedido foi cancelado", com número do pedido, fornecedor e
  motivo). Falha de e-mail (`MailException`) **não desfaz o
  cancelamento**: é registrada em log de aviso e devolvida como
  `customerNotified=false`, que a tela mostra ao fornecedor (falha
  visível, não engolida).
- **`PATCH /api/orders/{id}/cancel`** (`@RolesAllowed({"ADMIN",
  "SUPPLIER"})`, `@Valid`), dono = principal autenticado. OpenAPI:
  `@Operation` + `@ApiResponses` 200 (`CancelOrderResponseDTO`), 400
  (sem justificativa / já entregue ou cancelado), 403 (sem token, papel
  sem permissão ou pedido de outro fornecedor), 404.

### 5.3 Frontend

- `models/order.model.ts`: `OrderResponse.customerPhone?`,
  `cancellationReason?`; `CancelOrderResponse`.
- `order.service.ts`: `cancelOrder(orderId, reason)` →
  `PATCH /orders/{id}/cancel`.
- **`order-status-timeline`** (componente novo, standalone): recebe
  `status` e `deliveryMode`; marca etapas concluídas/atual; rótulo da
  4ª etapa conforme a modalidade ("Saiu para entrega" / "Pronto para
  retirada"); pedido cancelado mostra estado "Cancelado".
- **`received-orders`** (mestre-detalhe, Figura 14):
  - lista à esquerda com seção **"Novos pedidos"** (PENDING) no topo,
    badge com a contagem e destaque visual, e os demais abaixo;
  - pedido selecionado à direita (o primeiro pendente por padrão; no
    mobile a lista vira tela inteira e o detalhe abre ao tocar, com
    "Voltar");
  - ações do status atual reaproveitam `actionsByStatus`/`changeStatus`
    (Confirmar = um clique);
  - "Cancelar" abre modal com `textarea` obrigatória (até 500
    caracteres); só envia com texto; resposta com
    `customerNotified=false` mostra aviso de que o cliente não foi
    avisado por e-mail;
  - atualização a cada 30 s (pedido novo pendente ganha destaque
    "Novo"); sem toast de sucesso nas buscas;
  - tokens F2, cards `rounded-xl`, sem HEX novo.
- `orders-widget` deixa de ser usado pelo `received-orders`; continua
  em uso pelo `order-history` (histórico), que fica como está.

## 6. Plano de Testes (TDD)

**Backend:**
1. `OrderServiceTest` — `deveRejeitarCancelamentoSemJustificativa`,
   `deveCancelarPedidoComJustificativaValida` (status + motivo),
   `deveDevolverEstoqueAoCancelar`,
   `deveRejeitarCancelamentoDePedidoDeOutroFornecedor` (403),
   `deveRejeitarCancelamentoDePedidoEntregueOuJaCancelado`,
   `deveExporTelefoneDoClienteNoPedido`.
2. `OrderNotificationServiceTest` (Mockito) —
   `deveEnviarEmailAoClienteComMotivo`,
   `deveRetornarFalsoQuandoEmailFalhar`.
3. `OrderControllerTest` — `deveRetornar400SeJustificativaAusente`,
   `deveRetornar200AoCancelarComJustificativa` (com `customerNotified`).

**Frontend:**
4. `order.service.spec.ts` — `deveCancelarPedidoComMotivo`.
5. `order-status-timeline.component.spec.ts` —
   `deveMarcarEtapasConcluidasEAtual`,
   `deveUsarRotuloDeRetiradaQuandoPickup`.
6. `received-orders.component.spec.ts` —
   `deveSepararPedidosPendentesNoTopoComContagem`,
   `deveSelecionarPrimeiroPendentePorPadrao`,
   `deveConfirmarPedidoPendenteComUmClique`,
   `naoDevePermitirCancelarSemJustificativa`,
   `deveChamarCancelOrderComMotivoInformado`,
   `deveAvisarQuandoClienteNaoFoiNotificado`,
   `deveAtualizarPedidosPeriodicamente`,
   `naoDeveExibirToastDeSucessoAoBuscarPedidos`.

Regra herdada: specs antigos falhando no frontend não aumentam (23).

## 7. Fora de Escopo

- Integração real com gateway de pagamento/estorno financeiro.
- Botão "Atribuir Entregador" e modal (Figura 15) — **HU07**.
- Tempo real por SSE no painel do fornecedor (HU08 também exclui).
- Checagem de dono no endpoint genérico `update-order-status` (dívida
  das rotas antigas, registrada).
- Corrigir `action: 'cancel'` em `order-history.component.ts`.
- Notificação por outro canal além de e-mail.

## 8. Definição de Pronto

- [ ] `V6__order_cancellation_reason.sql` aplicada.
- [ ] Cancelamento exige justificativa (400 sem ela), checa dono (403),
      bloqueia entregue/cancelado (400), devolve estoque, persiste o
      motivo e tenta avisar o cliente por e-mail (`customerNotified`).
- [ ] `PATCH /api/orders/{id}/cancel` documentado no Swagger.
- [ ] Painel no layout da Figura 14: pendentes destacados com contagem,
      detalhe com linha do tempo, confirmação em um clique (regressão),
      atualização periódica.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 23 falhas
      antigas).
- [ ] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [ ] Testado de ponta a ponta (navegador ou E2E automatizado).
