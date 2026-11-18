package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.dto.CartPreviewDTO;
import com.h2gas.model.dto.CartPreviewRequestDTO;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.CartPreviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU20: rota pública do preview do carrinho — sem usuário no contexto, como o visitante
@WebMvcTest(CartController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CartPreviewService cartPreviewService;

    private org.springframework.test.web.servlet.ResultActions enviar(String corpo) throws Exception {
        return mockMvc.perform(post("/api/cart/preview").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    @DisplayName("HU20: responde sem token e devolve o preview")
    void deveResponderSemToken() throws Exception {
        when(cartPreviewService.preview(any(CartPreviewRequestDTO.class)))
                .thenReturn(new CartPreviewDTO("loja-1", "Gás Centro", List.of(), new BigDecimal("110.00")));

        enviar("{\"items\":[{\"productId\":5,\"quantity\":1}],\"deliveryMode\":\"DELIVERY\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value("loja-1"))
                .andExpect(jsonPath("$.subtotal").value(110.00));
    }

    @Test
    @DisplayName("HU20: lista vazia → 400")
    void deveRecusarListaVazia() throws Exception {
        enviar("{\"items\":[]}").andExpect(status().isBadRequest());
        verifyNoInteractions(cartPreviewService);
    }

    @Test
    @DisplayName("HU20: quantidade zero → 400")
    void deveRecusarQuantidadeZero() throws Exception {
        enviar("{\"items\":[{\"productId\":5,\"quantity\":0}]}").andExpect(status().isBadRequest());
        verifyNoInteractions(cartPreviewService);
    }

    @Test
    @DisplayName("HU20: mais de 50 itens → 400")
    void deveRecusarMaisDeCinquentaItens() throws Exception {
        String itens = IntStream.rangeClosed(1, 51)
                .mapToObj(i -> "{\"productId\":" + i + ",\"quantity\":1}")
                .collect(Collectors.joining(","));
        enviar("{\"items\":[" + itens + "]}").andExpect(status().isBadRequest());
        verifyNoInteractions(cartPreviewService);
    }

    @Test
    @DisplayName("HU20: modalidade inválida → 400")
    void deveRecusarModalidadeInvalida() throws Exception {
        enviar("{\"items\":[{\"productId\":5,\"quantity\":1}],\"deliveryMode\":\"DRONE\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(cartPreviewService);
    }
}
