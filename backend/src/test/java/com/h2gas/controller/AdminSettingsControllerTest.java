package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.enums.AppSettingKey;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AppSettingDTO;
import com.h2gas.model.dto.AppSettingsDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.AppSettingService;
import com.h2gas.configuration.security.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU26
@WebMvcTest(AdminSettingsController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminSettingsControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AppSettingService appSettingService;

    private void autenticarComo(UserTypeEnum tipo) {
        User user = new User("u-" + tipo, "Usuário", "u@teste.com", "41900000000", null, "senha", tipo);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveListarParaAdmin() throws Exception {
        autenticarComo(UserTypeEnum.ADMIN);
        when(appSettingService.list()).thenReturn(new AppSettingsDTO(true, List.of(
                new AppSettingDTO(AppSettingKey.WHATSAPP_LOGIN_CODE, false),
                new AppSettingDTO(AppSettingKey.WHATSAPP_ORDER_OUT_FOR_DELIVERY, true))));

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappConfigured").value(true))
                .andExpect(jsonPath("$.settings[0].key").value("WHATSAPP_LOGIN_CODE"))
                .andExpect(jsonPath("$.settings[1].enabled").value(true));
    }

    @Test
    void deveNegarParaCliente() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER);

        mockMvc.perform(get("/api/admin/settings")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/settings/WHATSAPP_LOGIN_CODE").contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": true}")).andExpect(status().isForbidden());
        verify(appSettingService, never()).update(any(), anyBoolean());
    }

    @Test
    void deveAtualizar() throws Exception {
        autenticarComo(UserTypeEnum.ADMIN);
        when(appSettingService.update(AppSettingKey.WHATSAPP_LOGIN_CODE, true))
                .thenReturn(new AppSettingDTO(AppSettingKey.WHATSAPP_LOGIN_CODE, true));

        mockMvc.perform(put("/api/admin/settings/WHATSAPP_LOGIN_CODE").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void deveRecusarChaveDesconhecidaOuCorpoSemEnabled() throws Exception {
        autenticarComo(UserTypeEnum.ADMIN);

        mockMvc.perform(put("/api/admin/settings/NAO_EXISTE").contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": true}")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/admin/settings/WHATSAPP_LOGIN_CODE").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        verify(appSettingService, never()).update(any(), anyBoolean());
    }
}
