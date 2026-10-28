package com.h2gas.configuration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU13: vitrine pública — com o filtro de segurança de verdade (os testes de controller rodam sem filtros)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class PublicCatalogAccessTest {

    @Autowired
    private MockMvc mockMvc;

    private static ResultMatcher semBloqueio() {
        return result -> {
            int s = result.getResponse().getStatus();
            assertTrue(s != 401 && s != 403, "rota pública respondeu " + s + " sem token");
        };
    }

    private static ResultMatcher bloqueada() {
        return result -> {
            int s = result.getResponse().getStatus();
            assertTrue(s == 401 || s == 403, "rota protegida respondeu " + s + " sem token");
        };
    }

    @Test
    @DisplayName("HU13: o catálogo abre sem token (só leitura)")
    void devePermitirCatalogoSemToken() throws Exception {
        mockMvc.perform(get("/api/suppliers/actives/open").param("cityId", "1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/suppliers/loja-inexistente/storefront")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/suppliers/loja-inexistente/reviews")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/product-items/by-supplier/loja-inexistente")).andExpect(semBloqueio());
        mockMvc.perform(get("/api/product-items/by-supplier-open").param("cityId", "1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/product-types")).andExpect(status().isOk());
        // HU19: a lista de categorias é pública (vitrine e formulário de produto)
        mockMvc.perform(get("/api/category-products")).andExpect(status().isOk());
        // HU20: o carrinho do visitante consulta preço e disponibilidade sem login
        mockMvc.perform(post("/api/cart/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":999999,\"quantity\":1}]}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU13: checkout, dados completos da loja e áreas do usuário seguem exigindo login")
    void deveExigirTokenNoCheckoutEMinhasAreas() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(bloqueada());
        mockMvc.perform(post("/api/orders/preview").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(bloqueada());
        mockMvc.perform(get("/api/suppliers/loja-1")).andExpect(bloqueada());
        mockMvc.perform(get("/api/suppliers/mine/profile")).andExpect(bloqueada());
        mockMvc.perform(get("/api/orders/mine/history")).andExpect(bloqueada());
    }

    @Test
    @DisplayName("HU13: escrita nas bases do catálogo continua protegida")
    void naoDevePermitirEscritaNasRotasDoCatalogo() throws Exception {
        mockMvc.perform(post("/api/product-types").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(bloqueada());
        mockMvc.perform(post("/api/category-products").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(bloqueada());
        mockMvc.perform(post("/api/product-items").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(bloqueada());
        mockMvc.perform(put("/api/suppliers/mine/banner")).andExpect(bloqueada());
        mockMvc.perform(delete("/api/product-items/1")).andExpect(bloqueada());
    }

    @Test
    @DisplayName("HU13: visitante precisa informar a cidade para ver o catálogo (RN05)")
    void deveExigirCidadeDoVisitanteNoCatalogo() throws Exception {
        mockMvc.perform(get("/api/suppliers/actives/open"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Informe a cidade para ver o catálogo."));
        mockMvc.perform(get("/api/product-items/by-supplier-open"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Informe a cidade para ver o catálogo."));
    }

    @Test
    @DisplayName("HU13: as rotas da vitrine aparecem sem cadeado no OpenAPI")
    void deveDocumentarAsRotasDaVitrineComoPublicas() throws Exception {
        var docs = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        for (String path : java.util.List.of("/api/suppliers/actives/open", "/api/suppliers/{id}/storefront", "/api/suppliers/{id}/reviews",
                "/api/product-items/by-supplier/{supplierId}", "/api/product-items/by-supplier-open", "/api/product-types",
                "/api/category-products")) {
            docs.andExpect(jsonPath("$.paths['" + path + "'].get.security").isEmpty());
        }
        docs.andExpect(jsonPath("$.paths['/api/cart/preview'].post.security").isEmpty());
        docs.andExpect(jsonPath("$.paths['/api/suppliers/{id}'].get.security").doesNotExist());
    }
}
