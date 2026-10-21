package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.EmailChangeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailChangeController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class EmailChangeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private EmailChangeService emailChangeService;

    private User autenticarComo(UserTypeEnum tipo) {
        User user = new User("u-" + tipo, "Usuário", "u@teste.com", "41900000000", null, "senha", tipo);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return user;
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU12: admin pede a troca e recebe 202")
    void deveRetornar202AoPedirCodigo() throws Exception {
        User admin = autenticarComo(UserTypeEnum.ADMIN);

        mockMvc.perform(post("/api/users/me/email-change").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\": \"novo@h2gas.local\"}"))
                .andExpect(status().isAccepted());

        verify(emailChangeService).requestChange(same(admin), eq("novo@h2gas.local"));
    }

    @Test
    @DisplayName("HU12: código certo devolve 200 com o token novo; e-mail ou código mal formados dão 400")
    void deveRetornar200ComTokenAoConfirmar() throws Exception {
        User admin = autenticarComo(UserTypeEnum.ADMIN);
        when(emailChangeService.confirmChange(same(admin), eq("123456"))).thenReturn("jwt-novo");

        mockMvc.perform(post("/api/users/me/email-change/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-novo"));

        mockMvc.perform(post("/api/users/me/email-change/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"12a\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users/me/email-change").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\": \"sem-arroba\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU12: só o ADMIN troca o e-mail por aqui (403 para os outros perfis)")
    void deveRetornar403ParaNaoAdmin() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER);

        mockMvc.perform(post("/api/users/me/email-change").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newEmail\": \"novo@h2gas.local\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users/me/email-change/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"123456\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(emailChangeService);
    }
}
