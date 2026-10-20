# HU09 — Dashboard Analítico e Bloqueio de Estoque

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU09 |
| Perfil | Fornecedor (SUPPLIER); cliente (CUSTOMER) na vitrine |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (`dispatchedAt`/`deliveredAt`), HU02 (catálogo filtrado), HU05/HU06 (estoque devolvido no cancelamento), HU07 (preenche `dispatchedAt`/`deliveredAt` e `OUT_FOR_DELIVERY`), F7 (padrão visual), RN02 |
| Referência no TCC | Apêndice A.1 — HU09 (p. 52); cap. 6.4 e 6.4.1; Figura 13 (p. 40); RN02 |

## 2. História de Usuário

Como fornecedor, quero visualizar indicadores de vendas e ter o controle
de estoque automatizado, para tomar decisões rápidas e evitar vender
produtos indisponíveis.

## 3. Critérios de Aceitação (BDD)

> Revisado em 2026-10-19 sobre a `develop` com HU01–HU08 mergeadas. A
> versão anterior (de antes da HU06) tinha só um gráfico de 7 dias, tempo
> médio e pendentes, e dizia que `dispatchedAt`/`deliveredAt` "ainda não
> são preenchidos" — a HU07 já os preenche. Decisões do autor nesta
> revisão: (a) **alinhar à Figura 13** do TCC: KPIs, vendas por dia,
> "Vendas por Produto" e "Pagamentos", mais o tempo médio de entrega;
> (b) o dashboard vira a **tela inicial do fornecedor** após o login (cap.
> 6.4), com o menu Dashboard | Pedidos | Produtos; (c) **seletor de mês**
> com variação percentual sobre o mês anterior (como "Abril 2026" e
> "+12,5% vs mês anterior" na figura).

- **Cenário 1 — Visão gerencial**: Dado o acesso administrativo, Quando o
  dashboard é renderizado, Então exibe gráficos consolidados de volume
  financeiro e tempo médio de entrega.
- **Cenário 2 — Proteção de inventário (RN02)**: Dado o controle de
  saídas, Quando a quantidade de um produto atinge zero, Então o sistema
  bloqueia ou oculta automaticamente o item no aplicativo do cliente.

Detalhamento operacional (não cria cenário novo):

- **1a — Indicadores do mês escolhido**: receita total, total de pedidos e
  ticket médio (com variação % sobre o mês anterior), receita a receber,
  entregas concluídas e tempo médio de entrega (min).
- **1b — Estado operacional agora**: pedidos pendentes e em rota (contagem
  atual, não depende do mês).
- **1c — Gráficos**: vendas por dia do mês (volume financeiro), "Vendas
  por Produto" (top 5 por unidades) e "Pagamentos" (por forma de pagamento,
  com %).
- **2a — Vitrine do cliente**: produto com estoque zero some do catálogo
  do cliente, **sempre decidido no backend**; a gestão do fornecedor
  continua vendo os itens zerados para repor.

## 4. Estado atual (achados)

- **RN02 não está em nenhuma query de listagem.**
  `ProductItemRepository.findBySupplierIdFiltered` e
  `findAvailableProducts` não olham `stockQuantity`. O que já protege:
  `OrderItemService.createOrderItem` recusa compra acima do estoque e o
  `reorder-preview` (HU05) já marca item zerado como indisponível — mas o
  item continua aparecendo na vitrine, falhando só ao comprar.
- **Restrição de design**: `GET /api/product-items/by-supplier/{id}` serve
  o catálogo do **cliente** (`product-list`) e a gestão de inventário do
  **fornecedor** (`product-item`), que precisa ver zerados. Por isso o
  filtro é por parâmetro. **Correção ao rascunho anterior**: deixar o
  cliente decidir (`onlyAvailable=true` enviado pela tela) viola "filtros
  no backend, nunca só no client" (RN05/CLAUDE.md) — um `CUSTOMER` que
  chame a rota com `onlyAvailable=false` veria itens zerados. O backend
  passa a **forçar** `onlyAvailable=true` quando o principal é `CUSTOMER`.
- `GET /api/product-items/by-supplier-open` (`findAvailableProducts`) é
  vitrine de cliente; ganha `stockQuantity > 0` incondicional.
- **Dashboard não existe** (nem endpoint, nem tela). Hoje o login manda
  todo perfil, exceto entregador, para `/inicio` — o fornecedor entra no
  catálogo do cliente. O painel de pedidos (HU06) é `/dashboard-fornecedor`
  (rota `received-orders`) e já mostra cards "A receber / Pendentes / Em
  rota / Em andamento" calculados no front; o dashboard novo calcula tudo
  no backend (RN01: cálculo financeiro no backend).
- **Dono do dashboard**: o rascunho anterior usava
  `GET /api/suppliers/{id}/dashboard` com o id na URL, deixando qualquer
  fornecedor ler o de outro. O fornecedor vem do **principal** (o id do
  `Supplier` é o id do `User`): rota `GET /api/suppliers/mine/dashboard`.
  ADMIN não tem fornecedor próprio e fica fora (403).
- **Sem biblioteca de gráficos** (`package.json`). O rascunho anterior
  adicionava `ng2-charts` + `chart.js`; a Figura 13 usa barras simples
  (as de "Vendas por Produto" são barras de progresso) e CLAUDE.md pede
  "sem dependência nova sem necessidade": os gráficos são **CSS/SVG
  próprios**, sem nova dependência.
- **Agregação em Java, não em JPQL**: `FUNCTION('DATE', …)` e
  `TIMESTAMPDIFF` não são portáveis entre MySQL e o H2 dos testes. Um mês
  de pedidos de um fornecedor é pequeno; busca-se o mês com os itens
  (`@EntityGraph`) e agrega-se em stream, o que também deixa a regra
  testável sem banco real.
- Valores gravados: `Order.paymentMethod` é texto (`PIX`, `DINHEIRO`,
  `CREDIT_CARD`); `OrderStatusEnum` já tem `PENDING`, `OUT_FOR_DELIVERY`,
  `DELIVERED`, `CANCELED`.

## 5. Contrato Técnico

### 5.1 Backend — RN02 (bloqueio de estoque)

- `ProductItemRepository.findBySupplierIdFiltered(supplierId, category,
  search, onlyAvailable, pageable)`: acrescenta `AND (:onlyAvailable =
  FALSE OR p.stockQuantity > 0)` (estoque nulo conta como indisponível).
- `ProductItemRepository.findAvailableProducts`: acrescenta
  `AND p.stockQuantity > 0`.
- `ProductItemController` `GET /by-supplier/{supplierId}`: novo
  `@RequestParam(defaultValue = "false") boolean onlyAvailable`; se o
  principal é `CUSTOMER`, o valor efetivo é **sempre `true`**. OpenAPI
  documenta o parâmetro e a regra.
- `ProductItemService.findProductsFiltered` recebe o flag.
- Sem mudança no frontend: o cliente não precisa enviar o parâmetro (o
  backend decide) e a gestão do fornecedor segue sem ele.

### 5.2 Backend — Dashboard (Cenário 1)

- `OrderRepository.findForDashboard(supplierId, start, end)`
  (`createdAt >= start AND < end`, com `@EntityGraph` nos itens e
  produtos) e `countBySupplierIdAndStatus`.
- DTOs (todos com `@Schema`):
  - `SupplierDashboardDTO`: `month` (`YYYY-MM`), `totalRevenue`,
    `revenueChangePercent` (nulo se o mês anterior não teve receita),
    `totalOrders`, `ordersChangePercent`, `averageTicket` (nulo sem
    pedidos), `receivable`, `deliveredOrders`, `pendingOrders`,
    `outForDeliveryOrders`, `averageDeliveryMinutes` (nulo sem entregas
    com os dois horários), `revenueByDay[]`, `salesByProduct[]`,
    `paymentBreakdown[]`;
  - `RevenuePointDTO(day, revenue)` — **todos** os dias do mês, com zero
    nos sem venda;
  - `ProductSalesDTO(productName, quantity, revenue)` — top 5 por
    unidades; nome = `marca + categoria`;
  - `PaymentShareDTO(method, orders, revenue, percent)` — percent sobre o
    total de pedidos do mês.
- `SupplierDashboardService.buildDashboard(supplierId, YearMonth month)`
  (`@Transactional(readOnly = true)`, injeção por construtor; serviço
  próprio para não inchar o `SupplierService`, que é todo por campo):
  - **pedido contado** = status ≠ `CANCELED`, criado no mês;
  - `totalRevenue` = soma de `totalPrice`; `averageTicket` = receita ÷
    pedidos (2 casas, `HALF_UP`); variação % = `(atual − anterior) ÷
    anterior × 100`, 1 casa, nulo se anterior = 0;
  - `receivable` = soma de `totalPrice` dos pedidos contados com status ≠
    `DELIVERED`; `deliveredOrders` = contados com `DELIVERED`;
  - `pendingOrders`/`outForDeliveryOrders` = contagem atual
    (`PENDING`/`OUT_FOR_DELIVERY`) **sem filtro de mês**;
  - `averageDeliveryMinutes` = média de `deliveredAt − dispatchedAt` em
    minutos, só `DELIVERED` do mês com os dois horários.
- `SupplierController` `GET /api/suppliers/mine/dashboard?month=YYYY-MM`
  (`@RolesAllowed("SUPPLIER")`): o fornecedor é o principal; `month`
  ausente = mês corrente; formato inválido ou **mês futuro → 400**
  (`BusinessRuleException`). OpenAPI 200/400/403.

### 5.3 Frontend

- `services/supplier.service.ts`: `getDashboard(month?)`; `models/supplier-
  dashboard.ts`.
- `components/pages/supplier-dashboard` (rota `/dashboard`, só
  `SUPPLIER`), padrão F7, desktop-first mas responsivo:
  - cabeçalho "Visão Geral" com **seletor de mês** (mês atual e os 11
    anteriores);
  - KPIs: Receita total, Total de pedidos, Ticket médio (com seta/% verde
    ou vermelha vs mês anterior), A receber, Pendentes, Em rota,
    Concluídas e Tempo médio ("—" quando nulo);
  - gráfico de barras de vendas por dia (SVG/CSS, com valor ao toque/
    hover), "Vendas por Produto" (barras de progresso com unidades) e
    "Pagamentos" (forma, %, valor);
  - estados de carregando, vazio ("Sem vendas neste mês") e erro (toast
    pelo helper do F7).
- Navegação: o login do `SUPPLIER` vai para `/dashboard`; o menu do
  fornecedor mostra **Dashboard | Pedidos | Produtos** (Pedidos →
  `/dashboard-fornecedor`).

## 6. Plano de Testes (TDD)

**Backend:**
1. `ProductItemRepositoryTest` —
   `deveOcultarProdutosZeradosQuandoOnlyAvailableTrue`,
   `deveExibirProdutosZeradosQuandoOnlyAvailableFalse`,
   `deveOcultarProdutosZeradosNaVitrineAberta`.
2. `ProductItemControllerTest` — `deveForcarOnlyAvailableParaCliente`,
   `devePermitirFornecedorVerProdutosZerados`.
3. `SupplierDashboardServiceTest` (dados de teste com `createdAt`,
   `dispatchedAt`, `deliveredAt` gravados à mão) —
   `deveSomarReceitaExcluindoPedidosCancelados`,
   `deveCalcularTicketMedio`, `deveCalcularVariacaoSobreMesAnterior`,
   `deveRetornarVariacaoNulaSemMesAnterior`,
   `devePreencherTodosOsDiasDoMesNaSerieDiaria`,
   `deveSomarReceitaAReceberSemEntregues`,
   `deveContarPendentesEEmRotaSemFiltroDeMes`,
   `deveCalcularTempoMedioDeEntregaEmMinutos`,
   `deveRetornarTempoMedioNuloSemEntregasComHorarios`,
   `deveListarTop5ProdutosPorUnidades`,
   `deveAgruparPagamentosComPercentual`,
   `naoDeveMisturarPedidosDeOutroFornecedor`, `deveRejeitarMesFuturo`.
4. `SupplierControllerTest` —
   `deveRetornar200ComDashboardDoFornecedorLogado`,
   `deveRetornar400ParaMesFuturoOuInvalido`.

**Frontend:**
5. `supplier.service.spec.ts` — `deveBuscarDashboardDoFornecedor`,
   `deveEnviarMesSelecionado`.
6. `supplier-dashboard.component.spec.ts` —
   `deveExibirKpisDoMes`, `deveRenderizarBarrasDeVendasPorDia`,
   `deveExibirVendasPorProdutoEPagamentos`,
   `deveExibirTracoQuandoTempoMedioForNulo`,
   `deveRecarregarAoTrocarOMes`, `deveExibirEstadoVazioSemVendas`.
7. `login.component.spec.ts` — `deveRedirecionarFornecedorParaDashboard`;
   `menu.component.spec.ts` — `deveExibirDashboardPedidosEProdutosParaFornecedor`.

Regra herdada: specs antigos falhando no frontend não aumentam (22).

## 7. Fora de Escopo

- Intervalo de datas livre (só mês fechado/corrente) e comparação anual.
- Alerta de estoque mínimo antes de chegar a zero: a HU09 trata só o
  bloqueio no momento em que zera.
- Atualização em tempo real do dashboard (SSE) e polling: carrega ao abrir
  e ao trocar o mês.
- Dashboard de ADMIN sobre qualquer fornecedor e exportação (CSV/PDF).
- Gráficos além dos da Figura 13 (ex.: por entregador, por bairro).
- Biblioteca de gráficos (`ng2-charts`/`chart.js`).

## 8. Definição de Pronto

- [ ] Produto com `stockQuantity = 0` não aparece na vitrine do cliente
      (decisão no backend, inclusive se o cliente pedir
      `onlyAvailable=false`) e continua na gestão do fornecedor.
- [ ] `GET /api/suppliers/mine/dashboard` devolve os indicadores do mês
      do fornecedor logado (nunca de outro), documentado no Swagger.
- [ ] Tela `/dashboard` mostra KPIs, vendas por dia, vendas por produto,
      pagamentos e tempo médio de entrega, com seletor de mês.
- [ ] Fornecedor entra pelo dashboard após o login; menu com Dashboard |
      Pedidos | Produtos.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 22 falhas
      antigas); sem HEX novo.
- [ ] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [ ] Testado de ponta a ponta (API e interface, com pedidos de meses e
      estados variados e produto zerado).
- [ ] Revisado e aprovado antes de HU10 começar.
