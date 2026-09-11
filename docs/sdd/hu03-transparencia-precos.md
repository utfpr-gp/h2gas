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

## 4. Estado atual (achados) — inclui uma quebra deixada pelo F3

- **Achado crítico**: `OrderItemService.createOrderItem` (backend) ainda
  chama `productItem.getPrice()` para calcular `unitPrice`/`totalPrice`.
  Esse campo **foi removido pelo F3** (split em `priceDelivery`/
  `pricePickup`). Ou seja, **o backend não compila** entre o merge do F3 e
  a implementação desta história — F3 não previu esse ponto de uso porque
  seu escopo era só schema/entidade, não lógica de negócio. Correção
  fica aqui, não retroativamente no F3.
- `Order` **não tem nenhum campo de modalidade** (entrega vs. retirada)
  hoje — nem no schema legado, nem no que o F3 adicionou (a Figura 19 do
  TCC também não lista essa coluna, apesar de ser pré-requisito lógico
  para HU03 funcionar). Precisa de nova migração.
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
- `ProductItem` (frontend) tem um único campo `price?: number` — precisa
  virar `priceDelivery?: number` / `pricePickup?: number`.
- `CartService` não tem nenhum conceito de modalidade logística associada
  ao carrinho.

## 5. Contrato Técnico

### 5.1 Migração `V3__order_delivery_mode.sql`

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
(convertido para o enum no mapper/service).

**`OrderItemService.createOrderItem`** — corrigir a quebra do F3 **e**
aplicar a modalidade:
```java
BigDecimal unitPrice = order.getDeliveryMode() == DeliveryModeEnum.PICKUP
    ? productItem.getPricePickup()
    : productItem.getPriceDelivery();

BigDecimal totalPrice = calculateTotalPrice(orderItem.getQuantity(), unitPrice);
orderItem.setTotalPrice(totalPrice);
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

### 5.3 Frontend

**`models/product-item.ts`** — trocar `price?: number` por:
```ts
priceDelivery?: number;
pricePickup?: number;
```

**Componentes de card de produto** (`supplier-list`,
`product-list` — templates onde o preço é hoje renderizado a partir de
`product.price`) — exibir as duas linhas simultaneamente:
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
4. `OrderControllerTest` — `devePreviewRetornar200ComSubtotalCorreto`.

**Frontend:**
5. `order-resume.component.spec.ts` —
   `deveChamarPreviewAoAlterarModalidade`,
   `naoDeveMaisCalcularTotalLocalmente` (regressão — garante que o
   getter antigo foi removido/substituído).
6. `cart.service.spec.ts` — `devePersistirModalidadeNoLocalStorage`.
7. Teste de snapshot/HTML dos cards — `deveExibirAmbosPrecosSimultaneamente`
   (verifica presença de `priceDelivery` e `pricePickup` renderizados).

## 7. Fora de Escopo

- Gatilho de "frete grátis" quando `priceDelivery == pricePickup`
  (etiqueta visual) — isso é HU10, do lado do cadastro do fornecedor.
- Tornar `deliveryAddress` opcional para pedidos `PICKUP` — mantido
  obrigatório por simplicidade; não é um critério de aceitação do TCC.
- Qualquer UI de seleção de modalidade por item individual — a modalidade
  é por pedido inteiro, não por produto.

## 8. Definição de Pronto

- [ ] Backend volta a compilar (`OrderItemService` corrigido).
- [ ] `V3__order_delivery_mode.sql` aplicada; `Order.deliveryMode`
      persistido.
- [ ] Endpoint `/api/orders/preview` implementado e não persiste nada.
- [ ] Cards exibem `priceDelivery` e `pricePickup` simultaneamente.
- [ ] Checkout usa o preview do backend, não cálculo local.
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado antes de HU04 começar.
