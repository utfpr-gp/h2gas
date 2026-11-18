# HU21 — Produto com um preço só

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU21 |
| Perfil | Fornecedor, Cliente e Visitante |
| Prioridade MoSCoW | Must have. Decisão do autor em 2026-10-26: "frete por produto não é válido". Em 2026-10-27 ele decidiu separar da HU17 a parte do preço, que entra antes dela. |
| Dependências | F3 (Flyway), F6 (OpenAPI), HU03 e HU10 (os dois preços, que esta HU revoga), HU16 (ordenação por preço), HU20 (preview do carrinho) |
| Antes de | HU17 (frete por km), que soma o frete por distância a este preço |
| Referência no TCC | RN01 (cálculo no backend); revisão do modelo de preço da HU03/HU10 |

## 2. História de Usuário

Como fornecedor, quero cadastrar **um** preço por produto, sem embutir o
frete nele; e como cliente, quero ver o preço do produto e o frete
separados. O frete passa a vir da distância (HU17).

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Cadastro com um preço**: Dado o fornecedor cadastrando ou
  editando um produto, Então informa um único "Preço". Os campos "Preço do
  frete" e "Entrega grátis" do produto deixam de existir.
- **Cenário 2 — Migração sem perda**: Dado os produtos já cadastrados,
  Quando a migração roda, Então cada um passa a ter `price` = o antigo
  preço de **retirada**, que é o preço sem frete. Nenhuma linha se perde, e
  as colunas `price_delivery` e `price_pickup` saem.
- **Cenário 3 — Catálogo com um preço**: Dado o card de produto (vitrine,
  página da loja e lista do fornecedor), Então ele mostra um preço só. Some
  a linha dupla "com entrega" / "Retirada na loja".
- **Cenário 4 — Mesmo preço nas duas modalidades**: Dado um pedido, o
  preview do pedido ou o preview do carrinho (HU20), Então o preço
  unitário é o `price` do produto, em entrega e em retirada. O frete por
  distância é somado na HU17. Até ela entrar, a entrega não cobra frete,
  o que só afeta o ambiente local.
- **Cenário 5 — Ordenar por preço**: Dado "Menor preço" ou "Maior preço"
  na vitrine (HU16), Então a ordem segue o `price`.
- **Cenário 6 — Pedir de novo**: Dado "Pedir de novo" (HU05), Então o
  preview traz o preço atual único do produto.

## 4. Estado atual (achados)

- `product_items.price_delivery` e `price_pickup` (sem `CREATE TABLE`
  versionado; o ambiente roda `ddl-auto=validate`).
- `ProductItemMapper` deriva `price` (retirada), `shippingFee` (entrega −
  retirada) e `freeDelivery` (iguais).
- `CreateProductItemDTO` recebe `price` + `shippingFee`, e o
  `CreateProductItemMapper` grava entrega = preço + frete.
- `OrderItemService.unitPriceFor(produto, modo)` escolhe o preço pelo modo,
  usado por `OrderService` (pedido e preview) e por `CartPreviewService`.
- `ProductSortEnum` ordena por `priceDelivery`; `ReorderItemDTO` expõe os
  dois preços.
- Frontend: `product-card` (linhas `price-delivery`/`price-pickup` e o selo
  `free-delivery`), lista do fornecedor (`manage-price-*`), formulário de
  produto (`shippingFee` + "Entrega grátis"), ordenação da vitrine
  (`priceDelivery,asc|desc`), `ReorderItem`.

## 5. Contrato Técnico

### 5.1 Migração

- **`V18__product_single_price.sql`** (MySQL; V16 é da HU18 e V17 da HU19):
  ```sql
  ALTER TABLE product_items ADD COLUMN price DECIMAL(10,2) NULL;
  UPDATE product_items SET price = price_pickup;
  ALTER TABLE product_items MODIFY price DECIMAL(10,2) NOT NULL;
  ALTER TABLE product_items DROP COLUMN price_delivery, DROP COLUMN price_pickup;
  ```
  As colunas antigas saem nesta mesma migração, por decisão do autor: só
  há o ambiente local, e o risco de deploy é baixo. Isso revoga a
  separação em duas migrações que a HU17 previa.

### 5.2 Backend

- `ProductItem.price` (`BigDecimal`, obrigatório); saem `priceDelivery` e
  `pricePickup`.
- `ProductItemDTO`: fica `price`; saem `priceDelivery`, `pricePickup`,
  `shippingFee` e `freeDelivery`. "Entrega grátis" passa a ser atributo da
  loja, na HU17.
- `CreateProductItemDTO`: sai `shippingFee`; `price` segue obrigatório e
  maior ou igual a zero. O Jackson ignora um `shippingFee` enviado por
  cliente antigo, e ele não é gravado.
- `OrderItemService.unitPriceFor(produto)`: devolve o `price`, sem modo.
  Usado pelo pedido, pelo preview do pedido e pelo preview do carrinho.
- `ProductSortEnum`: `price,asc` e `price,desc`, no lugar de
  `priceDelivery,…`.
- `ReorderItemDTO`: `price` no lugar dos dois preços.
- OpenAPI atualizado nas rotas de produto (descrições do cadastro, da
  edição e do parâmetro `sort`).

### 5.3 Frontend

- `ProductItem`: `price`; saem `priceDelivery`, `pricePickup`,
  `shippingFee` e `freeDelivery`. O mesmo vale para `ReorderItem`.
- `product-card`: um preço (`data-testid="product-price"`); saem
  `price-delivery`, `price-pickup` e o selo `free-delivery`.
- Lista do fornecedor: um preço (`manage-price`).
- Formulário: um campo "Preço"; saem "Preço do frete", "Entrega grátis" e
  o resumo "retirada/entrega".
- Vitrine: ordenação `price,asc` e `price,desc`.

## 6. Plano de Testes (TDD)

**Backend**
1. `ProductItemMapperTest`: `deveExporUmPrecoSo` (DTO sem os campos
   antigos).
2. `CreateProductItemMapperTest` (ou o teste do serviço):
   `deveGravarOPrecoUnico`, `deveIgnorarFreteEnviadoPorClienteAntigo`.
3. `OrderItemServiceTest`: `deveUsarOMesmoPrecoNaEntregaENaRetirada`.
4. `OrderServiceTest` e `CartPreviewServiceTest`: os testes de preço por
   modo passam a esperar o preço único.
5. `ProductSortEnumTest` / `ProductItemControllerTest`: `price,asc` ordena
   pelo preço.
6. Migração: conferida no MySQL local e registrada no DoD (o Flyway não
   roda no `mvn test`).

**Frontend**
7. `product-card.component.spec.ts`: `deveMostrarUmPrecoSo` (substitui os
   asserts de entrega/retirada e do selo).
8. `product-item-form.component.spec.ts`: `deveTerUmUnicoCampoDePreco`,
   `deveEnviarSoOPreco`.
9. `product-item.component.spec.ts`: a lista mostra um preço.
10. `supplier-list.component.spec.ts`: ordenação por `price,asc`.

**E2E (registrado no DoD)**: depois da migração, a vitrine, a página da
loja e a lista do fornecedor com um preço; cadastrar e editar um produto
com um preço; carrinho e resumo com o preço único. Em 390 e 1280 px.

## 7. Fora de Escopo

- Frete por distância, configuração de entrega da loja e "Entrega grátis"
  da loja: HU17.
- Preço diferente por modalidade (foi o modelo da HU10, agora revogado).
- Histórico de preço.

## 8. Definição de Pronto

- [x] `V18` aplicada no MySQL local, com `price` = antigo preço de
      retirada e nenhuma linha perdida.
- [x] Preço único em toda a cadeia: formulário, lista do fornecedor,
      catálogo, carrinho, pedido, pedir de novo e ordenação.
- [x] Rotas de produto com o OpenAPI atualizado.
- [x] Testes da seção 6 verdes; `mvn test` (448, só o `contextLoads` de
      ambiente) e `ng test` (só as 10 falhas antigas de provider) sem falha
      nova; `ng build` ok.
- [x] E2E em 390 e 1280 px registrado aqui com data.
- [x] Nada do "Fora de Escopo".

### Registro do teste manual (2026-10-28)

- Antes da V18: dump do banco no scratchpad da sessão e lista `id,
  price_pickup` dos 29 produtos. Depois da V18 (Flyway no boot do
  backend): os mesmos 29 ids, com `price` igual ao antigo `price_pickup`,
  e as colunas `price_delivery` e `price_pickup` removidas.
- Swagger: `ProductItemDTO` só com `price`; `CreateProductItemDTO` sem
  `shippingFee`; `sort` com `price,asc` e `price,desc`.
- Vitrine `?view=products` em 390 e 1280 px, como visitante, com as lojas
  de teste abertas à noite: cada card mostra um preço só (por exemplo
  Copagaz R$ 99,00 e Liquigás R$ 104,00), sem as linhas "com entrega" e
  "Retirada na loja" e sem o selo de frete do produto.
- O formulário de produto do fornecedor (exige login) ficou coberto pelos
  specs. Não foi aberto no navegador.
