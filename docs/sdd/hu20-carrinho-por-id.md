# HU20 — Carrinho guarda só id e quantidade; os dados vêm do banco

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU20 |
| Perfil | Cliente e Visitante |
| Prioridade MoSCoW | Should have — decisão do autor em 2026-10-27 ("o carrinho envia o objeto inteiro para o local storage; quero só o ID, e buscar no banco na hora de finalizar") |
| Dependências | HU13 (carrinho do visitante), HU14 (uma loja por vez), HU18 (loja aberta pelo horário), F6 (OpenAPI) |
| Referência no TCC | RN01 (cálculo no backend), RN02 (estoque) |

## 2. História de Usuário

Como cliente (ou visitante), quero que o carrinho mostre sempre o preço,
o nome e a disponibilidade atuais de cada item, para não ser surpreendido
no checkout por um preço antigo ou um produto que acabou.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Só id e quantidade no navegador**: Dado o cliente que
  adiciona um produto, Então o `localStorage` guarda apenas
  `{ productId, quantity }` (e o id da loja do carrinho), sem marca, preço,
  imagem nem estoque.
- **Cenário 2 — Dados atuais ao abrir o carrinho**: Dado itens no
  carrinho, Quando o cliente abre o carrinho, Então marca, foto, preço
  unitário e total vêm do backend, pelo modo escolhido (entrega ou
  retirada).
- **Cenário 3 — Preço mudou**: Dado um produto cujo preço mudou depois de
  adicionado, Então o carrinho e o checkout já mostram o preço novo.
- **Cenário 4 — Item indisponível**: Dado um item sem estoque suficiente,
  removido do catálogo, ou de loja inativa ou fechada (HU18), Então ele
  aparece com o motivo e a opção "Remover", não entra no subtotal, e o
  checkout fica bloqueado enquanto ele estiver no carrinho.
- **Cenário 5 — Visitante**: Dado um visitante sem conta, Então o
  carrinho funciona do mesmo jeito (a consulta não exige login).
- **Cenário 6 — Carrinho antigo**: Dado um navegador com o carrinho no
  formato antigo (objeto inteiro), Quando o app carrega, Então o carrinho é
  convertido para o novo formato sem perder itens nem quantidades.
- **Cenário 7 — Comprar agora e pedir de novo**: Dado "Comprar agora" ou
  "Pedir de novo", Então também guardam só id e quantidade.
- **Cenário 8 — Contador do menu**: Dado itens no carrinho, Então o
  contador do menu soma as quantidades locais, sem ir ao servidor.

## 4. Estado atual (achados)

- `CartService` grava o `ProductItem` inteiro em
  `localStorage['cart-product']` (`setCart`/`getCart`) e o "Comprar agora"
  grava o objeto em `sessionStorage`.
- O drawer do menu calcula uma estimativa com o objeto guardado
  (`menu.component.ts:215`); o resumo do pedido mostra marca e foto dele,
  e só o preço vem de `POST /api/orders/preview`
  (`OrderService.previewOrder`), que aceita só ADMIN e CUSTOMER — o
  visitante não acessa — e devolve só id, quantidade e preços.
- A regra da HU14 (uma loja por vez) compara o `supplierId` do objeto.
- "Pedir de novo" (`order-list.component.ts:148`) grava objetos montados
  a partir do `reorder-preview`.

## 5. Contrato Técnico

### 5.1 Backend
- Rota nova **pública** `POST /api/cart/preview` (`CartController`,
  `@Tag("Carrinho")`, `permitAll` em `SecurityConfigurations`):
  - corpo `CartPreviewRequestDTO { items: [{ productId, quantity }],
    deliveryMode }` — `@Valid`, de 1 a 50 itens, `quantity >= 1`,
    `deliveryMode` `DELIVERY|PICKUP` (padrão `DELIVERY`); fora disso, 400;
  - resposta `CartPreviewDTO { supplierId, supplierName, items[], subtotal }`;
    cada item: `productId, brand, category, type, imageUrl, quantity,
    unitPrice, totalPrice, available, unavailableReason`;
  - preço pelo modo com `OrderItemService.unitPriceFor` (RN01);
  - `available = false` com motivo: `NOT_FOUND`, `OUT_OF_STOCK` (estoque
    zerado ou menor que a quantidade, RN02), `STORE_INACTIVE`,
    `STORE_CLOSED` (regra da HU18); item indisponível fica fora do
    subtotal.
- OpenAPI completo (rota pública, sem cadeado).

### 5.2 Frontend
- `CartService` guarda `[{ productId, quantity }]` em `cart-product` e o
  id da loja em `cart-supplier`; `addProduct(product)` extrai só o id e a
  loja; a regra da HU14 compara com `cart-supplier`.
- Conversão única do formato antigo na leitura (`id` + `quantitySelect`
  → `productId` + `quantity`; loja tirada do primeiro item).
- "Comprar agora" guarda `{ productId, quantity }` no `sessionStorage`.
- Novo `cart-api.service.ts` (`preview(lines, mode)`), usado pelo drawer
  do menu ao abrir e pelo `order-resume`; item indisponível com o motivo e
  "Remover"; "Finalizar" bloqueado enquanto houver um.
- "Pedir de novo" grava só ids e quantidades.

## 6. Plano de Testes (TDD)

**Backend**
1. `CartPreviewServiceTest`: `deveCalcularOPrecoPeloModo`,
   `deveMarcarProdutoInexistente`, `deveMarcarSemEstoque`,
   `deveMarcarEstoqueMenorQueAQuantidade`, `deveMarcarLojaInativa`,
   `deveMarcarLojaFechadaPeloHorario`,
   `naoDeveSomarItemIndisponivelNoSubtotal`.
2. `CartControllerTest`: `deveResponderSemToken`,
   `deveRecusarListaVazia`, `deveRecusarQuantidadeZero`,
   `deveRecusarMaisDeCinquentaItens`.
3. `PublicCatalogAccessTest`: a rota aparece sem cadeado no OpenAPI.

**Frontend**
4. `cart.service.spec.ts`: `deveGuardarSoIdEQuantidade`,
   `deveConverterOCarrinhoAntigo`, `deveBloquearOutraLojaPeloCartSupplier`,
   `deveSomarAsQuantidadesNoContador`, `deveGuardarSoIdNoComprarAgora`.
5. `cart-api.service.spec.ts`: corpo e rota do preview.
6. `menu.component.spec.ts`: `deveBuscarOsItensAoAbrirOCarrinho`,
   `deveMostrarItemIndisponivelComMotivo`.
7. `order-resume.component.spec.ts`: `deveExibirOsDadosDoPreview`,
   `deveBloquearOCheckoutComItemIndisponivel`, `deveRemoverItemIndisponivel`.
8. `order-list.component.spec.ts`: `deveGuardarSoIdsAoPedirDeNovo`.

**E2E (registrado no DoD)**: adicionar itens e conferir o `localStorage`;
mudar o preço de um produto no banco e ver o carrinho atualizado; zerar o
estoque e ver o aviso; visitante com carrinho; carrinho antigo convertido.
Em 390 e 1280 px.

## 7. Fora de Escopo

- Carrinho salvo no banco ou sincronizado entre aparelhos (fora desde a HU13).
- Reservar estoque ao adicionar ao carrinho.
- Frete por distância no carrinho (HU17).
- Bloquear o envio do pedido fora do horário no backend (risco da HU18).

## 8. Definição de Pronto

- [ ] `localStorage` e `sessionStorage` com só id e quantidade.
- [ ] `POST /api/cart/preview` público, com os motivos de indisponibilidade
      e documentado no Swagger.
- [ ] Drawer e checkout com dados do backend; item indisponível com aviso,
      remoção e checkout bloqueado.
- [ ] Carrinho antigo convertido sem perda.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E em 390 e 1280 px registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
