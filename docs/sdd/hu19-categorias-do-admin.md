# HU19 — Categorias de produto cadastradas pelo admin

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU19 |
| Perfil | ADMIN e Fornecedor |
| Prioridade MoSCoW | Should have — decisão do autor em 2026-10-27 ("deixar categorias cadastráveis apenas para o ADM, junto a tipo") |
| Dependências | F3 (Flyway), F6 (OpenAPI), HU10 (catálogo do fornecedor), HU12 (tipos do admin), HU15 (tipos que a loja vende), HU18 (V16) |
| Referência no TCC | Revisão da hierarquia do catálogo da HU12 |

## 2. História de Usuário

Como administrador, quero manter uma lista única de categorias (P13, P45,
Galão 20L…) por tipo de produto, para que todas as lojas usem os mesmos
nomes; e como fornecedor, quero só escolher a categoria certa ao cadastrar
um produto, sem precisar criá-la.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Admin cadastra categoria**: Dado o ADMIN em
  `/admin/categorias`, Quando escolhe o tipo e informa o nome, Então a
  categoria fica disponível para todos os fornecedores.
- **Cenário 2 — Nome único por tipo**: Dado a categoria "P13" em Gás,
  Quando o ADMIN tenta outra "p13" em Gás, Então vê "Categoria já
  cadastrada."; "P13" em Água é permitido.
- **Cenário 3 — Renomear e excluir**: Dado uma categoria, Quando o ADMIN a
  renomeia, Então os produtos passam a mostrar o nome novo; Quando tenta
  excluir uma categoria com produtos, Então vê quantos produtos a usam e a
  exclusão é recusada.
- **Cenário 4 — Fornecedor só escolhe**: Dado o fornecedor cadastrando ou
  editando produto, Então a lista mostra **todas** as categorias,
  agrupadas por tipo; ele não cria, edita nem exclui categoria (403), e a
  tela "Categoria de produtos" sai do menu dele.
- **Cenário 5 — Dados existentes unificados**: Dado lojas que já tinham
  criado "P13" em Gás cada uma, Quando a migração roda, Então fica uma
  categoria só e todos os produtos apontam para ela; nenhum produto perde a
  categoria.
- **Cenário 6 — Vitrine segue certa**: Dado uma loja, Então os selos de
  tipo do card e o filtro por tipo da vitrine (HU15) refletem os produtos
  que ela vende de fato, não as categorias.

## 4. Estado atual (achados)

- `CategoryProduct.supplier` (coluna `category_products.supplier_id`, FK
  com nome gerado pelo Hibernate, sem `CREATE TABLE` versionado) — cada
  fornecedor cria as próprias categorias em `/api/category-products`
  (POST/PUT/DELETE só SUPPLIER), com nome único por fornecedor + tipo (409).
- `ProductItemService` recusa categoria de outro fornecedor (`:150-152` e
  `:195-204`).
- A HU15 deriva "tipos que a loja vende" de `category_products.supplier`
  (`CategoryProductRepository.findTypesBySupplierIds`,
  `SupplierRepository.CATALOG_WHERE`).
- O formulário de produto usa `GET /by-supplier/{id}` com página de 10
  (`product-item-form.component.ts:185-197`): **só as 10 primeiras
  categorias aparecem**.
- O filtro de categoria do dashboard do fornecedor usa `GET /mine`.
- **Revoga a HU12**: Cenário 7 ("produtos e categorias, do fornecedor"),
  o Fora de Escopo "categorias globais curadas pelo admin; unificar dados
  existentes" e a permissão "escrita de categoria só SUPPLIER".

## 5. Contrato Técnico

### 5.1 Dados
- `V17__category_products_global.sql` (MySQL): canônica = `MIN(id)` por
  `product_type_id, LOWER(TRIM(name))`; `UPDATE product_items` para a
  canônica; `DELETE` das duplicadas; descobre a FK de `supplier_id` em
  `information_schema` e a remove (padrão de `V2`); `DROP COLUMN
  supplier_id`; `UNIQUE (product_type_id, name)`.
- Entidade: saem `CategoryProduct.supplier` e `Supplier.categoryProducts`
  (apagar fornecedor deixa de apagar categorias).

### 5.2 API (`/api/category-products`, paths mantidos)

| Método | Rota | Papel | Respostas |
|---|---|---|---|
| GET | `/api/category-products` | **pública** | 200 lista com tipo, ordenada por tipo e nome |
| POST | `/api/category-products` | **ADMIN** | 201, 400 ("Categoria já cadastrada."), 401, 403, 404 (tipo) |
| PUT | `/api/category-products/{id}` | **ADMIN** | 200, 400, 401, 403, 404 |
| DELETE | `/api/category-products/{id}` | **ADMIN** | 204, 401, 403, 404, 409 (tem produtos) |
| GET | `/api/category-products/mine` | SUPPLIER | 200 — categorias que a loja **usa**, com a contagem dos produtos dela |

- Sai `GET /by-supplier/{supplierId}`.
- `ProductItemService`: saem as checagens de dono da categoria; fica o 404.
- HU15: tipos da loja e o filtro `productTypeId` passam a vir de
  `ProductItem` (`EXISTS (SELECT 1 FROM ProductItem p WHERE p.supplier = s
  AND p.categoryProduct.productType.id = :productTypeId)`).

### 5.3 Frontend
- Nova `/admin/categorias` (`admin-categories`), no padrão de
  `admin-product-types`: lista agrupada por tipo, criar e renomear inline,
  excluir com confirmação, sugestões de nome (P13, P45, 20L…).
- Menu do admin ganha "Categorias"; saem a tela `category-products` e o
  link "Categoria de produtos" do fornecedor.
- Formulário de produto lista `GET /api/category-products` (todas).

## 6. Plano de Testes (TDD)

**Backend**
1. `CategoryProductServiceTest`: `deveCriarCategoriaGlobal`,
   `deveRecusarNomeRepetidoNoMesmoTipo`, `devePermitirMesmoNomeEmOutroTipo`,
   `deveRenomearCategoria`, `deveRecusarExcluirCategoriaComProdutos`,
   `deveListarAsCategoriasQueALojaUsa`.
2. `CategoryProductControllerTest`: `deveCriarComoAdmin` (201),
   `deveRetornar403AoFornecedorCriar`, `deveListarSemToken`.
3. `CatalogoPermissoesTest`: `deveRetornar403AoAdminEscreverCategoria`
   inverte para `deveRetornar403AoFornecedorEscreverCategoria`.
4. `ProductItemServiceTest`: `deveCadastrarProdutoComQualquerCategoria`
   (saem os testes "categoria de outro fornecedor → 403").
5. `SupplierServiceTest` (HU15): `deveTrazerOsTiposPelosProdutosDaLoja`,
   `deveFiltrarLojasPeloTipoDosProdutos`.

**Frontend**
6. `admin-categories.component.spec.ts`: listar, criar, renomear, erro de
   duplicada, excluir com confirmação.
7. `menu.component.spec.ts`: "Categorias" no admin, sem "Categoria de
   produtos" no fornecedor.
8. `product-item-form.component.spec.ts`: `deveListarTodasAsCategorias`.

**Migração (registrada no DoD)**: no MySQL local, duas lojas com "P13" em
Gás → uma categoria, produtos repostos,
`SELECT COUNT(*) FROM product_items WHERE category_product_id NOT IN (SELECT id FROM category_products)` = 0.

## 7. Fora de Escopo

- Excluir tipo de produto (segue fora desde a HU12).
- Fornecedor sugerir categoria nova ao admin.
- Ícone ou foto por categoria.
- Renomear as rotas da API de categorias (o F8 trata só das telas).

## 8. Definição de Pronto

- [ ] Categorias globais, cadastradas só pelo ADMIN; migração `V17`
      aplicada e conferida no MySQL local.
- [ ] Fornecedor escolhe entre todas as categorias e não escreve nenhuma.
- [ ] Vitrine (selos e filtro por tipo) derivada dos produtos.
- [ ] Swagger com os papéis novos e sem `/by-supplier/{id}`.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E em 390 e 1280 px registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
