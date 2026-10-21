package com.h2gas.controller;

import com.h2gas.model.dto.DashboardFilter;
import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierDashboardDTO;
import com.h2gas.model.dto.SupplierSettingsDTO;
import com.h2gas.model.dto.SupplierProfileDTO;
import com.h2gas.model.dto.SupplierProfileRequestDTO;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.SupplierDashboardService;
import com.h2gas.services.SupplierService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private SupplierMapper supplierMapper;

    @MockitoBean
    private SupplierDashboardService supplierDashboardService;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoFornecedor(String id) {
        User fornecedor = new User(null, "Fornecedor", "fornecedor@teste.com", "41999999999", "12345678900", "senha", UserTypeEnum.SUPPLIER);
        fornecedor.setId(id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                fornecedor, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
    }

    @Test
    void deveRetornar200ComDashboardDoFornecedorLogado() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        SupplierDashboardDTO dashboard = new SupplierDashboardDTO();
        dashboard.setMonth("2026-10");
        when(supplierDashboardService.buildDashboard(eq("fornecedor-1"), eq(YearMonth.of(2026, 10)), any())).thenReturn(dashboard);

        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"));

        verify(supplierDashboardService).buildDashboard("fornecedor-1", YearMonth.of(2026, 10), DashboardFilter.NONE);
    }

    @Test
    void deveUsarMesCorrenteQuandoNaoInformado() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierDashboardService.buildDashboard(eq("fornecedor-1"), any(), any())).thenReturn(new SupplierDashboardDTO());

        mockMvc.perform(get("/api/suppliers/mine/dashboard")).andExpect(status().isOk());

        verify(supplierDashboardService).buildDashboard("fornecedor-1", YearMonth.now(), DashboardFilter.NONE);
    }

    @Test
    void deveRetornar400ParaMesFuturoOuInvalido() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierDashboardService.buildDashboard(eq("fornecedor-1"), any(), any()))
                .thenThrow(new BusinessRuleException("Mês futuro não tem indicadores."));

        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("month", "2099-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("month", "outubro"))
                .andExpect(status().isBadRequest());

        verify(supplierDashboardService, never()).buildDashboard(any(), eq(YearMonth.of(2026, 10)), any());
    }

    @Test
    void deveAceitarParametroCityIdOpcionalNoEndpointActivesOpen() throws Exception {
        SupplierDTO dto = new SupplierDTO();
        dto.setName("Fornecedor Curitiba");
        Page<SupplierDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        when(supplierService.findAllSuppliersOpenAndActive(eq(7L), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/actives/open").param("cityId", "7"))
                .andExpect(status().isOk());

        verify(supplierService).findAllSuppliersOpenAndActive(eq(7L), isNull(), any());
    }

    @Test
    void deveFuncionarSemCityIdMantendoComportamentoAtual() throws Exception {
        Page<SupplierDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(supplierService.findAllSuppliersOpenAndActive(isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/actives/open"))
                .andExpect(status().isOk());

        verify(supplierService).findAllSuppliersOpenAndActive(isNull(), isNull(), any());
    }

    @Test
    void deveUsarOFornecedorDoToken() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierService.findMySettings("fornecedor-1")).thenReturn(new SupplierSettingsDTO("08:00", "18:00", 30, 8));
        when(supplierService.updateMySettings(eq("fornecedor-1"), any())).thenReturn(new SupplierSettingsDTO("09:00", "19:00", 40, 6));

        mockMvc.perform(get("/api/suppliers/mine/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowStockThreshold").value(8));
        mockMvc.perform(put("/api/suppliers/mine/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingTime\":\"09:00\",\"closingTime\":\"19:00\",\"averageDeliveryTime\":40,\"lowStockThreshold\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingTime").value("09:00"));

        verify(supplierService).updateMySettings("fornecedor-1", new SupplierSettingsDTO("09:00", "19:00", 40, 6));
    }

    @Test
    void deveRetornar400ComTempoDeEntregaForaDoLimite() throws Exception {
        autenticarComoFornecedor("fornecedor-1");

        mockMvc.perform(put("/api/suppliers/mine/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingTime\":\"09:00\",\"closingTime\":\"19:00\",\"averageDeliveryTime\":500,\"lowStockThreshold\":6}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/suppliers/mine/settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingTime\":\"9h\",\"closingTime\":\"19:00\",\"averageDeliveryTime\":30,\"lowStockThreshold\":6}"))
                .andExpect(status().isBadRequest());

        verify(supplierService, never()).updateMySettings(any(), any());
    }

    @Test
    void deveRetornar403AoAlterarHorarioDeOutroFornecedor() throws Exception {
        autenticarComoFornecedor("fornecedor-1");

        mockMvc.perform(patch("/api/suppliers/fornecedor-2/update-times").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingTime\":\"00:00\"}"))
                .andExpect(status().isForbidden());

        verify(supplierService, never()).updateTimesSupplier(any(), any());
    }

    private void autenticarComoAdmin() {
        User admin = new User(null, "Admin", "admin@teste.com", "41999999999", "12345678900", "senha", UserTypeEnum.ADMIN);
        admin.setId("admin-1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void deveRepassarFiltrosDeStatusEBusca() throws Exception {
        autenticarComoAdmin();
        SupplierDTO dto = new SupplierDTO();
        dto.setName("Gás Centro");
        when(supplierService.findAllSuppliers(eq(false), eq("gás"), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/suppliers").param("active", "false").param("q", "gás"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Gás Centro"));

        verify(supplierService).findAllSuppliers(eq(false), eq("gás"), any());
    }

    @Test
    void deveListarTodosQuandoSemFiltros() throws Exception {
        autenticarComoAdmin();
        when(supplierService.findAllSuppliers(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/suppliers")).andExpect(status().isOk());

        verify(supplierService).findAllSuppliers(isNull(), isNull(), any());
    }

    @Test
    void deveUsarALojaDoTokenNoPerfil() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        SupplierProfileDTO perfil = new SupplierProfileDTO("Gás Centro", "gascentro@h2gas.local", "41999999999", "Desde 1990", "12345678000101",
                null, null, 3L, "Rua XV", "700", "Centro", "Curitiba", "PR");
        when(supplierService.findMyProfile("fornecedor-1")).thenReturn(perfil);
        when(supplierService.updateMyProfile(eq("fornecedor-1"), any())).thenReturn(perfil);

        mockMvc.perform(get("/api/suppliers/mine/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("12345678000101"));
        mockMvc.perform(put("/api/suppliers/mine/profile").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gás Centro\",\"phone\":\"41999999999\",\"description\":\"Desde 1990\"}"))
                .andExpect(status().isOk());

        verify(supplierService).updateMyProfile("fornecedor-1", new SupplierProfileRequestDTO("Gás Centro", "41999999999", "Desde 1990"));
    }

    @Test
    void deveRetornar400ComNomeVazio() throws Exception {
        autenticarComoFornecedor("fornecedor-1");

        mockMvc.perform(put("/api/suppliers/mine/profile").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"phone\":\"41999999999\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/suppliers/mine/profile").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gás\",\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest());

        verify(supplierService, never()).updateMyProfile(any(), any());
    }

    @Test
    void deveRetornar403AoEditarLojaDeOutroPeloId() throws Exception {
        autenticarComoFornecedor("fornecedor-1");

        mockMvc.perform(put("/api/suppliers/fornecedor-2").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Invadida\"}"))
                .andExpect(status().isForbidden());

        verify(supplierService, never()).update(any(), any());
    }

    @Test
    void deveEnviarALogoComoMultipart() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierService.updateMyLogo(eq("fornecedor-1"), any())).thenReturn("https://res.cloudinary.com/h2gas/logo.png");

        mockMvc.perform(multipart("/api/suppliers/mine/logo")
                        .file(new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3}))
                        .with(r -> { r.setMethod("PUT"); return r; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://res.cloudinary.com/h2gas/logo.png"));
        mockMvc.perform(delete("/api/suppliers/mine/logo")).andExpect(status().isNoContent());

        verify(supplierService).removeMyLogo("fornecedor-1");
    }

    @Test
    void deveRetornar403AoAbrirOuFecharLojaDeOutro() throws Exception {
        autenticarComoFornecedor("fornecedor-1");

        mockMvc.perform(patch("/api/suppliers/fornecedor-2/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":true}"))
                .andExpect(status().isForbidden());

        verify(supplierService, never()).updateStatusOpenClosed(any(), anyBoolean());
    }

    @Test
    void deveRepassarOsFiltrosDoDashboard() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierDashboardService.buildDashboard(eq("fornecedor-1"), any(), any())).thenReturn(new SupplierDashboardDTO());

        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("month", "2026-10").param("payment", "pix")
                        .param("mode", "PICKUP").param("typeId", "2").param("categoryId", "7").param("productId", "4"))
                .andExpect(status().isOk());

        verify(supplierDashboardService).buildDashboard("fornecedor-1", YearMonth.of(2026, 10),
                DashboardFilter.parse("PIX", "PICKUP", "2", "7", "4"));
    }

    @Test
    void deveRetornar400ComFiltroInvalidoNoDashboard() throws Exception {
        autenticarComoFornecedor("fornecedor-1");

        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("payment", "BOLETO")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("typeId", "abc")).andExpect(status().isBadRequest());

        verify(supplierDashboardService, never()).buildDashboard(any(), any(), any());
    }

    @Test
    void deveRepassarABuscaComACidade() throws Exception {
        when(supplierService.findAllSuppliersOpenAndActive(eq(7L), eq("gás azul"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/suppliers/actives/open").param("cityId", "7").param("q", "gás azul"))
                .andExpect(status().isOk());

        verify(supplierService).findAllSuppliersOpenAndActive(eq(7L), eq("gás azul"), any());
    }

    @Test
    void deveExporNotaEContagemNoCatalogo() throws Exception {
        SupplierDTO loja = new SupplierDTO();
        loja.setName("Gás Centro");
        loja.setAverageRating(4.3);
        loja.setReviewCount(14L);
        when(supplierService.findAllSuppliersOpenAndActive(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(loja), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/suppliers/actives/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].averageRating").value(4.3))
                .andExpect(jsonPath("$.data[0].reviewCount").value(14));
    }

    @Test
    void deveEnviarOBannerDoFornecedorDoToken() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierService.updateMyBanner(eq("fornecedor-1"), any())).thenReturn("https://res.cloudinary.com/h2gas/banner.jpg");

        mockMvc.perform(multipart("/api/suppliers/mine/banner")
                        .file(new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .with(r -> { r.setMethod("PUT"); return r; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bannerUrl").value("https://res.cloudinary.com/h2gas/banner.jpg"));
        mockMvc.perform(delete("/api/suppliers/mine/banner")).andExpect(status().isNoContent());

        verify(supplierService).removeMyBanner("fornecedor-1");
    }

    @Test
    void deveBloquearBannerParaQuemNaoEFornecedor() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cliente", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        mockMvc.perform(multipart("/api/suppliers/mine/banner")
                        .file(new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .with(r -> { r.setMethod("PUT"); return r; }))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/suppliers/mine/banner")).andExpect(status().isForbidden());

        verifyNoInteractions(supplierService);
    }

    @Test
    void deveListarAvaliacoesPaginadas() throws Exception {
        com.h2gas.model.dto.StoreReviewDTO review = new com.h2gas.model.dto.StoreReviewDTO(
                1L, "Maria", 4, "Entrega rápida", java.time.LocalDateTime.of(2026, 10, 18, 14, 30));
        when(supplierService.findReviews(eq("loja-1"), any()))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/suppliers/loja-1/reviews").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customerFirstName").value("Maria"))
                .andExpect(jsonPath("$.data[0].rating").value(4))
                .andExpect(jsonPath("$.data[0].comment").value("Entrega rápida"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(supplierService).findReviews(eq("loja-1"), eq(PageRequest.of(0, 5)));
    }
}
