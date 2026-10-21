package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierDashboardDTO;
import com.h2gas.model.dto.SupplierSettingsDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(supplierDashboardService.buildDashboard("fornecedor-1", YearMonth.of(2026, 10))).thenReturn(dashboard);

        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"));

        verify(supplierDashboardService).buildDashboard("fornecedor-1", YearMonth.of(2026, 10));
    }

    @Test
    void deveUsarMesCorrenteQuandoNaoInformado() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierDashboardService.buildDashboard(eq("fornecedor-1"), any())).thenReturn(new SupplierDashboardDTO());

        mockMvc.perform(get("/api/suppliers/mine/dashboard")).andExpect(status().isOk());

        verify(supplierDashboardService).buildDashboard("fornecedor-1", YearMonth.now());
    }

    @Test
    void deveRetornar400ParaMesFuturoOuInvalido() throws Exception {
        autenticarComoFornecedor("fornecedor-1");
        when(supplierDashboardService.buildDashboard(eq("fornecedor-1"), any()))
                .thenThrow(new BusinessRuleException("Mês futuro não tem indicadores."));

        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("month", "2099-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/suppliers/mine/dashboard").param("month", "outubro"))
                .andExpect(status().isBadRequest());

        verify(supplierDashboardService, never()).buildDashboard(any(), eq(YearMonth.of(2026, 10)));
    }

    @Test
    void deveAceitarParametroCityIdOpcionalNoEndpointActivesOpen() throws Exception {
        SupplierDTO dto = new SupplierDTO();
        dto.setName("Fornecedor Curitiba");
        Page<SupplierDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);

        when(supplierService.findAllSuppliersOpenAndActive(eq(7L), any())).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/actives/open").param("cityId", "7"))
                .andExpect(status().isOk());

        verify(supplierService).findAllSuppliersOpenAndActive(eq(7L), any());
    }

    @Test
    void deveFuncionarSemCityIdMantendoComportamentoAtual() throws Exception {
        Page<SupplierDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(supplierService.findAllSuppliersOpenAndActive(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/suppliers/actives/open"))
                .andExpect(status().isOk());

        verify(supplierService).findAllSuppliersOpenAndActive(isNull(), any());
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
}
