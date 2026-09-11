# HU09 — Dashboard Analítico e Bloqueio de Estoque

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU09 |
| Perfil | Fornecedor (SUPPLIER) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (`dispatchedAt`/`deliveredAt` para tempo médio de entrega) |
| Referência no TCC | Apêndice A.1 — HU09; RN02 |

## 2. História de Usuário

Como fornecedor, quero visualizar indicadores de vendas e ter o controle
de estoque automatizado, para tomar decisões rápidas e evitar vender
produtos indisponíveis.

## 3. Critérios de Aceitação (BDD)

*(correção: a versão anterior descrevia o Cenário 1 como "3 indicadores
numéricos" — receita, pedidos pendentes, tempo médio — reconstruído do
resumo do capítulo 5.3. O texto real do Apêndice A, pág. 52, pede
**gráficos**, não só números, e não menciona "pedidos pendentes"
explicitamente.)*

- **Cenário 1 — Visão gerencial**: Dado o acesso administrativo, Quando o
  dashboard é renderizado, Então exibe gráficos consolidados de volume
  financeiro e tempo médio de entrega.
- **Cenário 2 — Proteção de inventário (RN02)**: Dado o controle de
  saídas, Quando a quantidade de um produto atinge zero, Então o sistema
  bloqueia ou oculta automaticamente o item no aplicativo do cliente.

## 4. Estado atual (achados)

- **RN02 não está implementada em nenhuma query de listagem.**
  `ProductItemRepository` não tem nenhum filtro de `stockQuantity`.
  Produtos com estoque zero continuam aparecendo normalmente no catálogo
  do cliente. O que já funciona: `OrderItemService.createOrderItem`
  já impede a **compra** quando `quantityRequested > currentStock`
  (então não dá pra vender além do estoque) — mas isso não é o mesmo que
  "ocultar da vitrine": o item continua visível, só falha ao tentar
  comprar, o que é uma experiência ruim, não o "bloqueio automático sem
  intervenção manual" pedido pela HU09.
- **Restrição de design**: o mesmo endpoint backend
  (`GET /api/product-items/by-supplier/{supplierId}`) é usado tanto pelo
  catálogo do **cliente** (`product-list.component.ts`) quanto pela tela
  de **gestão de inventário do próprio fornecedor**
  (`product-item.component.ts`) — filtrar estoque zero incondicionalmente
  quebraria a visão do fornecedor, que precisa ver itens zerados
  justamente para repor estoque. Por isso o filtro precisa ser
  condicional por parâmetro, não incondicional na query.
- **Nenhuma infraestrutura de dashboard existe hoje** — nem endpoint, nem
  componente Angular. `distributor-management.component.ts` é uma tela
  de **administração de fornecedores pelo ADMIN** (CRUD), não o painel
  analítico do próprio fornecedor. É um build greenfield.
- `dispatchedAt`/`deliveredAt` (`Order`, adicionados no F3) ainda não são
  preenchidos por nenhum fluxo até este ponto da fila — isso só passa a
  acontecer na HU07 (entregador atualiza status). **Consequência
  documentada**: até a HU07 ser implementada, "tempo médio de entrega"
  vai retornar vazio/zero para todo fornecedor — comportamento esperado,
  não um bug desta história.

## 5. Contrato Técnico

### 5.1 Backend — RN02 (bloqueio de estoque)

**`ProductItemRepository.java`** — nova query com filtro condicional de
disponibilidade (reaproveita o padrão de parâmetro opcional já usado em
HU02):
```java
@Query("""
    SELECT p FROM ProductItem p
    WHERE p.supplier.id = :supplierId
      AND (:category IS NULL OR LOWER(p.categoryProduct.productType.name) = LOWER(:category))
      AND (:search IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')))
      AND (:onlyAvailable = FALSE OR p.stockQuantity > 0)
    """)
Page<ProductItem> findBySupplierFiltered(@Param("supplierId") String supplierId,
                                          @Param("category") String category,
                                          @Param("search") String search,
                                          @Param("onlyAvailable") boolean onlyAvailable,
                                          Pageable pageable);
```
(substitui `findProductsFiltered`/os métodos derivados equivalentes,
preservando os parâmetros já usados pela HU02.)

**`ProductItemController.java`** (`GET /by-supplier/{supplierId}`) —
adiciona `@RequestParam(defaultValue = "false") boolean onlyAvailable`.

**`ProductItemController.java`** (`GET /by-supplier-open`, da HU02) —
o `findAvailableProducts` da HU02 ganha `AND p.stockQuantity > 0`
incondicional (esse endpoint é exclusivamente de vitrine de cliente, sem
caso de uso administrativo — não precisa de flag).

### 5.2 Frontend — RN02

**`product-item.service.ts`**:
- `getProductsBySupplierId(...)` (usado por `product-list`, tela do
  **cliente**) passa a enviar `onlyAvailable=true` sempre.
- `getProductsBySupllierId(...)` (usado por `product-item.component.ts`,
  gestão do **fornecedor**) continua sem o parâmetro (default `false` —
  fornecedor vê tudo, inclusive zerado, para repor).

### 5.3 Backend — Dashboard (Cenário 1)

**Novo DTO `SupplierDashboardDTO`**:
```java
public class SupplierDashboardDTO {
    private BigDecimal totalRevenue;              // manchete do card, apoia o gráfico
    private List<RevenuePointDTO> revenueByDay;    // últimos 7 dias — alimenta o gráfico exigido pelo Cenário 1
    private Double averageDeliveryMinutes;         // null se não houver entregas com timestamps completos
    private long pendingOrdersCount;               // extra (Cap. 6.4.1), não exigido pelo Apêndice A
}

public class RevenuePointDTO {
    private LocalDate day;
    private BigDecimal revenue;
}
```

**`OrderRepository.java`** — novas queries:
```java
@Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.supplier.id = :supplierId AND o.status <> 'CANCELED'")
BigDecimal sumRevenueBySupplier(@Param("supplierId") String supplierId);

@Query("""
    SELECT FUNCTION('DATE', o.createdAt), COALESCE(SUM(o.totalPrice), 0)
    FROM Order o
    WHERE o.supplier.id = :supplierId AND o.status <> 'CANCELED' AND o.createdAt >= :since
    GROUP BY FUNCTION('DATE', o.createdAt)
    ORDER BY FUNCTION('DATE', o.createdAt)
    """)
List<Object[]> sumRevenueByDaySince(@Param("supplierId") String supplierId, @Param("since") LocalDateTime since);

long countBySupplierIdAndStatus(String supplierId, OrderStatusEnum status);

@Query("""
    SELECT AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, o.dispatchedAt, o.deliveredAt))
    FROM Order o
    WHERE o.supplier.id = :supplierId AND o.status = 'DELIVERED'
      AND o.dispatchedAt IS NOT NULL AND o.deliveredAt IS NOT NULL
    """)
Double averageDeliveryMinutesBySupplier(@Param("supplierId") String supplierId);
```

**`SupplierService.java`** — novo método:
```java
public SupplierDashboardDTO buildDashboard(String supplierId) {
    BigDecimal revenue = orderRepository.sumRevenueBySupplier(supplierId);
    LocalDateTime since = LocalDateTime.now().minusDays(7);
    List<RevenuePointDTO> revenueByDay = orderRepository.sumRevenueByDaySince(supplierId, since)
        .stream()
        .map(row -> new RevenuePointDTO((LocalDate) row[0], (BigDecimal) row[1]))
        .toList();
    Double avgMinutes = orderRepository.averageDeliveryMinutesBySupplier(supplierId);
    long pending = orderRepository.countBySupplierIdAndStatus(supplierId, OrderStatusEnum.PENDING);
    return new SupplierDashboardDTO(revenue, revenueByDay, avgMinutes, pending);
}
```
**Decisões registradas**:
- "Volume financeiro" (Cenário 1) é atendido por um gráfico de receita
  dos últimos 7 dias (`revenueByDay`) — período fixo, sem seletor de
  intervalo (o Apêndice A não pede customização de período).
- "Receita total" é a soma de `totalPrice` de todos os pedidos não
  cancelados (recebidos ou a receber), não só os `DELIVERED`.
- `pendingOrdersCount` é mantido como card extra (alinhado ao Cap. 6.4.1
  do TCC, que descreve um dashboard mais rico), mas não é parte do
  critério de aceitação vinculante — se cortar escopo, corta esse
  primeiro.

**`SupplierController.java`** — novo endpoint:
```java
@GetMapping("/{id}/dashboard")
@ResponseBody
@RolesAllowed({"ADMIN", "SUPPLIER"})
public ResponseEntity<SupplierDashboardDTO> getDashboard(@PathVariable String id) {
    return ResponseEntity.ok(supplierService.buildDashboard(id));
}
```

### 5.4 Frontend — Dashboard

**Nova dependência**: nenhuma biblioteca de gráficos está instalada hoje
(`package.json` não tem `chart.js`/`ngx-charts`/similar). Adicionar
`ng2-charts` + `chart.js` — wrapper Angular mais leve para o que o TCC
chama de "gráficos simples", evitando a complexidade de uma lib baseada
em D3.

**Novo componente `supplier-dashboard`**
(`components/pages/supplier-dashboard/`) — tela inicial pós-login do
fornecedor (Figura 13 do TCC):
- Gráfico de barras/linha com `revenueByDay` (via `ng2-charts`) —
  cumpre "gráficos consolidados de volume financeiro" do Cenário 1.
- Card numérico com `averageDeliveryMinutes` (exibir "—" quando `null`)
  — cumpre "tempo médio de entrega" do Cenário 1.
- Card numérico extra com `pendingOrdersCount` (Cap. 6.4.1, não
  vinculante).

Consome `GET /api/suppliers/{id}/dashboard` via novo método em
`supplier.service.ts`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `ProductItemRepositoryTest` —
   `deveOcultarProdutosZeradosQuandoOnlyAvailableTrue`,
   `deveExibirProdutosZeradosQuandoOnlyAvailableFalse` (regressão da
   visão do fornecedor).
2. `ProductItemControllerTest` — `deveEnviarOnlyAvailableParaOEndpointDoCliente`.
3. `OrderRepositoryTest`/`SupplierServiceTest` —
   `deveSomarReceitaExcluindoPedidosCancelados`,
   `deveAgruparReceitaPorDiaNosUltimosSeteDias`,
   `deveContarPedidosPendentesDoFornecedor`,
   `deveRetornarTempoMedioNuloQuandoNaoHaEntregasComTimestamps`,
   `deveCalcularTempoMedioDeEntregaEmMinutos` (com dados de teste com
   `dispatchedAt`/`deliveredAt` preenchidos manualmente, já que nenhum
   fluxo real ainda os popula).
4. `SupplierControllerTest` — `deveRetornar200ComDashboardDoFornecedor`.

**Frontend:**
5. `supplier.service.spec.ts` — `deveBuscarDashboardDoFornecedor`.
6. `supplier-dashboard.component.spec.ts` —
   `deveRenderizarGraficoComOsPontosDeReceitaDiaria`,
   `deveExibirTempoMedioDeEntrega`,
   `deveExibirTracoQuandoTempoMedioForNulo`.
7. `product-item.service.spec.ts` — atualizar/estender:
   `deveEnviarOnlyAvailableTrueParaClienteEFalseParaFornecedor`.

## 7. Fora de Escopo

- Seletor de período customizável no gráfico — período fixo de 7 dias.
- Popular `dispatchedAt`/`deliveredAt` de fato — isso é HU07/HU08; aqui
  só se consome esses campos, assumindo que podem estar vazios.
- Qualquer configuração de estoque mínimo/alerta preventivo antes de
  chegar a zero — a HU09 trata só o bloqueio no momento em que o estoque
  chega a zero, não alertas antecipados.
- Gráficos adicionais do Cap. 6.4.1 (ex.: "Pagamentos" por método,
  "Vendas por Produto") além do exigido pelo Cenário 1 — podem ser
  incremento futuro, não são vinculantes aqui.

## 8. Definição de Pronto

- [ ] Produtos com `stockQuantity = 0` não aparecem mais na vitrine do
      cliente (`onlyAvailable=true`), mas continuam visíveis na gestão do
      fornecedor.
- [ ] Endpoint `/api/suppliers/{id}/dashboard` implementado, retornando
      série diária de receita.
- [ ] Tela `supplier-dashboard` renderiza gráfico de volume financeiro e
      exibe tempo médio de entrega (Cenário 1 real).
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado antes de HU10 começar.
