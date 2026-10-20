# HU10 — Gestão de Catálogo e Precificação Logística

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU10 |
| Perfil | Fornecedor (SUPPLIER); cliente (CUSTOMER) vê a etiqueta |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (colunas `priceDelivery`/`pricePickup`), HU03 (cards do cliente já mostram os dois valores), HU09 (RN02 e `onlyAvailable`), F7 (padrão visual) |
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
> HU10 remove. Decisões do autor: (a) da Figura 16 entra **só o
> formulário em seções**, mantendo as duas páginas atuais; (b) o campo
> **SKU** **não entra**; (c) a etiqueta "Entrega Grátis" aparece para o
> **cliente e para o fornecedor**.
>
> **Revisão final (a pedido do autor)**: o fornecedor informa apenas
> **"Preço"** e **"Preço de frete"** — não mais "Preço para Retirada" e
> "Preço com Entrega" (texto do Apêndice A, que passa a ser **reinterpretado**
> neste ponto). Por baixo nada migra: **retirada = Preço** e **entrega =
> Preço + frete**, derivados na API, de modo que os cards, o carrinho, o
> checkout e o cálculo do pedido (HU03/HU05, RN01) seguem iguais. **Frete
> = 0 é Entrega Grátis** (o checkbox da revisão anterior sai).

- **Cenário 1 — Precificação dividida**: Dado o formulário de novo
  produto, Quando o fornecedor preenche os dados financeiros, Então o
  sistema exige a inserção obrigatória e separada de **"Preço"** e
  **"Preço de frete"** (reinterpretação dos dois preços do Apêndice A:
  Preço = retirada; Preço + frete = entrega).
- **Cenário 2 — Regra de Entrega Grátis**: Dado a edição de um item,
  Quando o fornecedor informa **frete zero** (preço de entrega igual ao de
  retirada), Então o sistema salva a configuração e o front-end passa a
  renderizar a etiqueta de "Entrega Grátis" para aquele produto.

Detalhamento operacional (não cria cenário novo):

- **1a — Obrigatório no backend**: os dois valores são validados na API
  (`@Valid`, 400 com a mensagem do campo), não só no formulário.
- **1b — Prévia**: o formulário mostra ao vivo "Retirada: R$ x · Entrega:
  R$ y", para o fornecedor ver o que o cliente vai pagar.
- **2a — Etiqueta computada**: "Entrega Grátis" = frete zero (`priceDelivery
  == pricePickup`), calculada na leitura, nunca gravada.
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
  - `@NotNull @DecimalMin("0.0") BigDecimal price` — "Preço" (valor para
    retirada);
  - `@NotNull @DecimalMin("0.0") BigDecimal shippingFee` — "Preço de
    frete" (acréscimo da entrega; zero = Entrega Grátis);
  - `@NotBlank String brand`, `@NotNull Long categoryProductId`,
    `@Min(0) Integer stockQuantity` (evita 500 e estoque negativo);
  - `priceDelivery`/`pricePickup` deixam de fazer parte da entrada.
- **Derivação (sem migração)** em `CreateProductItemMapper.toEntity` e no
  `updateProductItem`: `pricePickup = price`; `priceDelivery = price +
  shippingFee`. As colunas e tudo que as lê continuam como estão.
- `ProductItemDTO` (resposta): mantém `priceDelivery` e `pricePickup`
  (cards, carrinho e checkout dependem deles) e ganha `price`
  (`= pricePickup`) e `shippingFee` (`= priceDelivery − pricePickup`) para
  o formulário de edição; `boolean freeDelivery` computado (frete zero);
  `categoryProductId` é o da **categoria do fornecedor**.
- `ProductItemMapper.toDto`: nenhum preço nulo quebra o mapeamento
  (`price`/`shippingFee`/`freeDelivery` ficam nulos/falsos).
- `ProductItemService.updateProductItem(id, dto, actor)`: produto de
  outro fornecedor → 403 (ADMIN pode); categoria nova precisa ser do mesmo
  fornecedor do produto (inclusive categoria sem fornecedor → 403).
- `ProductItemController`: `POST` (`@RequestPart("product") @Valid`) e
  `PUT /{id}` (`@Valid @RequestBody`) validam o DTO; **OpenAPI** nas duas
  (POST: 200/400/403/404/502; PUT: 200/400/403/404).
- Sem migração: as colunas já existem e já são `NOT NULL`.

### 5.2 Frontend

- `models/product-item.ts`: adiciona `price?` e `shippingFee?` (preço de
  retirada e acréscimo de entrega) e mantém `priceDelivery`/`pricePickup`/
  `freeDelivery?` para leitura.
- `product-item-form` em seções, como a Figura 16, nos tokens F7 (sem HEX
  novo, `rounded-xl`):
  - **1. Informações básicas**: Marca, Categoria, Descrição;
  - **2. Preços e estoque**: **"Preço (R$)"** e **"Preço de frete (R$)"**
    (obrigatórios, `min(0)`, lado a lado) e Estoque; abaixo, a **prévia**
    "Retirada: R$ x · Entrega: R$ y" ao vivo e, quando o frete é 0, um
    aviso curto "Frete zero: este produto terá a etiqueta Entrega
    Grátis";
  - **3. Imagem**: arrasta-e-solta (`dragover`/`drop`) além do clique,
    com pré-visualização e remover;
  - no **cadastro** a imagem é obrigatória (a API exige `file`): sem ela
    avisa "Selecione uma imagem do produto."; na edição a imagem não muda;
  - **Cancelar** (volta para `/product-items`) e **Salvar Produto**; erros
    do backend pelo helper `backendMessage`;
  - envia só `price` e `shippingFee` (nunca `priceDelivery`/`pricePickup`);
    na edição carrega `price` e `shippingFee` da API.
- `product-card` (vitrine do cliente): selo **"Entrega Grátis"** quando
  `freeDelivery`, e continua exibindo Entrega e Retirada.
- `product-item` (gestão do fornecedor): cada linha mostra Entrega e
  Retirada, o frete e o selo "Entrega Grátis".

## 6. Plano de Testes (TDD)

**Backend:**
1. `ProductItemServiceTest` — atualizar os que usavam preços únicos; novos:
   `deveDerivarPrecoDeEntregaSomandoOFrete`,
   `deveSinalizarEntregaGratisQuandoOFreteForZero`,
   `deveAtualizarPrecoEFreteAoEditarProduto`,
   `deveRejeitarEdicaoDeProdutoDeOutroFornecedor` (403),
   `deveRejeitarMoverProdutoParaCategoriaDeOutroFornecedor` (403),
   `deveRejeitarMoverProdutoParaCategoriaSemFornecedor` (403),
   `deveManterCategoriaQuandoEdicaoReenviaOMesmoId`.
2. `ProductItemMapperTest` — `deveExporPrecoEFreteDerivadosDosDoisPrecos`,
   `deveSinalizarFreteGratisQuandoOFreteForZero`,
   `naoDeveSinalizarFreteGratisQuandoHouverFrete`,
   `naoDeveQuebrarQuandoAlgumPrecoForNulo`,
   `deveExporOIdDaCategoriaDoFornecedor`.
3. `ProductItemControllerTest` — `deveRejeitarCriacaoSemPreco` (400),
   `deveRejeitarCriacaoSemFrete` (400),
   `deveRejeitarCriacaoComFreteNegativo` (400),
   `deveRejeitarEdicaoSemPrecoEFrete` (400),
   `deveAceitarCriacaoComPrecoEFreteValidos`,
   `deveAceitarEdicaoComFreteZero`.

**Frontend:**
4. `product-item-form.component.spec.ts` —
   `deveExigirPrecoEFreteParaSubmeter`, `naoDeveAceitarValorNegativo`,
   `deveEnviarPriceEShippingFeeAoSalvar`,
   `deveMostrarPreviaDeRetiradaEEntrega`,
   `deveAvisarEntregaGratisQuandoOFreteForZero`,
   `naoDeveAvisarQuandoHouverFrete`,
   `deveAceitarImagemSoltaNaAreaDeUpload`,
   `deveVoltarParaListaAoCancelar`, `deveExigirImagemNoCadastro`,
   `deveIgnorarArquivoQueNaoEImagem`, `deveCarregarPrecoEFreteNaEdicao`,
   `deveSalvarAEdicaoSemExigirNovaImagem`.
5. `product-card.component.spec.ts` —
   `deveExibirBadgeDeFreteGratisQuandoFreteZero`,
   `naoDeveExibirBadgeQuandoHouverFrete`.
6. `product-item.component.spec.ts` —
   `deveExibirOsDoisPrecosEBadgeNaGestao`.

Regra herdada: specs antigos falhando no frontend não aumentam (19).

## 7. Fora de Escopo

- Campo **SKU / Código de Referência** da Figura 16 (exigiria migração
  V8) e a **tela dividida** lista + formulário.
- **Migrar o banco** para `price` + `shipping_fee`: as colunas
  `price_delivery`/`price_pickup` ficam, e o par `price`/`shippingFee` é
  só o contrato do fornecedor.
- Persistir "frete grátis" como coluna — fica computado.
- Descontos, precificação dinâmica e promoções.
- Exigir preço maior que zero (mantém `>= 0`).
- Checagem de dono em `DELETE` e `GET /{id}` de produto (dívida das
  rotas antigas) e troca de imagem na edição.
- Correção retroativa de produtos no banco: os atuais já têm entrega >=
  retirada, então o frete derivado nunca é negativo.

## 8. Definição de Pronto

- [ ] Cadastro e edição exigem `price` e `shippingFee` separados, com
      validação na API (400 se ausente ou negativo), e gravam
      `pricePickup = price` e `priceDelivery = price + shippingFee`.
- [ ] Editar produto não troca a categoria por engano e só vale para
      produto do próprio fornecedor (403 nos demais).
- [ ] `ProductItemDTO` expõe `price`, `shippingFee` e `freeDelivery`
      computados, sem quebrar `priceDelivery`/`pricePickup`.
- [ ] Rotas `POST`/`PUT` documentadas no Swagger.
- [ ] Formulário em seções (Figura 16) com "Preço" e "Preço de frete"
      obrigatórios, prévia de Retirada/Entrega, aviso de frete zero,
      upload arrasta-e-solta e Cancelar.
- [ ] Selo "Entrega Grátis" na vitrine do cliente e na gestão do
      fornecedor; cards, carrinho e checkout continuam com Entrega e
      Retirada.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 19 falhas
      antigas); sem HEX novo.
- [ ] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [ ] Testado de ponta a ponta (API e interface).
- [ ] Revisado e aprovado. Fecha os 10 Must-have; o próximo é a HU11
      (autenticação por token), início do Should-have.
