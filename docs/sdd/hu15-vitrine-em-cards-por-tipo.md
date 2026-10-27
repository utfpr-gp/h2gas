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
