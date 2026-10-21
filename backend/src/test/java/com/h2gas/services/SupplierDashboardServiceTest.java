package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.model.dto.DeliveryModeShareDTO;
import com.h2gas.model.dto.StoreCategorySalesDTO;
import com.h2gas.model.dto.DashboardFilter;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.CategorySalesDTO;
import com.h2gas.model.dto.PaymentShareDTO;
import com.h2gas.model.dto.SupplierDashboardDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SupplierDashboardServiceTest {

    @Autowired private SupplierDashboardService dashboardService;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private CategoryProductRepository categoryProductRepository;
    @Autowired private ProductItemRepository productItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager em;

    private Supplier fornecedor;
    private Supplier outroFornecedor;
    private User cliente;
    private ProductItem botijao;
    private ProductItem galao;
    private final YearMonth mes = YearMonth.now();

    @BeforeEach
    void setUp() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        fornecedor = novoFornecedor("gas.centro@teste.com", "11111111111", city);
        outroFornecedor = novoFornecedor("outro@teste.com", "22222222222", city);
        cliente = userRepository.save(new User(null, "Cliente", "cliente@teste.com", "41999999999", "33333333333", "senha", UserTypeEnum.CUSTOMER));
        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás", null));
        ProductType agua = productTypeRepository.save(new ProductType(null, "Água", null));
        botijao = novoProduto(fornecedor, gas, "Gás 13kg", "Ultragaz");
        galao = novoProduto(fornecedor, agua, "Água 20L", "Minalba");
    }

    private Supplier novoFornecedor(String email, String cpf, City city) {
        Supplier s = new Supplier();
        s.setName("Fornecedor " + email);
        s.setEmail(email);
        s.setPhoneNumber("41900000000");
        s.setCpf(cpf);
        s.setPassword("senha");
        s.setUserType(UserTypeEnum.SUPPLIER);
        s.setCnpj("12345678000100");
        s.setOpeningTime(LocalTime.of(8, 0));
        s.setClosingTime(LocalTime.of(18, 0));
        s.setAverageDeliveryTime(Duration.ofMinutes(30));
        s.setActive(true);
        s.setOpen(true);
        s.setCity(city);
        return supplierRepository.save(s);
    }

    private ProductItem novoProduto(Supplier supplier, ProductType tipo, String categoria, String marca) {
        CategoryProduct c = new CategoryProduct();
        c.setName(categoria);
        c.setProductType(tipo);
        c.setSupplier(supplier);
        c = categoryProductRepository.save(c);
        ProductItem p = new ProductItem();
        p.setBrand(marca);
        p.setPriceDelivery(new BigDecimal("100.00"));
        p.setPricePickup(new BigDecimal("100.00"));
        p.setStockQuantity(50);
        p.setSupplier(supplier);
        p.setCategoryProduct(c);
        return productItemRepository.save(p);
    }

    /** Cria o pedido e ajusta createdAt por JPQL, já que @CreationTimestamp ignora o valor informado. */
    private Order pedido(Supplier supplier, OrderStatusEnum status, String pagamento, String total, LocalDateTime criadoEm,
                         ProductItem produto, int quantidade) {
        Order o = new Order();
        o.setCustomer(cliente);
        o.setSupplier(supplier);
        o.setStatus(status);
        o.setPaymentMethod(pagamento);
        o.setTotalPrice(new BigDecimal(total));
        o = orderRepository.save(o);
        OrderItem item = new OrderItem();
        item.setOrder(o);
        item.setProduct(produto);
        item.setQuantity(quantidade);
        item.setUnitPrice(new BigDecimal(total).divide(BigDecimal.valueOf(quantidade)));
        item.setTotalPrice(new BigDecimal(total));
        List<OrderItem> itens = new ArrayList<>();
        itens.add(item);
        o.setItems(itens);
        orderRepository.saveAndFlush(o);
        em.createQuery("update Order o set o.createdAt = :d where o.id = :id")
                .setParameter("d", criadoEm).setParameter("id", o.getId()).executeUpdate();
        em.clear();
        return o;
    }

    private LocalDateTime dia(YearMonth ym, int day) {
        return ym.atDay(day).atTime(10, 0);
    }

    private SupplierDashboardDTO dashboard() {
        return dashboardService.buildDashboard(fornecedor.getId(), mes);
    }

    @Test
    @DisplayName("HU09: a receita ignora pedidos cancelados e de outro fornecedor")
    void deveSomarReceitaExcluindoPedidosCancelados() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.PENDING, "PIX", "50.00", dia(mes, 2), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.CANCELED, "PIX", "999.00", dia(mes, 2), botijao, 1);

        SupplierDashboardDTO d = dashboard();

        assertEquals(0, new BigDecimal("150.00").compareTo(d.getTotalRevenue()));
        assertEquals(2, d.getTotalOrders());
    }

    @Test
    @DisplayName("HU09: não mistura pedidos de outro fornecedor")
    void naoDeveMisturarPedidosDeOutroFornecedor() {
        pedido(fornecedor, OrderStatusEnum.PENDING, "PIX", "100.00", dia(mes, 1), botijao, 1);
        ProductItem produtoDeOutro = novoProduto(outroFornecedor, productTypeRepository.save(new ProductType(null, "Gás", null)), "Gás 13kg", "Liquigás");
        pedido(outroFornecedor, OrderStatusEnum.PENDING, "PIX", "700.00", dia(mes, 1), produtoDeOutro, 1);

        SupplierDashboardDTO d = dashboard();

        assertEquals(1, d.getTotalOrders());
        assertEquals(0, new BigDecimal("100.00").compareTo(d.getTotalRevenue()));
        assertEquals(1, d.getPendingOrders());
    }

    @Test
    @DisplayName("HU09: ticket médio = receita ÷ pedidos")
    void deveCalcularTicketMedio() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "50.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "30.00", dia(mes, 1), botijao, 1);

        assertEquals(0, new BigDecimal("60.00").compareTo(dashboard().getAverageTicket()));
    }

    @Test
    @DisplayName("HU09: variação percentual sobre o mês anterior")
    void deveCalcularVariacaoSobreMesAnterior() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes.minusMonths(1), 10), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "150.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "50.00", dia(mes, 2), botijao, 1);

        SupplierDashboardDTO d = dashboard();

        assertEquals(100.0, d.getRevenueChangePercent());   // 100 -> 200
        assertEquals(100.0, d.getOrdersChangePercent());    // 1 -> 2
    }

    @Test
    @DisplayName("HU09: sem receita no mês anterior a variação é nula")
    void deveRetornarVariacaoNulaSemMesAnterior() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "150.00", dia(mes, 1), botijao, 1);

        SupplierDashboardDTO d = dashboard();

        assertNull(d.getRevenueChangePercent());
        assertNull(d.getOrdersChangePercent());
    }

    @Test
    @DisplayName("HU09: a série diária traz todos os dias do mês, com zero nos sem venda")
    void devePreencherTodosOsDiasDoMesNaSerieDiaria() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "40.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "25.00", dia(mes, 3), botijao, 1);

        SupplierDashboardDTO d = dashboard();

        assertEquals(mes.lengthOfMonth(), d.getRevenueByDay().size());
        assertEquals(mes.atDay(1), d.getRevenueByDay().get(0).getDay());
        assertEquals(0, new BigDecimal("140.00").compareTo(d.getRevenueByDay().get(0).getRevenue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(d.getRevenueByDay().get(1).getRevenue()));
        assertEquals(0, new BigDecimal("25.00").compareTo(d.getRevenueByDay().get(2).getRevenue()));
    }

    @Test
    @DisplayName("HU09: a receber = pedidos contados ainda não entregues; concluídas = entregues")
    void deveSomarReceitaAReceberSemEntregues() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.OUT_FOR_DELIVERY, "PIX", "60.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.PENDING, "PIX", "40.00", dia(mes, 1), botijao, 1);

        SupplierDashboardDTO d = dashboard();

        assertEquals(0, new BigDecimal("100.00").compareTo(d.getReceivable()));
        assertEquals(1, d.getDeliveredOrders());
    }

    @Test
    @DisplayName("HU09: pendentes e em rota são o estado atual, sem filtro de mês")
    void deveContarPendentesEEmRotaSemFiltroDeMes() {
        pedido(fornecedor, OrderStatusEnum.PENDING, "PIX", "40.00", dia(mes.minusMonths(2), 5), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.OUT_FOR_DELIVERY, "PIX", "60.00", dia(mes.minusMonths(1), 5), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.PENDING, "PIX", "10.00", dia(mes, 1), botijao, 1);

        SupplierDashboardDTO d = dashboard();

        assertEquals(2, d.getPendingOrders());
        assertEquals(1, d.getOutForDeliveryOrders());
    }

    @Test
    @DisplayName("HU09: tempo médio de entrega em minutos")
    void deveCalcularTempoMedioDeEntregaEmMinutos() {
        Order a = pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);
        Order b = pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);
        marcarEntrega(a, dia(mes, 1), dia(mes, 1).plusMinutes(20));
        marcarEntrega(b, dia(mes, 1), dia(mes, 1).plusMinutes(40));

        assertEquals(30.0, dashboard().getAverageDeliveryMinutes());
    }

    @Test
    @DisplayName("HU09: sem entregas com os dois horários o tempo médio é nulo")
    void deveRetornarTempoMedioNuloSemEntregasComHorarios() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);

        assertNull(dashboard().getAverageDeliveryMinutes());
    }

    @Test
    @DisplayName("HU09: vendas por produto, top 5 por unidades")
    void deveListarTop5ProdutosPorUnidades() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "300.00", dia(mes, 1), botijao, 3);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "40.00", dia(mes, 1), galao, 2);
        pedido(fornecedor, OrderStatusEnum.CANCELED, "PIX", "900.00", dia(mes, 1), galao, 9);

        SupplierDashboardDTO d = dashboard();

        assertEquals(2, d.getSalesByProduct().size());
        assertEquals("Ultragaz Gás 13kg", d.getSalesByProduct().get(0).getProductName());
        assertEquals(3, d.getSalesByProduct().get(0).getQuantity());
        assertEquals("Minalba Água 20L", d.getSalesByProduct().get(1).getProductName());
        assertEquals(2, d.getSalesByProduct().get(1).getQuantity());
    }

    @Test
    @DisplayName("HU09: pagamentos agrupados com percentual sobre os pedidos do mês")
    void deveAgruparPagamentosComPercentual() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "50.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "50.00", dia(mes, 1), botijao, 1);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "DINHEIRO", "100.00", dia(mes, 1), botijao, 1);

        List<PaymentShareDTO> pagamentos = dashboard().getPaymentBreakdown();

        assertEquals("PIX", pagamentos.get(0).getMethod());
        assertEquals(3, pagamentos.get(0).getOrders());
        assertEquals(75.0, pagamentos.get(0).getPercent());
        assertEquals(0, new BigDecimal("200.00").compareTo(pagamentos.get(0).getRevenue()));
        assertEquals("DINHEIRO", pagamentos.get(1).getMethod());
        assertEquals(25.0, pagamentos.get(1).getPercent());
    }

    @Test
    @DisplayName("HU09: vendas por categoria (tipo de produto) com percentual da receita, sem cancelados")
    void deveAgruparVendasPorCategoriaComPercentualDaReceita() {
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "300.00", dia(mes, 1), botijao, 3);
        pedido(fornecedor, OrderStatusEnum.DELIVERED, "PIX", "100.00", dia(mes, 2), galao, 2);
        pedido(fornecedor, OrderStatusEnum.CANCELED, "PIX", "900.00", dia(mes, 3), galao, 9);

        List<CategorySalesDTO> categorias = dashboard().getSalesByCategory();

        assertEquals(2, categorias.size());
        assertEquals("Gás", categorias.get(0).getCategoryName());
        assertEquals(3, categorias.get(0).getQuantity());
        assertEquals(0, new BigDecimal("300.00").compareTo(categorias.get(0).getRevenue()));
        assertEquals(75.0, categorias.get(0).getPercent());
        assertEquals("Água", categorias.get(1).getCategoryName());
        assertEquals(25.0, categorias.get(1).getPercent());
    }

    @Test
    @DisplayName("HU09: mês futuro não tem indicadores")
    void deveRejeitarMesFuturo() {
        assertThrows(BusinessRuleException.class,
                () -> dashboardService.buildDashboard(fornecedor.getId(), mes.plusMonths(1)));
    }

    private void marcarEntrega(Order pedido, LocalDateTime despachadoEm, LocalDateTime entregueEm) {
        em.createQuery("update Order o set o.dispatchedAt = :a, o.deliveredAt = :b where o.id = :id")
                .setParameter("a", despachadoEm).setParameter("b", entregueEm).setParameter("id", pedido.getId()).executeUpdate();
        em.clear();
    }

    // ---- F7 fatia 6: filtros do dashboard ----
    private Order pedidoComItens(OrderStatusEnum status, String pagamento, DeliveryModeEnum modo, LocalDateTime criadoEm,
                                 Object... produtoEValor) {
        Order o = new Order();
        o.setCustomer(cliente);
        o.setSupplier(fornecedor);
        o.setStatus(status);
        o.setPaymentMethod(pagamento);
        o.setDeliveryMode(modo);
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> itens = new ArrayList<>();
        for (int k = 0; k < produtoEValor.length; k += 2) {
            OrderItem item = new OrderItem();
            item.setOrder(o);
            item.setProduct((ProductItem) produtoEValor[k]);
            item.setQuantity(1);
            BigDecimal valor = new BigDecimal((String) produtoEValor[k + 1]);
            item.setUnitPrice(valor);
            item.setTotalPrice(valor);
            itens.add(item);
            total = total.add(valor);
        }
        o.setTotalPrice(total);
        o.setItems(itens);
        o = orderRepository.saveAndFlush(o);
        em.createQuery("update Order o set o.createdAt = :d where o.id = :id")
                .setParameter("d", criadoEm).setParameter("id", o.getId()).executeUpdate();
        em.clear();
        return o;
    }

    private SupplierDashboardDTO dashboard(String payment, String mode, Long typeId, Long categoryId, Long productId) {
        return dashboardService.buildDashboard(fornecedor.getId(), mes, DashboardFilter.parse(payment, mode,
                typeId == null ? null : typeId.toString(), categoryId == null ? null : categoryId.toString(),
                productId == null ? null : productId.toString()));
    }

    @Test
    @DisplayName("F7 fatia 6: filtro de pagamento e de modo valem para o pedido inteiro")
    void deveFiltrarPorPagamentoEModo() {
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, dia(mes, 1), botijao, "100.00");
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.PICKUP, dia(mes, 2), galao, "40.00");
        pedidoComItens(OrderStatusEnum.DELIVERED, "DINHEIRO", DeliveryModeEnum.DELIVERY, dia(mes, 3), botijao, "90.00");

        SupplierDashboardDTO pix = dashboard("PIX", null, null, null, null);
        assertEquals(2, pix.getTotalOrders());
        assertEquals(0, new BigDecimal("140.00").compareTo(pix.getTotalRevenue()));
        assertEquals(List.of("PIX"), pix.getPaymentBreakdown().stream().map(PaymentShareDTO::getMethod).toList());

        SupplierDashboardDTO pixRetirada = dashboard("PIX", "PICKUP", null, null, null);
        assertEquals(1, pixRetirada.getTotalOrders());
        assertEquals(0, new BigDecimal("40.00").compareTo(pixRetirada.getTotalRevenue()));
    }

    @Test
    @DisplayName("F7 fatia 6: filtro de tipo, categoria ou produto soma só os itens que batem")
    void deveSomarSoOsItensQueBatemComTipoCategoriaOuProduto() {
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, dia(mes, 1), botijao, "300.00", galao, "100.00");
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, dia(mes, 2), galao, "50.00");
        Long gas = botijao.getCategoryProduct().getProductType().getId();

        SupplierDashboardDTO soGas = dashboard(null, null, gas, null, null);
        assertEquals(1, soGas.getTotalOrders());
        assertEquals(0, new BigDecimal("300.00").compareTo(soGas.getTotalRevenue()));
        assertEquals(0, new BigDecimal("300.00").compareTo(soGas.getAverageTicket()));
        assertEquals(0, new BigDecimal("300.00").compareTo(soGas.getPaymentBreakdown().get(0).getRevenue()));
        assertEquals(List.of("Gás"), soGas.getSalesByCategory().stream().map(CategorySalesDTO::getCategoryName).toList());

        SupplierDashboardDTO porCategoria = dashboard(null, null, null, galao.getCategoryProduct().getId(), null);
        assertEquals(2, porCategoria.getTotalOrders());
        assertEquals(0, new BigDecimal("150.00").compareTo(porCategoria.getTotalRevenue()));

        SupplierDashboardDTO porProduto = dashboard(null, null, null, null, botijao.getId());
        assertEquals(1, porProduto.getSalesByProduct().size());
        assertEquals(botijao.getId(), porProduto.getSalesByProduct().get(0).getProductId());
        BigDecimal dia1 = porProduto.getRevenueByDay().get(0).getRevenue();
        assertEquals(0, new BigDecimal("300.00").compareTo(dia1));
    }

    @Test
    @DisplayName("F7 fatia 6: a variação compara com o mês anterior usando o mesmo filtro")
    void deveAplicarOFiltroNaVariacaoDoMesAnterior() {
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, dia(mes, 1), botijao, "150.00");
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, dia(mes.minusMonths(1), 1), botijao, "100.00");
        pedidoComItens(OrderStatusEnum.DELIVERED, "DINHEIRO", DeliveryModeEnum.DELIVERY, dia(mes.minusMonths(1), 2), botijao, "900.00");

        assertEquals(50.0, dashboard("PIX", null, null, null, null).getRevenueChangePercent());
    }

    @Test
    @DisplayName("F7 fatia 6: quebras por categoria da loja e por entrega/retirada")
    void deveQuebrarPorCategoriaDaLojaEPorModo() {
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, dia(mes, 1), botijao, "300.00");
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.PICKUP, dia(mes, 1), galao, "100.00");

        SupplierDashboardDTO d = dashboard(null, null, null, null, null);

        List<StoreCategorySalesDTO> categorias = d.getSalesByStoreCategory();
        assertEquals("Gás 13kg", categorias.get(0).getCategoryName());
        assertEquals("Gás", categorias.get(0).getTypeName());
        assertEquals(botijao.getCategoryProduct().getId(), categorias.get(0).getCategoryId());
        assertEquals(75.0, categorias.get(0).getPercent());

        List<DeliveryModeShareDTO> modos = d.getDeliveryModeBreakdown();
        assertEquals("DELIVERY", modos.get(0).getMode());
        assertEquals(0, new BigDecimal("300.00").compareTo(modos.get(0).getRevenue()));
        assertEquals(50.0, modos.get(0).getPercent());
        assertEquals("PICKUP", modos.get(1).getMode());
    }

    @Test
    @DisplayName("F7 fatia 6: quebras trazem os ids para o clique filtrar")
    void deveTrazerIdsNasQuebrasParaOFiltro() {
        pedidoComItens(OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, dia(mes, 1), botijao, "300.00");

        SupplierDashboardDTO d = dashboard(null, null, null, null, null);

        assertEquals(botijao.getId(), d.getSalesByProduct().get(0).getProductId());
        assertEquals(0, new BigDecimal("300.00").compareTo(d.getSalesByProduct().get(0).getRevenue()));
        assertEquals(botijao.getCategoryProduct().getProductType().getId(), d.getSalesByCategory().get(0).getProductTypeId());
    }

    @Test
    @DisplayName("F7 fatia 6: filtro inválido do dashboard é 400")
    void deveRecusarFiltroInvalidoDoDashboard() {
        assertThrows(BusinessRuleException.class, () -> DashboardFilter.parse("BOLETO", null, null, null, null));
        assertThrows(BusinessRuleException.class, () -> DashboardFilter.parse(null, "DRONE", null, null, null));
        assertThrows(BusinessRuleException.class, () -> DashboardFilter.parse(null, null, "abc", null, null));
    }
}
