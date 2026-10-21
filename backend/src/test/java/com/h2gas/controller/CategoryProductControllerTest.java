package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CategoryRequestDTO;
import com.h2gas.model.dto.SupplierCategoryDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.CategoryProductMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.CategoryProductService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryProductController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CategoryProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CategoryProductService categoryProductService;

    @MockitoBean
    private CategoryProductMapper categoryProductMapper;

    @BeforeEach
    void autenticarComoFornecedor() {
        User fornecedor = new User(null, "Fornecedor", "f@teste.com", "41999999999", "12345678900", "senha", UserTypeEnum.SUPPLIER);
        fornecedor.setId("forn-1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                fornecedor, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornar201AoCriarCategoria() throws Exception {
        when(categoryProductService.createCategory("forn-1", new CategoryRequestDTO("P13", 2L)))
                .thenReturn(new CategoryProductDTO(5L, "P13", "Gás", 2L, "forn-1"));

        // supplierId no corpo é ignorado: vale o do token
        mockMvc.perform(post("/api/category-products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P13\",\"productTypeId\":2,\"supplierId\":\"outro\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));

        verify(categoryProductService).createCategory("forn-1", new CategoryRequestDTO("P13", 2L));
    }

    @Test
    void deveRetornar400SemNomeOuTipo() throws Exception {
        mockMvc.perform(post("/api/category-products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"productTypeId\":2}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/category-products/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P13\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryProductService, never()).createCategory(any(), any());
        verify(categoryProductService, never()).update(any(), any(), any());
    }

    @Test
    void deveListarAsCategoriasDoToken() throws Exception {
        when(categoryProductService.findMine("forn-1"))
                .thenReturn(List.of(new SupplierCategoryDTO(5L, "P13", "Gás", 2L, "local_fire_department", 3L)));

        mockMvc.perform(get("/api/category-products/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCount").value(3))
                .andExpect(jsonPath("$[0].typeIcon").value("local_fire_department"));
    }
}
