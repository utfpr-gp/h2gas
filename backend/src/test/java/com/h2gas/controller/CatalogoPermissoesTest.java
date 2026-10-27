package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.CategoryProductService;
import com.h2gas.services.ProductItemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU12 (Cenário 7): categoria e produto são do fornecedor; o admin só cuida dos tipos
@WebMvcTest({CategoryProductController.class, ProductItemController.class})
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CatalogoPermissoesTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CategoryProductService categoryProductService;
    @MockitoBean private CategoryProductMapper categoryProductMapper;
    @MockitoBean private ProductItemService productItemService;
    @MockitoBean private ProductItemMapper productItemMapper;
    @MockitoBean private CreateProductItemMapper createProductItemMapper;

    @BeforeEach
    void autenticarComoAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU19: o admin escreve categoria (revoga o 403 da HU12)")
    void deveRetornarOkAoAdminEscreverCategoria() throws Exception {
        mockMvc.perform(delete("/api/category-products/1")).andExpect(status().isOk());

        verify(categoryProductService).deleteCategory(1L);
    }

    @Test
    @DisplayName("HU12: admin não escreve nem abre produto para edição (403)")
    void deveRetornar403AoAdminEscreverProduto() throws Exception {
        mockMvc.perform(delete("/api/product-items/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/product-items/1")).andExpect(status().isForbidden());

        verifyNoInteractions(productItemService);
    }
}
