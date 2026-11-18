# HU15 — Vitrine em cards com o tipo de produto de cada loja

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU15 |
| Perfil | Cliente (CUSTOMER) e Visitante |
| Prioridade MoSCoW | Should have — pedido do autor em 2026-10-26, com a vitrine do iFood como referência visual |
| Dependências | F2 (tokens), F7 §5.2/§5.6/§5.21/§5.25/§5.26 (vitrine, card de loja, nota, banner), HU01 (RN05), HU02 (chips de tipo), HU12 §5.2.2 (ícone do tipo), HU13 (vitrine pública) |
| Referência no TCC | Figura 11 (catálogo mobile-first); extensão da HU02 |

## 2. História de Usuário

Como cliente (ou visitante) na vitrine, quero ver as lojas em cards que
mostrem o que cada uma vende, e poder tocar em "Gás" ou "Água" para ver
só as lojas daquele tipo, para escolher onde comprar sem precisar abrir
loja por loja.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Lojas em cards**: Dado um cliente em `/inicio` na visão
  "Lojas", Quando a lista carrega, Então as lojas aparecem em cards numa
  grade (1 coluna no celular, 2 em tablet, 4 no desktop), com a mesma
  linguagem visual dos cards de produto: imagem no topo, nome, nota e
  ação.
- **Cenário 2 — O card diz o que a loja vende**: Dado uma loja que tem
  categorias de Gás e de Água, Quando o card dela é exibido, Então mostra
  um selo para cada tipo, com o ícone que o admin gravou no tipo (HU12
  §5.2.2) e o nome ("Gás", "Água"), sem repetir o mesmo tipo quando a
  loja tem várias categorias dele.
- **Cenário 2a — A faixa de tipos aparece nas duas visões**: Dado o cliente
  em `/inicio`, Quando alterna entre "Lojas" e "Produtos", Então a mesma faixa
  de ícones circulares (Todas, Gás, Água…) continua visível, logo abaixo da
  busca, e o tipo escolhido vale para a visão em que ele está.
- **Cenário 2b — Vitrine sem banners promocionais**: Dado a visão "Lojas",
  Então não há banners de "Fardinho de Água" nem de "Gás de Cozinha e
  Industrial" — a lista de lojas começa logo após a faixa de tipos.
- **Cenário 3 — Faixa de tipos filtra as lojas**: Dado a visão "Lojas",
  Quando o cliente toca no chip "Gás", Então a lista passa a mostrar só
  as lojas que têm ao menos uma categoria daquele tipo, e o chip fica
  marcado (`aria-pressed="true"`).
- **Cenário 4 — Voltar a ver todas**: Dado um tipo selecionado, Quando o
  cliente toca em "Todas", Então o filtro sai e a lista volta a mostrar
  todas as lojas da cidade.
- **Cenário 5 — Loja sem catálogo montado**: Dado uma loja aberta que
  ainda não cadastrou nenhuma categoria, Quando a lista carrega sem
  filtro de tipo, Então ela aparece normalmente, só que sem selo de tipo;
  Quando há filtro de tipo, Então ela não aparece.
- **Cenário 6 — Filtro combina com busca e cidade**: Dado o cliente com
  um tipo selecionado, Quando digita parte do nome de uma loja, Então o
  resultado respeita os dois filtros e continua restrito à cidade do
  endereço ativo (RN05); o mesmo vale ao trocar de página.
- **Cenário 7 — Nenhuma loja do tipo na cidade**: Dado uma cidade sem
  loja do tipo escolhido, Quando o filtro é aplicado, Então aparece o
  estado vazio ("Nenhuma loja encontrada") com a opção de limpar o
  filtro — não um erro.
- **Cenário 8 — Toque e leitura**: Dado a vitrine no celular (390 px),
  Então chips e botões do card têm alvo de toque de ao menos 44 px, o
  card tem um único `h2` com o nome da loja, e os selos de tipo são
  legíveis por leitor de tela (texto, não só ícone).

## 4. Estado atual (achados)

- A visão "Lojas" de `/inicio` é uma lista vertical de uma coluna
  (`supplier-list.component.html:65-68`) de `app-supplier-card`, que hoje
  é um card **horizontal**: avatar de 48 px, nome, `app-store-rating`,
  horário/tempo médio e botão "Ver produtos". A visão "Produtos" já usa
  grade (`grid-cols-2 lg:grid-cols-4`) de `app-product-card` — é o padrão
  visual a estender para as lojas.
- `app-category-chip-bar` já existe, já consome `GET /api/product-types`
  e já usa o `icon` gravado pelo admin, mas só é renderizado na visão
  "Produtos" (`supplier-list.component.html:74`). Ele emite o **nome** do
  tipo, porque o filtro de produtos usa `category=<nome>`
  (`ProductItemRepository.findAvailableProducts`, comparação por nome do
  `ProductType`).
- `StorefrontDTO` (14 campos) não tem nada sobre tipo de produto, e
  `SupplierRepository.findOpenForCatalog` só filtra cidade e nome.
- A relação existe em cadeia: `ProductItem → CategoryProduct` (de cada
  fornecedor) `→ ProductType` (global). Não há campo de tipo no
  fornecedor; o dado é derivado.
- `SupplierService.addRatings` já resolve o problema de N+1 para a nota,
  agregando por `supplierIds` depois de paginar — o mesmo molde serve
  para os tipos.
- Os cards atuais usam paleta Tailwind crua (`bg-white`, `text-gray-800`,
  `border-gray-200`) em vez dos tokens do tema `h2gas`. Não é HEX
  literal, mas a HU corrige isso nos arquivos que tocar.
- `supplier-card.component.spec.ts` já proíbe `text-gray-400` por
  contraste; manter essa garantia no card novo.

**Decisões do autor em 2026-10-26, ao ver a tela rodando** (revogam parte do
F7 §5.21):
- os **banners promocionais** da visão Lojas saem. O F7 pedia "até 2-3 banners
  com conteúdo hardcoded no frontend" (`f7-aplicacao-ui-ux.md:71-72`); eles
  viviam só em `supplier-list.component.ts:43-46` e no bloco 42-52 do template,
  sem nenhum spec dependendo deles;
- a faixa de tipos vale para **as duas visões**, não só Produtos;
- os controles de filtro ficam **à direita**;
- o card de loja segue o formato compacto do iFood (logo quadrada à esquerda),
  e não o card vertical espelhando o `product-card`, como este contrato dizia
  na primeira versão.

**Ordenação dos selos**: feita no serviço, com `Collator` pt-BR, e não no
`ORDER BY` da consulta. O H2 dos testes ordena por binário e põe "Gás" antes
de "Água"; o MySQL, com collation acento-insensível, faz o contrário — deixar
no banco tornaria o resultado dependente do ambiente.

## 5. Contrato Técnico

### 5.1 Backend — tipos de produto na vitrine

- **Novo DTO** `ProductTypeChipDTO` (record, `model/dto`): `id` (Long),
  `name` (String), `icon` (String). Documentado com `@Schema`.
- `StorefrontDTO` ganha `productTypes: List<ProductTypeChipDTO>` — os
  tipos que a loja de fato vende, sem repetição, ordenados por nome.
  Loja sem categoria devolve lista vazia (nunca `null`).
- **Projeção** `SupplierProductTypeRow` (`supplierId`, `typeId`,
  `typeName`, `typeIcon`) e query em `CategoryProductRepository`:
  ```java
  @Query("SELECT DISTINCT c.supplier.id AS supplierId, t.id AS typeId,"
          + " t.name AS typeName, t.icon AS typeIcon"
          + " FROM CategoryProduct c JOIN c.productType t"
          + " WHERE c.supplier.id IN :supplierIds ORDER BY t.name")
  List<SupplierProductTypeRow> findTypesBySupplierIds(
          @Param("supplierIds") Collection<String> supplierIds);
  ```
- `SupplierService`: método `addProductTypes(List<StorefrontDTO>)`, no
  mesmo molde de `addRatings` — **uma** consulta por página, agrupando em
  `Map<String, List<ProductTypeChipDTO>>`. Chamado junto de `addRatings`
  no catálogo paginado e em `getStorefront(id)`.
- `GET /api/suppliers/actives/open` ganha o parâmetro opcional
  `productTypeId` (Long). `findOpenForCatalog` passa a receber:
  ```java
  AND (:productTypeId IS NULL OR EXISTS (SELECT 1 FROM CategoryProduct c
       WHERE c.supplier = s AND c.productType.id = :productTypeId))
  ```
  Tipo inexistente devolve **página vazia** (200), não 404 — o cliente
  não controla o id, e 404 quebraria a navegação por chip.
- Nenhuma migração: o dado é derivado do que já existe.
- OpenAPI (obrigatório no mesmo passo): `@Parameter` de `productTypeId`
  em `SupplierController`, `@Schema` do campo novo e do DTO novo. A rota
  segue pública, sem cadeado, como a HU13 definiu.

### 5.2 Frontend — card de loja e chips

- **`supplier-card` passa a ser horizontal e compacto**, no formato da
  referência que o autor trouxe (print do iFood, 2026-10-26):
  - **logo quadrada** `w-20 h-20 rounded-xl object-cover` à esquerda (antes
    era circular de 48 px), com o fallback `storefront` que já existe
    (`logoBroken`) no mesmo quadrado;
  - à direita: `h2` com o nome (truncado); linha com `app-store-rating` e os
    **selos de tipo** (ícone do tipo + nome); linha com horário e tempo médio;
  - o `bannerUrl` **não** entra no card — segue só no cabeçalho da loja
    (F7 §5.26);
  - **o card inteiro é a área de clique**: o `data-testid="view-products"` sai
    do botão e vai para o container, com `role="button"`, `tabindex="0"` e
    `keydown.enter`/`keydown.space`. Não pode ser um `<button>` porque o
    `app-store-rating` já é um, e botão aninhado é HTML inválido; o
    `stopPropagation` do store-rating segue garantindo que as estrelas abram
    as avaliações, não a loja.
- Lista vira grade em `supplier-list.component.html:65-68`:
  `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-x-6 gap-y-4`.
- **`category-chip-bar` passa a emitir o `ProductType` inteiro** (ou
  `null` no chip "Todas"), em vez do nome. A visão "Produtos" continua
  mandando `category=<name>` para `/product-items/by-supplier-open` (HU02
  intacta); a visão "Lojas" manda `productTypeId=<id>`. O chip bar passa
  a ser renderizado nas **duas** visões.
- `models/storefront.ts` ganha `productTypes: ProductTypeChip[]`;
  `models/product-type.ts` ganha a interface `ProductTypeChip`.
- `supplier.service.ts` — `getAllSupplierOpenAndActive(page, size,
  cityId?, q?, productTypeId?)`, com `HttpParams` (sem concatenar query
  string à mão).
- `supplier-list.component.ts` guarda `selectedTypeId` e o repassa; ao
  trocar de tipo volta para a página 0.
- Cores pelos tokens do tema `h2gas` (`bg-base-100`, `text-base-content`,
  `border-base-300`, `bg-primary/10`) nos arquivos tocados — sem HEX
  literal novo (F2).

### 5.3 Revisão: vitrine no celular (2026-10-27)

Pedido do autor ao ver a vitrine em 390 px: layout apertado, botões difíceis
de tocar, muita rolagem até as lojas e visual pouco cuidado. Só classes de
layout — nenhum comportamento muda:

- título de uma linha, à esquerda, e sem subtítulo no celular (o desktop
  segue como estava);
- seletor Lojas/Produtos em largura total, com os dois lados do mesmo tamanho;
- linha "Lojas próximas · N" e o botão de ordenar na **mesma** linha; o botão
  mostra só a opção ativa (o rótulo "Ordenar:" volta de `sm` para cima, e o
  `aria-label` continua dizendo "Ordenar por: …");
- faixa de tipos sangrando até a borda da tela e círculos de 48 px no celular;
- card da loja com borda, nota e tipos em linhas próprias (antes quebravam de
  forma irregular), tipos como pílulas, e a área de toque da nota (que abre as
  avaliações) de 44 px sem aumentar o card.

Segunda rodada, ainda em 2026-10-27:

- **um produto por linha no celular**: a grade de produtos (página da loja e
  visão "Produtos" da vitrine) vira `grid-cols-1 sm:grid-cols-2 lg:grid-cols-4`
  — antes, uma categoria com um produto só deixava o card espremido na
  metade esquerda da tela. De 640 px para cima nada muda;
- **sai a aba "Categorias"** da barra inferior. Cliente: Início · Pedidos ·
  Perfil; visitante: Início · Entrar. Isso revoga a aba que a HU13
  ("Início, Categorias e Entrar") e o F7 (barra inferior da Figura 11) haviam
  definido. Os produtos seguem a um toque pelo seletor Lojas/Produtos da
  própria vitrine, e `?view=products` na URL continua funcionando.

Medido com o Chrome em 390 px: a primeira loja passa de ~510 px para ~400 px
do topo, sem rolagem horizontal.

### 5.4 Revisão: a loja no card de produto (2026-10-27)

Pedido do autor: na visão "Produtos" da vitrine, que mistura lojas, mostrar
quem vende cada produto. Decisão: **no card**, e não num título por
categoria — a mesma categoria (ex.: "Gás 13kg") reúne produtos de lojas
diferentes, então um título não teria uma loja só para mostrar.

- `app-product-card` ganha `showStore` (padrão `false`) e a saída
  `viewStore`. Com `showStore`, uma linha abaixo do nome com o ícone de loja
  e o nome dela, como botão (`aria-label` "Ver a loja …", 44 px no celular)
  que leva à página da loja pelo `navigate` que a vitrine já tinha.
- Só a vitrine liga `showStore`; a página da loja não, porque lá todos os
  produtos são da mesma loja, já no cabeçalho.
- Sem mudança no backend: `ProductItemDTO` já trazia `supplierId` e
  `supllierName`. A grafia errada do campo é de origem; corrigi-la muda o
  contrato da API e fica fora deste ajuste.
- Testes: `product-card.component.spec.ts` (`deveMostrarALojaQuandoPedido`,
  `naoDeveMostrarALojaPorPadrao`, `naoDeveMostrarALojaSemNome`,
  `deveEmitirALojaAoTocarNoNome`) e `supplier-list.component.spec.ts`
  (`deveMostrarALojaNosProdutos`, `deveAbrirALojaAoTocarNoNomeDoProduto`).

### 5.5 Revisão: card de produto (2026-10-27)

Pedido do autor: "Comprar agora" verde, UX e UI melhores, o produto bem
exibido sem o fundo branco em volta e ícones modernos. Decisões dele: card
vertical com foto grande; Adicionar em contorno e Comprar agora verde,
lado a lado; ícones novos só no card por enquanto.

- **Foto**: área 4:3 com a imagem por cima (`absolute`), para a proporção
  valer com foto em pé. As fotos reais não têm fundo branco (são fotos de
  ambiente), então a própria foto, desfocada e decorativa (`aria-hidden`,
  `alt=""`), preenche as laterais, e a foto nítida fica inteira por cima,
  com as bordas dissolvidas por máscara (`.product-photo-edge`) — sem faixa
  vazia nem emenda. Mesmo endereço nas duas: um download só.
- **Preço**: entrega como preço principal (`text-lg`, "com entrega");
  retirada como linha secundária ("Retirada na loja · R$ …").
- **Ações** lado a lado: Adicionar em contorno; Comprar agora preenchido em
  verde. O verde é o 700 da paleta (`--btn-color: var(--color-green-700)`),
  não o `success` do tema: com texto branco em 14 px o `success` (#16a34a)
  dá ~3,3:1, abaixo dos 4,5:1; o 700 dá ~5:1. **Revoga o F7 §5.21**, que
  fazia do Adicionar a única ação cheia.
- **Selo "Entrega grátis"** sobre a foto, texto escuro em fundo claro e o
  ícone em verde (o verde em texto pequeno não passaria no contraste).
- **Ícones** Material Symbols Rounded (peso 400, sem preenchimento), pelo
  Google Fonts com `icon_names` — só os 4 ícones do card. O resto do app
  segue com Material Icons.
- **Moldura**: `rounded-2xl`, borda clara, sombra só no hover de quem tem
  ponteiro.
- **Loja com avatar redondo** (pedido seguinte do autor): a linha da loja mostra a
  logo num círculo de 24 px; sem logo, ou se a imagem quebrar, a inicial do nome.
  Para isso o `ProductItemDTO` ganhou `supplierLogoUrl` (`@Schema`, opcional, nulo
  sem logo), preenchido pelo `ProductItemMapper` a partir de `Supplier.logoUrl` —
  campo novo e opcional, nenhum consumidor quebra.
- **Retirada** com o ícone de sacola (`shopping_bag`), no lugar do `storefront` que
  repetia o da loja; o subconjunto de ícones carregado passa a
  `add_shopping_cart, inventory_2, local_shipping, shopping_bag`.
- Testes em `product-card.component.spec.ts`: `deveDestacarComprarAgoraEmVerde`,
  `deveDeixarAsDuasAcoesLadoALado`, `deveExibirAFotoSemCaixaBranca`,
  `deveMostrarOIconeQuandoNaoHaFoto`, `deveUsarOsIconesArredondados`,
  `deveDarDestaqueAoPrecoDeEntrega`, `deveMostrarALogoRedondaDaLoja`,
  `deveMostrarAInicialDaLojaSemLogo`, `deveVoltarParaAInicialSeALogoQuebrar`,
  `deveUsarASacolaNaRetirada`; no backend, `ProductItemMapperTest`
  (`deveLevarALogoDaLojaQueVende`, `deveDeixarALogoNulaSemLogo`).

### 5.6 Revisão: produtos agrupados por tipo e categoria (2026-10-27)

Na página da loja e na visão "Produtos" da vitrine, a grade única vira
seções: um título por **tipo** (Gás, Água), na ordem dos chips, e dentro
dele um subtítulo por **categoria** (Gás 13kg, Gás 45kg), em ordem
alfabética pt-BR. Com um tipo já escolhido no chip, o título do tipo some
(só sobram as categorias). Produto sem tipo ou sem categoria vai para
"Outros", sempre por último.

- Só frontend: função pura `groupProductsByTypeAndCategory` em
  `utils/product-sections.ts`, usada pelos dois componentes. Agrupa **a
  página atual** que o backend devolveu — não muda paginação, filtro nem
  ordenação da API (RN05 segue no backend).
- Cada categoria mantém a grade `grid-cols-1 sm:grid-cols-2 lg:grid-cols-4`
  da 5.3.
- Testes: `product-sections.spec.ts` (ordem dos tipos, ordem das
  categorias, sem seção vazia com o tipo filtrado, "Outros");
  `deveAgruparProdutosPorTipoECategoria` em `product-list.component.spec.ts`
  e `supplier-list.component.spec.ts`.

## 6. Plano de Testes (TDD)

**Backend**
1. `SupplierServiceTest`: `deveTrazerOsTiposDeProdutoDaLoja`,
   `naoDeveRepetirTipoQuandoALojaTemVariasCategorias`,
   `deveDevolverListaVaziaDeTiposParaLojaSemCategoria`,
   `deveBuscarOsTiposEmUmaConsultaSoParaAPagina` (anti N+1).
2. `SupplierServiceTest`: `deveFiltrarLojasPeloTipoDeProduto`,
   `deveDevolverPaginaVaziaParaTipoInexistente`,
   `deveCombinarTipoCidadeEBusca` (RN05 junto do filtro).
3. `SupplierControllerTest`: `deveRepassarOTipoDeProdutoAoServico`,
   `deveDevolverOsTiposNaVitrine` (JSON com `productTypes[].icon`).

**Frontend**
4. `supplier-card.component.spec.ts`: `deveMostrarOsSelosDeTipo`,
   `naoDeveMostrarSeloQuandoALojaNaoTemTipo`,
   `deveUsarOIconeDoTipo`, `deveManterOFallbackDeLogo`,
   `deveTerAlvoDeToqueDe44px`.
5. `supplier-list.component.spec.ts`: `deveExibirAsLojasEmGrade`,
   `deveMostrarOsChipsNaVisaoDeLojas`,
   `deveEnviarOProductTypeIdAoFiltrarLojas`,
   `deveVoltarParaAPrimeiraPaginaAoTrocarDeTipo`,
   `deveMostrarEstadoVazioQuandoNaoHaLojaDoTipo`.
6. `category-chip-bar.component.spec.ts`: `deveEmitirOTipoInteiro`,
   `deveEmitirNuloEmTodas`.
7. `supplier.service.spec.ts`: `deveEnviarOProductTypeIdNaQuery`.

**E2E (registrado no DoD)**: em 390 e 1280 px — abrir `/inicio`, conferir
a grade e os selos; filtrar por Gás e por Água; combinar com busca;
"Todas" limpando; cidade sem loja do tipo mostrando o estado vazio.

## 7. Fora de Escopo

- Distância em km no card e ordenação por distância — não existe esse
  dado no sistema hoje (HU17).
- Taxa de entrega, selo "entrega grátis" e modalidade entrega/retirada no
  card (HU17).
- Barra de filtros e ordenação da lista de lojas (HU16).
- Filtro por sub-categoria (`CategoryProduct`, ex. "P13" × "P45") — segue
  fora desde a HU02.
- Tela de detalhe de produto; favoritos; banners promocionais dinâmicos;
  preço mínimo da loja no card (fora desde o F7 §6).
- Redesenho do card de produto e da página da loja
  (`/fornecedores/:id/produtos`).
- Dark mode.

## 8. Definição de Pronto

- [ ] `StorefrontDTO` devolve `productTypes` sem repetição, em uma
      consulta por página, e `GET /api/suppliers/actives/open` aceita
      `productTypeId`.
- [ ] Rota documentada no Swagger com o parâmetro e o schema novos.
- [ ] Lojas em grade responsiva, card vertical com selos de tipo e
      fallback de logo preservado.
- [ ] Chips de tipo nas duas visões, sem quebrar o filtro de produtos da
      HU02.
- [ ] Banners promocionais removidos da visão Lojas (revoga o F7 §5.21).
- [ ] Controles de filtro alinhados à direita nas duas visões.
- [ ] Card de loja no formato compacto (logo quadrada), com o card inteiro
      clicável e navegável por teclado.
- [ ] Testes da seção 6 verdes; `mvn -f backend/pom.xml test` e
      `ng test` sem falha nova; `ng build` ok.
- [ ] E2E em 390 e 1280 px registrado aqui com data.
- [ ] Tokens F2 nos arquivos tocados, sem HEX literal novo.
- [ ] Nada do "Fora de Escopo".
