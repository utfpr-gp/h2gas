package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AdminDashboardDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.AdminDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminDashboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AdminDashboardService dashboardService;

    private void autenticarComo(UserTypeEnum tipo) {
        User user = new User("u-" + tipo, "Usuário", "u@teste.com", "41900000000", null, "senha", tipo);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU12: painel do admin devolve os números da plataforma")
    void deveDevolverOPainelParaOAdmin() throws Exception {
        autenticarComo(UserTypeEnum.ADMIN);
        when(dashboardService.buildDashboard(any(LocalDate.class))).thenReturn(new AdminDashboardDto(
                Map.of(UserTypeEnum.CUSTOMER, 12L), 3, 2, 4, 5, 40,
                Map.of(OrderStatusEnum.PENDING, 6L), new BigDecimal("1234.50")));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usersByType.CUSTOMER").value(12))
                .andExpect(jsonPath("$.pendingSuppliers").value(2))
                .andExpect(jsonPath("$.ordersByStatus.PENDING").value(6))
                .andExpect(jsonPath("$.revenueThisMonth").value(1234.50));
    }

    @Test
    @DisplayName("HU12: painel do admin é 403 para os outros perfis")
    void deveRetornar403ParaNaoAdmin() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER);

        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }
}
