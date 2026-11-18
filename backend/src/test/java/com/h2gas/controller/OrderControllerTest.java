package com.h2gas.controller;

import com.h2gas.model.dto.OrderHistorySummaryDTO;
import com.h2gas.model.dto.OrderHistoryFilter;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.dto.OrderPreviewDTO;
import com.h2gas.model.dto.OrderPreviewItemDTO;
import com.h2gas.model.dto.OrderPreviewRequestDTO;
import com.h2gas.model.dto.ReorderItemDTO;
import com.h2gas.model.dto.ReorderPreviewDTO;
import com.h2gas.model.entity.User;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.mapper.OrderMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.OrderService;
import com.h2gas.services.OrderNotificationService;
import com.h2gas.services.DelivererService;
import com.h2gas.model.dto.DeliveryHistoryDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.model.entity.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMapper orderMapper;

    @MockitoBean
    private OrderNotificationService orderNotificationService;

    @MockitoBean
    private DelivererService delivererService;

    @MockitoBean
    private com.h2gas.services.ReviewService reviewService;

    private User autenticarComo(UserTypeEnum tipo, String id) {
        User usuario = new User(null, "Usuário " + tipo, tipo.name().toLowerCase() + "@teste.com", "41999999999",
                "12345678900", "senha", tipo);
        usuario.setId(id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + tipo.name()))));
        return usuario;
    }

    @BeforeEach
    void autenticarComoCliente() {
        var authToken = new UsernamePasswordAuthenticationToken(
                "cliente-logado", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devePreviewRetornar200ComSubtotalCorreto() throws Exception {
        OrderPreviewItemDTO item = new OrderPreviewItemDTO(
                5L, 2, new BigDecimal("79.90"), new BigDecimal("159.80"));
        when(orderService.previewOrder(any(OrderPreviewRequestDTO.class)))
                .thenReturn(new OrderPreviewDTO(List.of(item), new BigDecimal("159.80")));

        mockMvc.perform(post("/api/orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryMode": "PICKUP",
                                 "items": [{"productItemRef": 5, "quantity": 2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(159.80))
                .andExpect(jsonPath("$.items[0].unitPrice").value(79.90))
                .andExpect(jsonPath("$.items[0].totalPrice").value(159.80));
    }

    @Test
    void devePreviewRetornar400SemItens() throws Exception {
        mockMvc.perform(post("/api/orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryMode": "DELIVERY", "items": []}
                                """))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).previewOrder(any());
    }

    @Test
    void deveRetornar200ComPreviewValido() throws Exception {
        User cliente = new User(null, "Cliente", "cliente@teste.com", "41999999999",
                "12345678900", "senha", UserTypeEnum.CUSTOMER);
        cliente.setId("cliente-1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                cliente, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        ReorderItemDTO item = new ReorderItemDTO();
        item.setProductId(5L);
        item.setQuantity(2);
        item.setSupplierId("sup-1");
        when(orderService.buildReorderPreview(eq("pedido-1"), same(cliente)))
                .thenReturn(new ReorderPreviewDTO(List.of(item), List.of()));

        mockMvc.perform(get("/api/orders/pedido-1/reorder-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableItems[0].productId").value(5))
                .andExpect(jsonPath("$.availableItems[0].supplierId").value("sup-1"))
                .andExpect(jsonPath("$.unavailableItems").isEmpty());
    }

    @Test
    void deveRetornar400SeJustificativaAusente() throws Exception {
        mockMvc.perform(patch("/api/orders/pedido-1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"  \"}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).cancelOrder(any(), any(), any());
    }

    @Test
    void deveRetornar200AoCancelarComJustificativa() throws Exception {
        User fornecedor = new User(null, "Gás Centro", "gas@teste.com", "41999999999",
                "12345678900", "senha", UserTypeEnum.SUPPLIER);
        fornecedor.setId("fornecedor-1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                fornecedor, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
        Order cancelado = new Order();
        cancelado.setStatus(OrderStatusEnum.CANCELED);
        when(orderService.cancelOrder(eq("pedido-1"), eq("Sem gás no estoque"), same(fornecedor))).thenReturn(cancelado);
        when(orderNotificationService.notifyCancellation(cancelado)).thenReturn(false);

        mockMvc.perform(patch("/api/orders/pedido-1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Sem gás no estoque\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Cancelado"))
                .andExpect(jsonPath("$.customerNotified").value(false));
    }

    @Test
    void deveRetornar200AoDespacharComEntregador() throws Exception {
        User fornecedor = autenticarComo(UserTypeEnum.SUPPLIER, "fornecedor-1");
        OrderResponseDTO despachado = new OrderResponseDTO();
        despachado.setStatus("Saiu para entrega");
        despachado.setDelivererName("Carlos Silva");
        when(orderService.assignDeliverer(eq("pedido-1"), eq(7L), same(fornecedor))).thenReturn(despachado);

        mockMvc.perform(patch("/api/orders/pedido-1/assign-deliverer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delivererId\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Saiu para entrega"))
                .andExpect(jsonPath("$.delivererName").value("Carlos Silva"));
    }

    @Test
    void deveRetornar400AoDespacharSemEntregador() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "fornecedor-1");

        mockMvc.perform(patch("/api/orders/pedido-1/assign-deliverer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).assignDeliverer(any(), any(), any());
    }

    @Test
    void deveRetornarApenasRotasDoEntregadorAutenticado() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER, "usuario-entregador");
        Deliverer entregador = new Deliverer();
        entregador.setId(9L);
        when(delivererService.findByUserId("usuario-entregador")).thenReturn(entregador);
        OrderResponseDTO rota = new OrderResponseDTO();
        rota.setId("pedido-rota");
        when(orderService.findActiveByDeliverer(9L)).thenReturn(List.of(rota));

        mockMvc.perform(get("/api/orders/my-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pedido-rota"));

        verify(orderService).findActiveByDeliverer(9L);
        verify(orderService, never()).findActiveByDeliverer(eq(1L));
    }

    @Test
    void deveRetornarHistoricoDoEntregador() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER, "usuario-entregador");
        Deliverer entregador = new Deliverer();
        entregador.setId(9L);
        when(delivererService.findByUserId("usuario-entregador")).thenReturn(entregador);
        when(orderService.findDeliveryHistory(9L)).thenReturn(new DeliveryHistoryDTO(12, List.of()));

        mockMvc.perform(get("/api/orders/my-deliveries/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveredToday").value(12));
    }

    @Test
    void deveRetornar200AoIniciarEntrega() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER, "usuario-entregador");
        Deliverer entregador = new Deliverer();
        entregador.setId(9L);
        when(delivererService.findByUserId("usuario-entregador")).thenReturn(entregador);
        OrderResponseDTO emRota = new OrderResponseDTO();
        emRota.setStatus("Saiu para entrega");
        when(orderService.startDelivery("pedido-rota", 9L)).thenReturn(emRota);

        mockMvc.perform(patch("/api/orders/pedido-rota/start-delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Saiu para entrega"));
    }

    @Test
    void deveRetornar400AoIniciarComOutraEntregaEmRota() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER, "usuario-entregador");
        Deliverer entregador = new Deliverer();
        entregador.setId(9L);
        when(delivererService.findByUserId("usuario-entregador")).thenReturn(entregador);
        when(orderService.startDelivery("pedido-novo", 9L))
                .thenThrow(new BusinessRuleException("Finalize a entrega em rota antes de iniciar outra."));

        mockMvc.perform(patch("/api/orders/pedido-novo/start-delivery"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Finalize a entrega em rota antes de iniciar outra."));
    }

    @Test
    void deveRetornar400AoCancelarEntregaSemMotivo() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER, "usuario-entregador");

        mockMvc.perform(patch("/api/orders/pedido-rota/cancel-delivery")
                        .contentType("application/json")
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).cancelDelivery(any(), any(), any());
    }

    @Test
    void deveRetornar200AoCancelarEntregaComMotivo() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER, "usuario-entregador");
        Deliverer entregador = new Deliverer();
        entregador.setId(9L);
        when(delivererService.findByUserId("usuario-entregador")).thenReturn(entregador);
        OrderResponseDTO devolvido = new OrderResponseDTO();
        devolvido.setStatus("Em preparacao");
        when(orderService.cancelDelivery("pedido-rota", 9L, "Moto quebrou")).thenReturn(devolvido);

        mockMvc.perform(patch("/api/orders/pedido-rota/cancel-delivery")
                        .contentType("application/json")
                        .content("{\"reason\":\"Moto quebrou\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Em preparacao"));
    }

    @Test
    void deveRetornar200AoConfirmarEntrega() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER, "usuario-entregador");
        Deliverer entregador = new Deliverer();
        entregador.setId(9L);
        when(delivererService.findByUserId("usuario-entregador")).thenReturn(entregador);
        OrderResponseDTO entregue = new OrderResponseDTO();
        entregue.setStatus("Entregue");
        when(orderService.completeDelivery("pedido-rota", 9L)).thenReturn(entregue);

        mockMvc.perform(patch("/api/orders/pedido-rota/complete-delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Entregue"));
    }

    @Test
    void deveUsarOFornecedorDoTokenNoHistorico() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "fornecedor-1");
        when(orderService.findMyHistory(eq("fornecedor-1"), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 10), 0));
        when(orderService.summarizeMyHistory(eq("fornecedor-1"), any()))
                .thenReturn(new OrderHistorySummaryDTO(2, 1, new java.math.BigDecimal("150.00"), new java.math.BigDecimal("75.00")));

        mockMvc.perform(get("/api/orders/mine/history").param("status", "delivered").param("payment", "PIX")
                        .param("from", "2026-10-01").param("to", "2026-10-31").param("mode", "DELIVERY").param("q", "maria"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/mine/history/summary").param("payment", "PIX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(2))
                .andExpect(jsonPath("$.revenue").value(150.00));

        verify(orderService).findMyHistory(eq("fornecedor-1"),
                eq(OrderHistoryFilter.parse("delivered", "2026-10-01", "2026-10-31", "PIX", "DELIVERY", "maria")), any());
    }

    @Test
    void deveRetornar400ComFiltroInvalido() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "fornecedor-1");

        mockMvc.perform(get("/api/orders/mine/history").param("payment", "BOLETO")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orders/mine/history/summary").param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).findMyHistory(any(), any(), any());
    }

    @Test
    void deveRetornar403AoListarPedidosDeOutroFornecedor() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "fornecedor-1");

        mockMvc.perform(get("/api/orders/fornecedor-2/exclude-status")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/by-supplier/fornecedor-2")).andExpect(status().isForbidden());

        verify(orderService, never()).findBySupplierId(any(), any());
        verify(orderService, never()).findBySupplierIdAndStatusNotDeliveryAndCanceled(any(), any(), any());
    }

    // ---- HU23 ----
    @Test
    void deveContarOsPedidosDoFornecedorDoToken() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "fornecedor-1");
        java.util.Map<com.h2gas.enums.OrderStatusEnum, Long> counts = new java.util.EnumMap<>(com.h2gas.enums.OrderStatusEnum.class);
        for (com.h2gas.enums.OrderStatusEnum st : com.h2gas.enums.OrderStatusEnum.values()) counts.put(st, 0L);
        counts.put(com.h2gas.enums.OrderStatusEnum.PENDING, 2L);
        when(orderService.countMyOrdersByStatus("fornecedor-1")).thenReturn(new com.h2gas.model.dto.OrderStatusCountsDTO(counts, 2, 2, new java.math.BigDecimal("150.00")));

        mockMvc.perform(get("/api/orders/mine/status-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.PENDING").value(2))
                .andExpect(jsonPath("$.counts.DELIVERED").value(0))
                .andExpect(jsonPath("$.active").value(2))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.receivable").value(150.00));
    }

    // ---- HU24 ----
    @Test
    void deveAvaliarOPedidoDoClienteDoToken() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER, "cliente-1");
        when(reviewService.reviewOrder(eq("pedido-1"), eq("cliente-1"), any()))
                .thenReturn(new com.h2gas.model.dto.OrderReviewDTO(4, 5, "Chegou rápido"));

        mockMvc.perform(post("/api/orders/pedido-1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeRating\": 4, \"delivererRating\": 5, \"comment\": \"Chegou rápido\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeRating").value(4))
                .andExpect(jsonPath("$.delivererRating").value(5))
                .andExpect(jsonPath("$.comment").value("Chegou rápido"));

        verify(reviewService).reviewOrder(eq("pedido-1"), eq("cliente-1"),
                eq(new com.h2gas.model.dto.CreateReviewDTO(4, 5, "Chegou rápido")));
    }

    @Test
    void deveRecusarNotaForaDe1a5() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER, "cliente-1");

        mockMvc.perform(post("/api/orders/pedido-1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeRating\": 6}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/orders/pedido-1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delivererRating\": 3}"))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).reviewOrder(any(), any(), any());
    }

    @Test
    void deveNegarAvaliacaoAoFornecedor() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "fornecedor-1");

        mockMvc.perform(post("/api/orders/pedido-1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeRating\": 5}"))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).reviewOrder(any(), any(), any());
    }

    // F12 Cenário 4: cliente não lista os pedidos de outro cliente
    @Test
    void deveNegarPedidosDeOutroCliente() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER, "cliente-1");

        mockMvc.perform(get("/api/orders/by-customer/cliente-2"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).findByCustomerId(any(), any(), any());
    }
}
