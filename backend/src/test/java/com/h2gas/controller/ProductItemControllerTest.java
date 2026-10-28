package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.dto.ProductStockSummaryDTO;
import com.h2gas.enums.StockFilterEnum;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.CreateProductItemMapper;
import com.h2gas.model.mapper.ProductItemMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.ProductItemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductItemController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ProductItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProductItemService productItemService;

    @MockitoBean
    private ProductItemMapper productItemMapper;

    @MockitoBean
    private CreateProductItemMapper createProductItemMapper;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAceitarParametrosCityIdCategoriaESort() throws Exception {
        var authToken = new UsernamePasswordAuthenticationToken(
                "cliente-logado", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        ProductItemDTO dto = new ProductItemDTO();
        dto.setBrand("Botijão Curitiba");
        Page<ProductItemDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        when(productItemService.findBySupplierActiveAndSupplierOpen(eq(7L), eq("Gás"), isNull(), eq(com.h2gas.enums.ProductSortEnum.PRICE_ASC), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/product-items/by-supplier-open")
                        .param("cityId", "7")
                        .param("category", "Gás")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk());

        verify(productItemService).findBySupplierActiveAndSupplierOpen(eq(7L), eq("Gás"), isNull(), eq(com.h2gas.enums.ProductSortEnum.PRICE_ASC), any());
    }

    private void autenticarComo(String papel) {
        var authToken = new UsernamePasswordAuthenticationToken(
                "usuario-logado", null, List.of(new SimpleGrantedAuthority("ROLE_" + papel)));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        when(productItemService.findProductsFiltered(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
    }

    @Test
    void deveForcarOnlyAvailableParaCliente() throws Exception {
        autenticarComo("CUSTOMER");

        mockMvc.perform(get("/api/product-items/by-supplier/forn-1").param("onlyAvailable", "false"))
                .andExpect(status().isOk());

        verify(productItemService).findProductsFiltered(eq("forn-1"), any(), any(), eq(true), any());
    }

    // HU13: o visitante (sem login) também nunca vê produto zerado (RN02)
    @Test
    void deveForcarOnlyAvailableParaVisitante() throws Exception {
        when(productItemService.findProductsFiltered(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/product-items/by-supplier/forn-1").param("onlyAvailable", "false"))
                .andExpect(status().isOk());

        verify(productItemService).findProductsFiltered(eq("forn-1"), any(), any(), eq(true), any());
    }

    @Test
    void devePermitirFornecedorVerProdutosZerados() throws Exception {
        autenticarComo("SUPPLIER");

        mockMvc.perform(get("/api/product-items/by-supplier/forn-1"))
                .andExpect(status().isOk());

        verify(productItemService).findProductsFiltered(eq("forn-1"), any(), any(), eq(false), any());
    }

    private void autenticarFornecedor() {
        User fornecedor = new User(null, "Fornecedor", "fornecedor@teste.com", "41999999999", "12345678900", "senha", UserTypeEnum.SUPPLIER);
        fornecedor.setId("forn-1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                fornecedor, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
    }

    private String produtoJson(String precos) {
        return "{\"brand\":\"Ultragaz\",\"categoryProductId\":3,\"stockQuantity\":10,\"description\":\"Botijão\"" + precos + "}";
    }

    private org.springframework.test.web.servlet.ResultActions postarProduto(String json) throws Exception {
        return mockMvc.perform(multipart("/api/product-items")
                .file(new MockMultipartFile("product", "", "application/json", json.getBytes()))
                .file(new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3})));
    }

    @Test
    void deveRejeitarCriacaoSemPreco() throws Exception {
        autenticarFornecedor();

        postarProduto(produtoJson("")).andExpect(status().isBadRequest());

        verify(productItemService, never()).createProductItem(any(), any(), any());
    }

    @Test
    void deveRejeitarCriacaoComPrecoNegativo() throws Exception {
        autenticarFornecedor();

        postarProduto(produtoJson(",\"price\":-1.00")).andExpect(status().isBadRequest());

        verify(productItemService, never()).createProductItem(any(), any(), any());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("HU21: o produto é cadastrado só com o preço")
    void deveAceitarCriacaoComPrecoUnico() throws Exception {
        autenticarFornecedor();
        ProductItemDTO criado = new ProductItemDTO();
        criado.setBrand("Ultragaz");
        when(productItemService.createProductItem(any(), any(), any())).thenReturn(criado);

        postarProduto(produtoJson(",\"price\":100.00")).andExpect(status().isOk());

        verify(productItemService).createProductItem(any(), any(), any());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("HU21: um frete enviado por cliente antigo é ignorado")
    void deveIgnorarFreteEnviadoPorClienteAntigo() throws Exception {
        autenticarFornecedor();
        when(productItemService.createProductItem(any(), any(), any())).thenReturn(new ProductItemDTO());

        postarProduto(produtoJson(",\"price\":100.00,\"shippingFee\":20.00")).andExpect(status().isOk());

        org.mockito.ArgumentCaptor<com.h2gas.model.dto.CreateProductItemDTO> enviado =
                org.mockito.ArgumentCaptor.forClass(com.h2gas.model.dto.CreateProductItemDTO.class);
        verify(productItemService).createProductItem(enviado.capture(), any(), any());
        org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("100.00").compareTo(enviado.getValue().getPrice()));
    }

    @Test
    void deveRejeitarEdicaoSemPreco() throws Exception {
        autenticarFornecedor();

        mockMvc.perform(put("/api/product-items/12").contentType("application/json").content(produtoJson("")))
                .andExpect(status().isBadRequest());

        verify(productItemService, never()).updateProductItem(any(), any(), any());
    }

    @Test
    void deveAceitarEdicaoComPrecoUnico() throws Exception {
        autenticarFornecedor();
        when(productItemService.updateProductItem(eq(12L), any(), any())).thenReturn(new ProductItemDTO());

        mockMvc.perform(put("/api/product-items/12").contentType("application/json")
                        .content(produtoJson(",\"price\":120.00")))
                .andExpect(status().isOk());

        verify(productItemService).updateProductItem(eq(12L), any(), any());
    }

    @Test
    void deveListarSoOsProdutosDoToken() throws Exception {
        autenticarFornecedor();
        when(productItemService.findMine(eq("forn-1"), eq("ultra"), eq(StockFilterEnum.LOW), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(productItemService.summarizeMine("forn-1")).thenReturn(new ProductStockSummaryDTO(7, 6, 2, 1, 8));

        mockMvc.perform(get("/api/product-items/mine").param("q", "ultra").param("stock", "low"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/product-items/mine/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStock").value(2))
                .andExpect(jsonPath("$.lowStockThreshold").value(8));

        verify(productItemService).findMine(eq("forn-1"), eq("ultra"), eq(StockFilterEnum.LOW), any());
    }

    @Test
    void deveRetornar400ComFiltroDeEstoqueInvalido() throws Exception {
        autenticarFornecedor();

        mockMvc.perform(get("/api/product-items/mine").param("stock", "metade"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Filtro de estoque inválido: use all, available, low ou out."));

        verify(productItemService, never()).findMine(any(), any(), any(), any());
    }

    @Test
    void deveRepassarOFornecedorDoTokenAoExcluir() throws Exception {
        autenticarFornecedor();

        mockMvc.perform(delete("/api/product-items/9")).andExpect(status().isOk());

        verify(productItemService).deleteProductItem(eq(9L), org.mockito.ArgumentMatchers.argThat(u -> "forn-1".equals(u.getId())));
    }

    @Test
    void deveRepassarABuscaComACidade() throws Exception {
        autenticarComo("CUSTOMER");
        when(productItemService.findBySupplierActiveAndSupplierOpen(eq(7L), isNull(), eq("minalba"), eq(com.h2gas.enums.ProductSortEnum.NONE), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/product-items/by-supplier-open").param("cityId", "7").param("q", "minalba"))
                .andExpect(status().isOk());

        verify(productItemService).findBySupplierActiveAndSupplierOpen(eq(7L), isNull(), eq("minalba"), eq(com.h2gas.enums.ProductSortEnum.NONE), any());
    }

    // ---- HU16 ----

    @Test
    void deveRepassarAOrdenacaoPelaNotaDaLoja() throws Exception {
        when(productItemService.findBySupplierActiveAndSupplierOpen(eq(7L), isNull(), isNull(),
                eq(com.h2gas.enums.ProductSortEnum.SHOP_RATING), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/product-items/by-supplier-open").param("cityId", "7").param("sort", "rating"))
                .andExpect(status().isOk());

        verify(productItemService).findBySupplierActiveAndSupplierOpen(eq(7L), isNull(), isNull(),
                eq(com.h2gas.enums.ProductSortEnum.SHOP_RATING), any());
    }

    @Test
    void deveRecusarOrdenacaoInvalidaDeProdutos() throws Exception {
        mockMvc.perform(get("/api/product-items/by-supplier-open").param("cityId", "7").param("sort", "stockQuantity,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ordenação inválida."));

        verifyNoInteractions(productItemService);
    }
}
