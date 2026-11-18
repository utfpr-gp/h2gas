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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    private static void autenticarComo(UserTypeEnum tipo, String id) {
        User usuario = new User(null, "Usuario", "u@teste.com", "41999999999", "12345678900", "senha", tipo);
        usuario.setId(id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + tipo.name()))));
    }

    @BeforeEach
    void autenticarComoAdmin() {
        autenticarComo(UserTypeEnum.ADMIN, "admin-1");
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---- HU19: categorias globais — só o admin escreve ----

    @Test
    void deveCriarComoAdmin() throws Exception {
        when(categoryProductService.createCategory(new CategoryRequestDTO("P13", 2L)))
                .thenReturn(new CategoryProductDTO(5L, "P13", "Gás", 2L));

        mockMvc.perform(post("/api/category-products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P13\",\"productTypeId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.supplierId").doesNotExist());

        verify(categoryProductService).createCategory(new CategoryRequestDTO("P13", 2L));
    }

    @Test
    void deveRetornar403AoFornecedorEscreverCategoria() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "forn-1");

        mockMvc.perform(post("/api/category-products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P13\",\"productTypeId\":2}")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/category-products/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P13\",\"productTypeId\":2}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/category-products/5")).andExpect(status().isForbidden());

        verifyNoInteractions(categoryProductService);
    }

    @Test
    void deveRetornar400SemNomeOuTipo() throws Exception {
        mockMvc.perform(post("/api/category-products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"productTypeId\":2}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/category-products/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P13\"}"))
                .andExpect(status().isBadRequest());

        verify(categoryProductService, never()).createCategory(any());
        verify(categoryProductService, never()).update(any(), any());
    }

    @Test
    void deveEditarEExcluirComoAdmin() throws Exception {
        when(categoryProductService.update(eq(5L), eq(new CategoryRequestDTO("P45", 2L))))
                .thenReturn(new CategoryProductDTO(5L, "P45", "Gás", 2L));

        mockMvc.perform(put("/api/category-products/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P45\",\"productTypeId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("P45"));
        mockMvc.perform(delete("/api/category-products/5")).andExpect(status().isOk());

        verify(categoryProductService).deleteCategory(5L);
    }

    @Test
    void deveListarTodasAsCategoriasSemExigirPapel() throws Exception {
        SecurityContextHolder.clearContext();
        when(categoryProductService.findAllCategoryProducts())
                .thenReturn(List.of(new CategoryProductDTO(5L, "P13", "Gás", 2L)));

        mockMvc.perform(get("/api/category-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("P13"))
                .andExpect(jsonPath("$[0].type").value("Gás"));
    }

    @Test
    void deveListarAsCategoriasQueALojaUsa() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER, "forn-1");
        when(categoryProductService.findMine("forn-1"))
                .thenReturn(List.of(new SupplierCategoryDTO(5L, "P13", "Gás", 2L, "local_fire_department", 3L)));

        mockMvc.perform(get("/api/category-products/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCount").value(3))
                .andExpect(jsonPath("$[0].typeIcon").value("local_fire_department"));
    }
}
