# HU14 — Carrinho de uma loja por vez

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU14 |
| Perfil | Visitante e Cliente (CUSTOMER) |
| Prioridade MoSCoW | Should have |
| Dependências | F7 (adicionar ao carrinho no catálogo), HU13 (vitrine do visitante) |
| Referência no TCC | Experiência de pedido estilo marketplace (uma loja por compra) |

## 2. História de Usuário

Como cliente (ou visitante montando o carrinho), quero que o carrinho
aceite produtos de **uma loja por vez**, para não misturar pedidos de
fornecedores diferentes; e, se eu quiser itens de outra loja, poder
esvaziar o carrinho e adicionar o novo produto.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Bloqueio de outra loja**: Dado o carrinho com item(ns) da
  loja A, Quando o usuário toca em "Adicionar" em um produto da loja B,
  Então o item B **não** entra no carrinho e aparece um diálogo
  explicando que só é possível comprar de uma loja por vez.
- **Cenário 2 — Esvaziar e adicionar**: Dado o diálogo do Cenário 1,
  Quando o usuário confirma "Esvaziar e adicionar", Então o carrinho fica
  só com o produto B (quantidade 1) e vê confirmação de adicionado.
- **Cenário 3 — Cancelar**: Dado o diálogo do Cenário 1, Quando o usuário
  cancela ("Manter carrinho" / Cancelar), Então o carrinho da loja A
  permanece intacto e o diálogo fecha.
- **Cenário 4 — Mesma loja**: Dado o carrinho com itens da loja A, Quando
  adiciona outro produto (ou o mesmo) da loja A, Então funciona como
  hoje (entra ou soma quantidade) sem diálogo.

## 4. Estado atual (achados)

- `CartService.addProduct` aceita qualquer `supplierId` e mistura lojas no
  `localStorage` (`cart-product`).
- Checkout (`order-resume`) cria **um pedido por fornecedor** se o
  carrinho estiver misto — legado; esta HU não altera o backend.
- "Comprar agora" (`startBuyNow`) é item avulso na sessão e **não** toca
  no carrinho (F7) — fora do escopo desta regra.

## 5. Contrato Técnico

### 5.1 `CartService`

- `getCartSupplierId(): string | null` — `supplierId` do primeiro item;
  carrinho vazio → `null`.
- `addProduct(product)` retorna `'added' | 'other_supplier'`:
  - carrinho vazio ou mesmo `supplierId` → adiciona/soma e retorna
    `'added'`;
  - outro `supplierId` (ou produto sem `supplierId` com carrinho já com
    loja) → **não** muta o carrinho e retorna `'other_supplier'`.
- `clearAndAdd(product)` — esvazia (`setCart([])`) e adiciona o produto
  (sempre resulta em `'added'`).

### 5.2 UI

- Componente standalone `cart-other-store-dialog` (`role="dialog"`,
  `aria-modal`): título "Uma loja por vez"; texto "Seu carrinho tem itens
  de outra loja. Esvazie para adicionar este produto."; botões Cancelar
  (`data-testid="keep-cart"`) e "Esvaziar e adicionar"
  (`data-testid="clear-and-add"`); container
  `data-testid="cart-other-store-dialog"`.
- `product-list` e `supplier-list` no `addToCart`:
  - `'added'` → toast de sucesso atual;
  - `'other_supplier'` → guarda o produto pendente e abre o diálogo;
  - no diálogo, confirmar → `clearAndAdd` + toast; cancelar → fecha sem
    mudar o carrinho.

## 6. Plano de Testes (TDD)

1. `cart.service.spec.ts`: `deveAdicionarQuandoCarrinhoVazio`,
   `deveSomarQuandoMesmaLoja`, `deveBloquearQuandoOutraLoja`,
   `deveEsvaziarEAdicionar`.
2. `product-list.component.spec.ts` e `supplier-list.component.spec.ts`:
   `deveAbrirDialogoAoAdicionarDeOutraLoja`,
   `deveEsvaziarEAdicionarPeloDialogo`, `deveManterCarrinhoAoCancelar`.

## 7. Fora de Escopo

- Validar "uma loja" no backend / rejeitar pedido misto na API.
- Remover o `forkJoin` multi-fornecedor do checkout (legado).
- Alterar o fluxo "Comprar agora".
- Sincronizar carrinho com o servidor.

## 8. Definição de Pronto

- [x] Script atendido; nada do "Fora de Escopo".
- [x] Testes do Plano verdes.
- [x] Fluxo manual: loja A → item → loja B → diálogo → cancelar mantém A;
      esvaziar deixa só B.

> **Verificação (2026-10-21)**: `ng test` — `cart.service`, `product-list` e
> `supplier-list` (50 specs) verdes. Regra no `CartService`; diálogo
> `cart-other-store-dialog` nas duas listas do catálogo.
