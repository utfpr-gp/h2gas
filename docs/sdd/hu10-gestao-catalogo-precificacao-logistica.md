# HU10 — Gestão de Catálogo e Precificação Logística

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU10 |
| Perfil | Fornecedor (SUPPLIER); cliente (CUSTOMER) vê a etiqueta |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (`priceDelivery`/`pricePickup`), HU03 (cards do cliente já mostram os dois preços), HU09 (RN02 e `onlyAvailable`), F7 (padrão visual) |
| Referência no TCC | Apêndice A.1 — HU10 (p. 53); Figura 16 (p. 41) |

## 2. História de Usuário

Como fornecedor, quero cadastrar e editar meus produtos definindo
valores distintos para retirada e entrega, para precificar minha
operação de acordo com o custo logístico individual de cada item.

## 3. Critérios de Aceitação (BDD)

> Revisado em 2026-10-20 sobre a `develop` com HU01–HU09 mergeadas. A
> versão anterior dizia que o backend "não compila desde o F3" — hoje ele
> compila: a HU03/HU09 deixaram uma **ponte provisória** com um `price`
> único que grava o mesmo valor nas duas colunas, e é essa ponte que a
> HU10 remove. Decisões do autor nesta revisão: (a) da Figura 16 entra
> **só o formulário em seções** (1. Informações básicas, 2. Preços e
> estoque, 3. Imagem), mantendo as duas páginas atuais (lista e
> formulário); (b) o campo **SKU** da figura **não entra** (o Apêndice A
> não o pede e exigiria migração); (c) a etiqueta "Entrega Grátis" aparece
> para o **cliente e para o fornecedor**, e o formulário **avisa ao vivo**
> quando os dois preços são iguais.

- **Cenário 1 — Precificação dividida**: Dado o formulário de novo
  produto, Quando o fornecedor preenche os dados financeiros, Então o
  sistema exige a inserção obrigatória e separada dos campos "Preço para
  Retirada" e "Preço com Entrega".
- **Cenário 2 — Regra de Entrega Grátis**: Dado a edição de um item,
  Quando o fornecedor informa valores idênticos para retirada e entrega,
  Então o sistema salva a configuração e o front-end passa a renderizar a
  etiqueta de "Entrega Grátis" para aquele produto.

Detalhamento operacional (não cria cenário novo):

- **1a — Obrigatório no backend**: os dois preços são validados na API
  (`@Valid`, 400 com a mensagem do campo), não só no formulário.
- **2a — Etiqueta computada**: "Entrega Grátis" = `priceDelivery ==
  pricePickup`, calculada na leitura, nunca gravada.
- **2b — Editar com segurança**: o fornecedor só edita produto da própria
  loja (403 nos demais) e só move o produto para categoria dele.

## 4. Estado atual (achados)

- **Ponte provisória do `price` único** (HU03/HU09 só tornaram o backend
  compilável): `CreateProductItemDTO.price`; `CreateProductItemMapper`
  copia `price` para `priceDelivery` **e** `pricePickup`;
  `ProductItemService.updateProductItem` faz o mesmo; `ProductItemDTO`
  ainda expõe `price` (= `priceDelivery`). Ou seja, hoje é **impossível**
  cadastrar preços diferentes — o Cenário 1 não é atendido.
- Frontend: `product-item-form` tem um campo "Preço (R$)" (`price`); o
  modelo `ProductItem` ainda tem `price`; a lista de gestão
  (`product-item`) mostra um preço só. O `product-card` (vitrine) já
  exibe Entrega e Retirada (HU03), mas sem etiqueta.
- **Nenhum endpoint de criação/edição usa `@Valid`**, e o handler global
  de `MethodArgumentNotValidException` **já existe** (400 com
  `campo: mensagem`) — basta anotar. Criar com `categoryProductId` nulo
  hoje estoura 500.
- **Defeito na edição (achado novo)**: `ProductItemMapper.toDto` preenche
  `categoryProductId` com o id do **tipo** (`ProductType`: Gás/Água), não
  o da categoria do fornecedor. O formulário de edição carrega esse valor
  e o reenvia; `updateProductItem` então troca a categoria do produto
  para a de **mesmo id** (sem checar o dono) sempre que ele difere do id
  real. Corrige-se no mapper (e o `ProductItemDTO` ganha o id correto).
- **Sem checagem de dono na edição**: `PUT /api/product-items/{id}` não
  verifica se o produto é do fornecedor logado (a criação checa a
  categoria). Entra no escopo só a **edição** (é a rota que a HU10 altera);
  `DELETE` e `GET /{id}` com a mesma lacuna ficam fora.
- **Rotas sem OpenAPI**: `POST` e `PUT` de produto não têm
  `@Operation`/`@ApiResponses` (CLAUDE.md exige em rota nova ou alterada).
- Figura 16 (TCC): tela dividida "Meus Produtos" + "Cadastrar Novo
  Produto", com seções e campo SKU; mostra **um** preço de venda — o texto
  do Apêndice A (dois preços) é o que vale para a HU10.

## 5. Contrato Técnico

### 5.1 Backend

- `CreateProductItemDTO` (todos com `@Schema`):
  - `@NotNull @DecimalMin("0.0") BigDecimal priceDelivery` — "Preço com
    Entrega";
  - `@NotNull @DecimalMin("0.0") BigDecimal pricePickup` — "Preço para
    Retirada";
  - `@NotBlank String brand`, `@NotNull Long categoryProductId`,
    `@Min(0) Integer stockQuantity` (evita 500 e estoque negativo);
  - o campo `price` deixa de existir.
- `CreateProductItemMapper`: `toEntity`/`toDto` passam a mapear os dois
  preços (sem a cópia do `price`).
- `ProductItemDTO`: remove `price`; adiciona `boolean freeDelivery`
  (computado); `categoryProductId` passa a ser o id da **categoria do
  fornecedor** (`CategoryProduct`), não do tipo.
- `ProductItemMapper.toDto`: `freeDelivery = priceDelivery != null &&
  pricePickup != null && priceDelivery.compareTo(pricePickup) == 0`;
  nenhum preço nulo quebra o mapeamento.
- `ProductItemService.updateProductItem(id, dto, actor)`:
  - produto de outro fornecedor → `ForbiddenResourceException` (403);
    ADMIN pode;
  - `categoryProductId` diferente da atual: a nova categoria precisa ser
    do **mesmo fornecedor do produto**, senão 403;
  - grava `priceDelivery` e `pricePickup` separados.
- `ProductItemController`:
  - `POST` (`@RequestPart("product") @Valid CreateProductItemDTO`) e
    `PUT /{id}` (`@Valid @RequestBody`) validam o DTO; `PUT` passa o
    principal ao service;
  - **OpenAPI** nas duas rotas: `@Operation`, `@ApiResponses` (POST:
    200/400/403/404/502; PUT: 200/400/403/404) e `@Parameter`.
- Sem migração: as colunas já existem e já são `NOT NULL`.

### 5.2 Frontend

- `models/product-item.ts`: remove `price`; adiciona `freeDelivery?`.
- `product-item-form` refeito em seções, como a Figura 16, nos tokens F7
  (sem HEX novo, `rounded-xl`):
  - **1. Informações básicas**: Marca, Categoria, Descrição;
  - **2. Preços e estoque**: "Preço com Entrega" e "Preço para Retirada"
    (obrigatórios, `min(0)`, lado a lado) e Estoque; **aviso ao vivo**
    "Preços iguais: este produto terá Entrega Grátis" quando os dois
    forem iguais e preenchidos;
  - **3. Imagem**: área arrasta-e-solta (`dragover`/`drop`) além do
    clique, com pré-visualização e remover;
  - no **cadastro** a imagem é obrigatória (a API exige a parte `file`):
    sem ela o formulário avisa "Selecione uma imagem do produto." em
    vez de deixar a API devolver erro; na edição a imagem não muda;
  - botões **Cancelar** (volta para `/product-items`) e **Salvar
    Produto**; erros do backend pelo helper `backendMessage`;
  - envia `priceDelivery`/`pricePickup` (nunca `price`); na edição o
    `categoryProductId` vem correto da API.
- `product-card` (vitrine do cliente): selo **"Entrega Grátis"** quando
  `freeDelivery`.
- `product-item` (gestão do fornecedor): cada linha mostra Entrega e
  Retirada (não mais um preço só) e o selo "Entrega Grátis".

## 6. Plano de Testes (TDD)

**Backend:**
1. `ProductItemServiceTest` — atualizar os que usavam `setPrice`/`price`;
   novos: `deveCriarProdutoComPrecoDeEntregaEDeRetiradaDiferentes`,
   `deveAtualizarAmbosPrecosAoEditarProduto`,
   `deveRejeitarEdicaoDeProdutoDeOutroFornecedor` (403),
   `deveRejeitarMoverProdutoParaCategoriaDeOutroFornecedor` (403),
   `deveManterCategoriaQuandoEdicaoReenviaOMesmoId`.
2. `ProductItemMapperTest` —
   `deveSinalizarFreteGratisQuandoPrecosForemIguais`,
   `naoDeveSinalizarFreteGratisQuandoPrecosDiferentes`,
   `naoDeveQuebrarQuandoAlgumPrecoForNulo`,
   `deveExporOIdDaCategoriaDoFornecedor` (e não o do tipo).
3. `ProductItemControllerTest` —
   `deveRejeitarCriacaoSemPrecoDeEntrega` (400),
   `deveRejeitarCriacaoSemPrecoDeRetirada` (400),
   `deveRejeitarCriacaoComPrecoNegativo` (400),
   `deveRejeitarEdicaoSemPrecos` (400),
   `deveAceitarCriacaoComAmbosOsPrecosValidos`,
   `deveAceitarEdicaoComAmbosOsPrecosValidos`.

**Frontend:**
4. `product-item-form.component.spec.ts` —
   `deveExigirAmbosOsCamposDePrecoParaSubmeter`,
   `deveEnviarPriceDeliveryEPricePickupAoSalvar`,
   `deveAvisarEntregaGratisQuandoPrecosForemIguais`,
   `naoDeveAvisarQuandoPrecosDiferentes`,
   `deveAceitarImagemSoltaNaAreaDeUpload`,
   `deveVoltarParaListaAoCancelar`, `deveExigirImagemNoCadastro`,
   `deveIgnorarArquivoQueNaoEImagem`, `deveCarregarOsDoisPrecosNaEdicao`,
   `deveSalvarAEdicaoSemExigirNovaImagem`.
5. `product-card.component.spec.ts` —
   `deveExibirBadgeDeFreteGratisQuandoPrecosIguais`,
   `naoDeveExibirBadgeQuandoPrecosDiferentes`.
6. `product-item.component.spec.ts` —
   `deveExibirOsDoisPrecosEBadgeNaGestao`.

Regra herdada: specs antigos falhando no frontend não aumentam (21).

## 7. Fora de Escopo

- Campo **SKU / Código de Referência** da Figura 16 (exigiria migração
  V8) e a **tela dividida** lista + formulário.
- Persistir "frete grátis" como coluna — fica computado.
- Descontos, precificação dinâmica e promoções.
- Exigir preço maior que zero (o script mantém `>= 0`, como no Apêndice).
- Checagem de dono em `DELETE` e `GET /{id}` de produto (dívida das
  rotas antigas) e troca de imagem na edição.
- Correção retroativa de produtos no banco além do que o F3 migrou.

## 8. Definição de Pronto

- [ ] Cadastro e edição exigem e persistem `priceDelivery` e
      `pricePickup` separados, com validação na API (400 se ausente ou
      negativo) e `price` removido do contrato.
- [ ] Editar produto não troca a categoria por engano e só vale para
      produto do próprio fornecedor (403 nos demais).
- [ ] `ProductItemDTO` expõe `freeDelivery` computado.
- [ ] Rotas `POST`/`PUT` documentadas no Swagger.
- [ ] Formulário em seções (Figura 16) com dois preços obrigatórios,
      aviso de Entrega Grátis ao vivo, upload arrasta-e-solta e Cancelar.
- [ ] Selo "Entrega Grátis" na vitrine do cliente e na gestão do
      fornecedor.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 21 falhas
      antigas); sem HEX novo.
- [ ] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [ ] Testado de ponta a ponta (API e interface).
- [ ] Revisado e aprovado. Fecha os 10 Must-have; o próximo é a HU11
      (autenticação por token), início do Should-have.
