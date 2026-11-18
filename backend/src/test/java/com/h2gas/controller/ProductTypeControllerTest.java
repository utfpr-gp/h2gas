package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.ProductTypeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductTypeController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ProductTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProductTypeService productTypeService;

    @MockitoBean
    private ProductTypeMapper productTypeMapper;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveExigirApenasAutenticacaoParaListarTipos() throws Exception {
        var authToken = new UsernamePasswordAuthenticationToken(
                "cliente-logado", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(productTypeService.findAllProductTypes()).thenReturn(List.of(new ProductTypeDTO(1L, "Gás")));

        mockMvc.perform(get("/api/product-types"))
                .andExpect(status().isOk());
    }

    private void autenticarComo(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario", null, List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("HU12: admin cria tipo (201)")
    void deveCriarTipoComoAdmin() throws Exception {
        autenticarComo("ROLE_ADMIN");
        when(productTypeService.createProductType(any(ProductTypeDTO.class))).thenReturn(new ProductTypeDTO(3L, "Carvão"));

        mockMvc.perform(post("/api/product-types").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Carvão\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("HU12: admin renomeia tipo (200)")
    void deveRenomearTipoComoAdmin() throws Exception {
        autenticarComo("ROLE_ADMIN");
        when(productTypeService.updateProductType(eq(1L), any(ProductTypeDTO.class))).thenReturn(new ProductTypeDTO(1L, "Gás GLP"));

        mockMvc.perform(put("/api/product-types/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Gás GLP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gás GLP"));
    }

    @Test
    @DisplayName("HU12: fornecedor não cria nem renomeia tipo (403)")
    void deveRetornar403AoFornecedorCriarTipo() throws Exception {
        autenticarComo("ROLE_SUPPLIER");

        mockMvc.perform(post("/api/product-types").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Carvão\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/product-types/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Carvão\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productTypeService);
    }

    @Test
    @DisplayName("HU12: nome vazio é 400")
    void deveRetornar400ComNomeVazio() throws Exception {
        autenticarComo("ROLE_ADMIN");

        mockMvc.perform(post("/api/product-types").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productTypeService);
    }
}
