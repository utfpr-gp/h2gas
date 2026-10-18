# HU03 — Transparência de Preços

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU03 |
| Perfil | Cliente (CLIENT) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (`priceDelivery`/`pricePickup`) |
| Referência no TCC | Apêndice A.1 — HU03; RN01 |

## 2. História de Usuário

Como cliente, quero visualizar claramente a diferença de preço entre as
modalidades de entrega e retirada, para decidir a opção mais vantajosa.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Exibição dividida**: Dado um produto com precificação
  variável, Quando o cliente visualiza o card, Então o sistema exibe
  simultaneamente o valor de "Entrega no local" e "Retirada na loja".
- **Cenário 2 — Reajuste no carrinho**: Dado itens já no carrinho, Quando
  o cliente altera a modalidade logística, Então o sistema recalcula e
  atualiza o subtotal automaticamente.

## 4. Estado atual (achados)

> Revisado em 2026-10-14, sobre a `develop` com F1–F7 (fatias 1 e 2),
> HU01, HU02 e HU04 mergeados. A versão original deste script foi escrita
> antes do F3 e alguns achados mudaram.

- `OrderItemService.createOrderItem` **já compila**: depois do F3 passou a
  usar `productItem.getPriceDelivery()` **fixo**, para qualquer pedido. O
  achado agora é: **o preço unitário ignora a modalidade** — pedido de
  retirada é cobrado pelo preço de entrega.
- `ProductItemDTO` (backend) expõe um único `price` (preenchido com
  `priceDelivery` pelo `ProductItemMapper`). O cliente **não recebe**
  `pricePickup` hoje. O `price` continua sendo usado pelo lado do
  fornecedor (`CreateProductItemMapper`, `ProductItemService.update` grava
  o mesmo valor nos dois preços); trocar esse cadastro é HU10.
- A ordenação por preço da HU02 já usa `priceDelivery,asc` — mantida.
- `Order` **não tem nenhum campo de modalidade** (entrega vs. retirada)
  hoje — nem no schema legado, nem no que o F3 adicionou (a Figura 19 do
  TCC também não lista essa coluna, apesar de ser pré-requisito lógico
  para HU03 funcionar). Precisa de nova migração. A `V3` já foi usada
  (`V3__allow_deliverer_user_type.sql`); a desta história é a `V4`.
- `OrderController` tem `@Tag("Pedidos")`, mas as rotas não têm
  `@Operation`/`@ApiResponses`. `POST /api/orders` é alterada aqui (ganha
  `deliveryMode`), então passa a ser documentada junto com a rota nova.
- RN01 já é parcialmente respeitada: `OrderService.calculateTotalPrice`
  soma `OrderItem.totalPrice`, que é sempre calculado no backend a partir
  do produto (nunca confia em valor vindo do cliente) — esse padrão será
  mantido, só trocando a fonte do preço unitário conforme a modalidade.
- **Contra-exemplo de RN01 já existente**: `order-resume.component.ts`
  (tela de checkout) calcula `totalPrice` **no frontend**
  (`item.price! * item.quantitySelect!`), usado só para exibição. Com
  preço dependente de modalidade, esse cálculo local fica ainda mais
  arriscado de divergir do valor real. Precisa ser substituído por uma
  chamada ao backend.
- `ProductItem` (frontend) tem um único campo `price?: number`. Preço do
  cliente aparece em três lugares: `product-card.component.html` (cards do
  `supplier-list`/`product-list`, Fatia 1 do F7), drawer do carrinho em
  `menu.component.html` e `order-resume.component.html` (preço por
  unidade e total por item, também calculado no client via
  `totalPerUnit`).
- `CartService` não tem nenhum conceito de modalidade logística associada
  ao carrinho.

## 5. Contrato Técnico

### 5.1 Migração `V4__order_delivery_mode.sql`

```sql
ALTER TABLE orders ADD COLUMN delivery_mode VARCHAR(20) NOT NULL DEFAULT 'DELIVERY';
```

### 5.2 Backend

**Novo enum `DeliveryModeEnum.java`** (`com.h2gas.enums`):
```java
public enum DeliveryModeEnum {
    DELIVERY("Entrega no local"),
    PICKUP("Retirada na loja");
    // construtor/getter no mesmo padrão de OrderStatusEnum
}
```

**`Order.java`** — adicionar:
```java
@Enumerated(EnumType.STRING)
private DeliveryModeEnum deliveryMode;
```

**`OrderRequestDTO.java`** — adicionar `private String deliveryMode;`
(convertido para o enum no service; ausente → `DELIVERY`, para não
quebrar clientes antigos; valor fora de `DELIVERY`/`PICKUP` →
`BusinessRuleException` → 400).

**`ProductItemDTO.java`** — adicionar `priceDelivery` e `pricePickup`
(`Double`, mesmo tipo do `price` atual), preenchidos pelo
`ProductItemMapper`. `price` permanece (lado fornecedor, ver §4).

**`OrderItemService.createOrderItem`** — aplicar a modalidade. A escolha
do preço é um invariante único, reaproveitado pelo preview:
```java
static BigDecimal unitPriceFor(ProductItem productItem, DeliveryModeEnum mode) {
    return mode == DeliveryModeEnum.PICKUP
        ? productItem.getPricePickup()
        : productItem.getPriceDelivery();
}

BigDecimal unitPrice = unitPriceFor(productItem, order.getDeliveryMode());
orderItem.setTotalPrice(calculateTotalPrice(orderItem.getQuantity(), unitPrice));
orderItem.setUnitPrice(unitPrice);
```

**Novo endpoint de pré-visualização** (RN01 — nunca confiar em subtotal
calculado no cliente, nem para exibição):

`OrderPreviewRequestDTO { List<OrderItemRequestDTO> items; String deliveryMode; }`
`OrderPreviewDTO { List<OrderPreviewItemDTO> items; BigDecimal subtotal; }`
(`OrderPreviewItemDTO`: `productItemRef`, `quantity`, `unitPrice`, `totalPrice`)

```java
@PostMapping("/preview")
@ResponseBody
@RolesAllowed({"ADMIN", "CUSTOMER"})
public ResponseEntity<OrderPreviewDTO> previewOrder(@RequestBody OrderPreviewRequestDTO request) {
    return ResponseEntity.ok(orderService.previewOrder(request));
}
```

`OrderService.previewOrder`: para cada item, busca `ProductItem` (sem
lock — não decrementa estoque, é só leitura), escolhe
`priceDelivery`/`pricePickup` pela modalidade recebida, soma. Não
persiste nada.

Validação no boundary (`@Valid`): `items` `@NotEmpty`, `deliveryMode`
`@NotBlank`; cada item com `productItemRef` e `quantity` positivos.

OpenAPI (CLAUDE.md): `@Operation` + `@ApiResponses` em
`POST /api/orders/preview` e em `POST /api/orders` (alterada):

| Rota | Códigos |
|---|---|
| `POST /api/orders/preview` | 200, 400 (payload inválido / modalidade desconhecida), 403 (sem token ou papel sem permissão), 404 (produto inexistente) |
| `POST /api/orders` | 200, 400, 403, 404 |

A API não tem entry point 401: requisição sem token cai em 403 (comportamento atual de todo o projeto).

`@Schema` em `OrderPreviewRequestDTO`, `OrderPreviewDTO`,
`OrderPreviewItemDTO` e no campo `deliveryMode` do `OrderRequestDTO`
(`allowableValues = {"DELIVERY", "PICKUP"}`).

### 5.3 Frontend

**`models/product-item.ts`** — adicionar (o `price` fica enquanto o lado
fornecedor usar, até a HU10):
```ts
priceDelivery?: number;
pricePickup?: number;
```

**`product-card`** (usado por `supplier-list` e `product-list`) — exibir
as duas linhas simultaneamente:
```html
<span>Entrega: {{ product.priceDelivery | currency:'BRL' }}</span>
<span>Retirada: {{ product.pricePickup | currency:'BRL' }}</span>
```

**`cart.service.ts`** — adicionar estado de modalidade:
```ts
private deliveryModeSubject = new BehaviorSubject<'DELIVERY' | 'PICKUP'>(
  this.readDeliveryModeFromStorage()
);
deliveryMode$ = this.deliveryModeSubject.asObservable();

setDeliveryMode(mode: 'DELIVERY' | 'PICKUP') {
  localStorage.setItem('cart-delivery-mode', mode);
  this.deliveryModeSubject.next(mode);
}
```

**`order-resume.component.ts`** — remover o getter `totalPrice` local
(cálculo client-side); ao entrar na tela e a cada mudança de modalidade,
chamar `POST /api/orders/preview` com os itens do carrinho + modalidade
atual, e exibir `subtotal` da resposta. Adicionar toggle de modalidade
("Entrega no local" / "Retirada na loja") que dispara nova chamada de
preview. Payload final de `createOrder` passa a incluir `deliveryMode`.
O total por item (`totalPerUnit`, hoje calculado no client) também passa
a vir do preview (`OrderPreviewItemDTO.totalPrice`).

**`order.service.ts`** — `previewOrder(request)` →
`POST /api/orders/preview`.

**Drawer do carrinho (`menu`)** — mostra o preço unitário da modalidade
ativa (`deliveryMode$`). O total do drawer já era somado no client antes
desta HU; passa a usar o preço da modalidade ativa e fica rotulado como
estimativa. O valor cobrado é sempre o do preview/pedido no backend.
Trocar esse total por uma chamada ao preview fica como dívida registrada
(não é critério de aceitação da HU03).

### 5.4 Visual (F7 §5.8)

Telas desta HU recebem o F7 aqui, com a **Figura 12a** do TCC como
referência: só tokens do F2 (sem HEX novo), cards `rounded-xl`, toggle de
modalidade como controle segmentado com `bg-primary` no ativo, `pb-24`
no mobile por causa da bottom tab bar.

## 6. Plano de Testes (TDD)

**Backend:**
1. `OrderItemServiceTest` — atualizar testes existentes que usam
   `.setPrice(...)`; novos casos:
   `deveUsarPrecoDeEntregaQuandoModalidadeForDelivery`,
   `deveUsarPrecoDeRetiradaQuandoModalidadeForPickup`.
2. `OrderServiceTest` — `devePersistirDeliveryModeNoPedido`.
3. `OrderServiceTest` (preview) —
   `deveCalcularSubtotalSemPersistirNadaNoPreview`,
   `devePreviewRefletirModalidadeInformada`.
4. `OrderControllerTest` (novo, no padrão de `ProductItemControllerTest`)
   — `devePreviewRetornar200ComSubtotalCorreto`,
   `devePreviewRetornar400SemItens`.
5. `OrderServiceTest` — `deveRejeitarModalidadeDesconhecida`,
   `deveAssumirEntregaQuandoModalidadeAusente`.
6. `ProductItemMapper` — `deveExporPrecoDeEntregaEDeRetirada`.

**Frontend:**
7. `order-resume.component.spec.ts` —
   `deveChamarPreviewAoAlterarModalidade`,
   `naoDeveMaisCalcularTotalLocalmente` (regressão — garante que o
   getter antigo foi removido/substituído),
   `deveEnviarDeliveryModeAoCriarPedido`.
8. `cart.service.spec.ts` — `devePersistirModalidadeNoLocalStorage`.
9. `product-card.component.spec.ts` —
   `deveExibirAmbosPrecosSimultaneamente` (verifica `priceDelivery` e
   `pricePickup` renderizados).

Regra herdada: o total de specs antigos falhando no frontend (25) não
pode aumentar.

## 7. Fora de Escopo

- Gatilho de "frete grátis" quando `priceDelivery == pricePickup`
  (etiqueta visual) — isso é HU10, do lado do cadastro do fornecedor.
- ~~Tornar `deliveryAddress` opcional para pedidos `PICKUP` — mantido
  obrigatório por simplicidade; não é um critério de aceitação do TCC.~~
  **Revertido pela HU05 (§5.3.1, 2026-10-15)**: pedidos de retirada não
  têm endereço de entrega.
- Qualquer UI de seleção de modalidade por item individual — a modalidade
  é por pedido inteiro, não por produto.

## 8. Definição de Pronto

- [ ] `OrderItemService` cobra o preço da modalidade do pedido.
- [ ] `V4__order_delivery_mode.sql` aplicada; `Order.deliveryMode`
      persistido.
- [ ] Endpoint `/api/orders/preview` implementado e não persiste nada.
- [ ] `/api/orders/preview` e `POST /api/orders` documentados no Swagger.
- [ ] Cards exibem `priceDelivery` e `pricePickup` simultaneamente.
- [ ] Checkout usa o preview do backend, não cálculo local.
- [ ] Telas no padrão F7 (Figura 12a): tokens, `rounded-xl`, `pb-24`.
- [ ] Todos os testes da seção 6 passando; specs antigos falhando ≤ 25.
- [ ] Testado manualmente no navegador pelo desenvolvedor.
