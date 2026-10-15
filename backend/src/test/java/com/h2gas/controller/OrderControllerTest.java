package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.dto.OrderPreviewDTO;
import com.h2gas.model.dto.OrderPreviewItemDTO;
import com.h2gas.model.dto.OrderPreviewRequestDTO;
import com.h2gas.model.mapper.OrderMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.OrderService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
