# HU16 — Filtros e busca da vitrine

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU16 |
| Perfil | Cliente (CUSTOMER) e Visitante |
| Prioridade MoSCoW | Should have — pedido do autor em 2026-10-26 ("devemos ter filtros também; há um bug no input de busca, com dois X") |
| Dependências | HU15 (cards e chips na visão lojas), HU02 (ordenação de produtos), F7 §5.21 (busca com debounce), F7 §5.25 (nota agregada), HU13 (vitrine pública) |
| Referência no TCC | Sem figura própria; extensão da HU02 (navegação por categorias e filtros) |

## 2. História de Usuário

Como cliente (ou visitante) na vitrine, quero ordenar as lojas e buscar
pelo nome sem a interface atrapalhar, para achar rápido onde comprar.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Um único botão de limpar (correção)**: Dado o campo de
  busca de `/inicio` com texto digitado, Quando o cliente olha o campo,
  Então vê **um** botão de limpar — não dois. Hoje aparecem dois: o X
  nativo do navegador e o botão da aplicação.
- **Cenário 2 — Limpar a busca**: Dado texto no campo, Quando o cliente
  toca no botão de limpar, Então o campo fica vazio, a lista recarrega
  sem o termo e o botão some.
- **Cenário 3 — Ordenar por nome (padrão)**: Dado a vitrine recém-aberta,
  Então as lojas vêm em ordem alfabética e a barra de filtros mostra
  "Nome (A-Z)" como ordenação ativa.
- **Cenário 4 — Melhor avaliação**: Dado a barra de filtros, Quando o
  cliente escolhe "Melhor avaliação", Então as lojas vêm da maior para a
  menor média, e as que ainda não têm avaliação ficam no fim.
- **Cenário 5 — Menor tempo de entrega**: Quando escolhe "Menor tempo de
  entrega", Então as lojas vêm do menor para o maior tempo médio
  cadastrado; loja sem tempo cadastrado fica no fim.
- **Cenário 6 — Abertas primeiro e lojas fechadas visíveis**: Quando
  escolhe "Abertas primeiro", Então a vitrine passa a mostrar também as
  lojas **fechadas**, depois das abertas, com o selo "Fechada" no card e
  sem a ação de compra. Nas outras ordenações a vitrine segue como hoje,
  só com lojas abertas.
- **Cenário 7 — Filtros combinam**: Dado um tipo de produto selecionado
  (HU15) e um termo buscado, Quando o cliente troca a ordenação, Então os
  três valem juntos, a lista volta para a primeira página e a cidade do
  endereço ativo continua restringindo tudo (RN05).
- **Cenário 8 — O filtro sobrevive ao recarregar**: Dado busca, tipo e
  ordenação escolhidos, Quando a página é recarregada ou o cliente volta
  de uma loja, Então os mesmos filtros continuam aplicados (estado na URL).
- **Cenário 9 — Limpar tudo**: Quando o cliente toca em "Limpar", Então
  busca, tipo e ordenação voltam ao padrão e a lista recarrega.
- **Cenário 10 — Ordenação inválida**: Dado uma chamada à API com um
  `sort` fora da lista aceita, Então a resposta é 400 "Ordenação
  inválida." — a vitrine nunca cai em erro genérico.
- **Cenário 11 — Toque e leitura**: Dado a barra de filtros no celular
  (390 px), Então ela rola na horizontal sem cortar conteúdo, cada chip
  tem alvo de 44 px e estado em `aria-pressed`, e o menu de ordenação é
  navegável por teclado.

## 4. Estado atual (achados)

- **O bug dos dois X está em `supplier-list.component.html:15`**: o input
  é `type="search"`, e o Chrome/WebKit desenha o pseudo-elemento
  `::-webkit-search-cancel-button` sempre que há texto. Logo abaixo
  (`:19-22`) existe um botão próprio `clear-search`, renderizado por
  `*ngIf="query"` — exatamente na mesma condição. Não há nenhuma regra
  CSS neutralizando o nativo (`grep` por `search-cancel` em
  `frontend/src/styles.css` não retorna nada).
- `clearSearch()` (`supplier-list.component.ts:151-156`) manipula o DOM
  direto (`document.getElementById('catalog-search')`) porque o input não
  tem binding de valor. Funciona, mas é frágil e impede controlar o campo
  pelo estado.
- Outras telas (`product-item`, `supplier-approval`, `order-history`,
  `city-register`, `admin-users`) também usam `type="search"`, mas sem
  botão próprio — não têm o bug, embora fiquem com o X nativo.
- A busca da página da loja (`product-list.component.html:67-78`) é
  `type="text"` + `ngModel` e dispara só no Enter: um segundo padrão de
  busca, incoerente com o de `/inicio`. Unificar as duas continua fora de
  escopo (F7 §5.24).
- **Não existe barra de filtros.** O F7 §5.21 decidiu "busca única sem
  botão Filtrar", e `supplier-list.component.spec.ts:145` garante que o
  `filter-button` não exista. **Esta HU revoga essa decisão**: a tela
  cresceu (tipo, ordenação, e depois modalidade na HU17) e a busca
  sozinha não dá conta. O spec é substituído por asserts da barra nova, e
  esta seção fica como registro da mudança.
- `GET /api/suppliers/actives/open` não aceita `sort`; a ordem é fixa
  (`ORDER BY s.name` em `findOpenForCatalog`) e a query filtra
  `open = true`, ou seja, **loja fechada não aparece hoje**.
- A nota não é persistida: `SupplierService.addRatings` agrega **depois**
  de paginar. Ordenar por nota exige trazer a média para dentro da query.
- A visão "Produtos" já tem um toggle "Menor preço" (HU02) que continua
  valendo; esta HU trata da lista de **lojas**.

## 5. Contrato Técnico

### 5.1 Backend — ordenação da vitrine

- `GET /api/suppliers/actives/open` ganha o parâmetro opcional `sort`,
  com lista fechada: `name` (padrão), `rating`, `deliveryTime`,
  `openFirst`. Valor fora da lista → **400** "Ordenação inválida."
  (`BusinessRuleException`, tratada no `GlobalExceptionHandler`).
- `SupplierRepository` ganha a consulta com a média para poder ordenar
  por nota — a nota deixa de ser só pós-processamento quando
  `sort=rating`:
  ```java
  @Query("SELECT s FROM Supplier s LEFT JOIN Review r ON r.supplier = s"
          + " WHERE s.active = true AND (:includeClosed = true OR s.open = true)"
          + " AND (:cityId IS NULL OR s.city.id = :cityId)"
          + " AND (:q IS NULL OR LOCATE(:q, LOWER(s.name)) > 0)"
          + " AND (:productTypeId IS NULL OR EXISTS (...))"
          + " GROUP BY s")
  ```
  com o `ORDER BY` montado por `sort`:
  - `name` → `s.name`;
  - `rating` → `AVG(r.rating) DESC`, nulos por último (`COALESCE` ou
    `CASE WHEN AVG(r.rating) IS NULL THEN 1 ELSE 0 END, AVG(r.rating) DESC`);
  - `deliveryTime` → `s.averageDeliveryTime` crescente, nulos por último;
  - `openFirst` → `s.open DESC, s.name`.
- `includeClosed` é **true apenas** quando `sort=openFirst` (Cenário 6);
  nas demais ordenações a vitrine segue só com lojas abertas, como hoje.
- `StorefrontDTO` já tem `open` — o frontend usa esse campo para o selo
  "Fechada"; nenhum campo novo é necessário.
- Nenhuma migração.
- OpenAPI: `@Parameter` de `sort` com os valores aceitos descritos, e
  `@ApiResponse` 400 com a mensagem do contrato.

### 5.2 Frontend — correção da busca

- `supplier-list.component.html:15`: `type="search"` → `type="text"`,
  mantendo o botão próprio `clear-search` (mesmo padrão já usado em
  `product-list`). É a correção mínima que elimina o X nativo.
- Defesa extra para as telas de back-office que seguem com
  `type="search"`, em `frontend/src/styles.css`:
  ```css
  input[type="search"]::-webkit-search-cancel-button { appearance: none; }
  ```
- O input ganha `[value]="query"` e `#catalogSearch` (`@ViewChild`);
  `clearSearch()` deixa de usar `document.getElementById`.

### 5.3 Frontend — barra de filtros

- Componente novo `app-catalog-filter-bar` (standalone,
  `components/catalog-filter-bar/`), tira horizontal rolável logo abaixo
  da busca, no idioma visual dos chips existentes:
  - chip "Ordenar" abrindo menu com as quatro opções (uma marcada);
  - chips de filtro ativo (tipo escolhido, termo buscado) com X próprio;
  - botão "Limpar" quando há qualquer filtro ativo.
  - `@Input() sort`, `@Input() activeType`, `@Input() query`;
    `@Output() sortChange`, `@Output() clearAll`.
- `supplier-list.component.ts` passa a refletir `q`, `tipo` e `ordem` em
  **query params** (`router.navigate([], { queryParams, replaceUrl: true })`)
  e a lê-los na inicialização (Cenário 8), junto do `?view=` que já existe.
- Card da loja fechada: selo "Fechada" (`badge` com token de aviso) e o
  botão "Ver produtos" segue disponível — o que a loja fechada não tem é
  ação de compra, já garantida no catálogo dela.
- Tokens F2, sem HEX literal novo.

## 6. Plano de Testes (TDD)

**Backend**
1. `SupplierServiceTest`: `deveOrdenarPorMelhorAvaliacao`,
   `deveColocarLojaSemAvaliacaoNoFim`,
   `deveOrdenarPorMenorTempoDeEntrega`,
   `deveColocarLojaSemTempoNoFim`,
   `deveListarAbertasPrimeiroIncluindoFechadas`,
   `naoDeveTrazerLojaFechadaNasDemaisOrdenacoes`,
   `deveManterCidadeBuscaETipoAoOrdenar` (RN05 junto da ordenação).
2. `SupplierControllerTest`: `deveRepassarAOrdenacaoAoServico`,
   `deveRecusarOrdenacaoInvalida` (400 com a mensagem do contrato),
   `deveUsarNomeComoOrdenacaoPadrao`.

**Frontend**
3. `supplier-list.component.spec.ts` (o teste que reproduz o bug vem
   primeiro): `naoDeveUsarInputDoTipoSearch`,
   `deveMostrarUmUnicoBotaoDeLimpar`,
   `deveLimparABuscaSemAcessarODocumento`.
4. `supplier-list.component.spec.ts`: `deveEnviarAOrdenacaoEscolhida`,
   `deveVoltarParaAPrimeiraPaginaAoTrocarAOrdenacao`,
   `deveRefletirOsFiltrosNaUrl`, `deveLerOsFiltrosDaUrlAoIniciar`,
   `deveLimparTodosOsFiltros`, `deveMarcarLojaFechadaComSelo`.
   O assert `expect(byId('filter-button')).toBeNull()` (linha 145) é
   removido, com comentário-âncora `// ---- HU16 §4 (revoga F7 §5.21) ----`.
5. `catalog-filter-bar.component.spec.ts`: `deveEmitirAOrdenacaoEscolhida`,
   `deveMarcarAOrdenacaoAtivaComAriaPressed`,
   `deveMostrarLimparSomenteComFiltroAtivo`, `deveEmitirLimparTudo`.
6. `supplier.service.spec.ts`: `deveEnviarOSortNaQuery`.

**E2E (registrado no DoD)**: em 390 e 1280 px — digitar na busca e
conferir um só X; as quatro ordenações; combinar tipo + busca + ordem;
recarregar a página mantendo os filtros; "Limpar"; loja fechada visível
só em "Abertas primeiro".

## 7. Fora de Escopo

- Filtro de entrega/retirada e de entrega grátis — dependem de dados que
  não existem hoje (HU17).
- Distância e ordenação por distância (HU17).
- Filtro por faixa de preço, por nota mínima ou por sub-categoria.
- Levar a busca da página da loja ao padrão de `/inicio` (F7 §5.24).
- Mover a busca para dentro da navbar global (fora desde o F7 §6).
- Guardar a preferência de filtro do usuário entre sessões.
- Ordenação na visão "Produtos" além do "Menor preço" da HU02.

## 8. Definição de Pronto

- [ ] Um único botão de limpar no campo de busca, com teste que
      reproduzia o bug agora verde.
- [ ] `clearSearch()` sem acesso direto ao DOM.
- [ ] `GET /api/suppliers/actives/open` aceita `sort` com as quatro
      opções, recusa valor inválido com 400 e está documentado no Swagger.
- [ ] Barra de filtros com ordenação, chips de filtro ativo e "Limpar";
      estado na URL sobrevivendo ao recarregar.
- [ ] Loja fechada aparece com selo apenas em "Abertas primeiro".
- [ ] Testes da seção 6 verdes; `mvn -f backend/pom.xml test` e
      `ng test` sem falha nova; `ng build` ok.
- [ ] E2E em 390 e 1280 px registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
