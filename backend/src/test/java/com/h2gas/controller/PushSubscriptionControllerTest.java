package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.PushSubscriptionRequestDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.PushSubscriptionService;
import com.h2gas.services.WebPushSender;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HU27
@WebMvcTest(PushSubscriptionController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class PushSubscriptionControllerTest {

    private static final String CORPO = """
            {"endpoint": "https://fcm.googleapis.com/fcm/send/abc", "keys": {"p256dh": "pk", "auth": "au"}}""";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PushSubscriptionService service;
    @MockitoBean private WebPushSender sender;

    private void autenticarComo(UserTypeEnum tipo) {
        User user = new User("u-1", "Maria", "m@teste.com", "41900000000", null, "s", tipo);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveGravarParaOUsuarioDoToken() throws Exception {
        autenticarComo(UserTypeEnum.DELIVERER);

        mockMvc.perform(post("/api/push/subscriptions").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isNoContent());

        verify(service).subscribe("u-1", new PushSubscriptionRequestDTO("https://fcm.googleapis.com/fcm/send/abc",
                new PushSubscriptionRequestDTO.Keys("pk", "au")));
    }

    @Test
    void deveApagarDoUsuarioDoToken() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER);

        mockMvc.perform(delete("/api/push/subscriptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\": \"https://fcm.googleapis.com/fcm/send/abc\"}"))
                .andExpect(status().isNoContent());

        verify(service).unsubscribe("u-1", "https://fcm.googleapis.com/fcm/send/abc");
    }

    @Test
    void deveRecusarCorpoInvalido() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER);

        mockMvc.perform(post("/api/push/subscriptions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\": \"\", \"keys\": {\"p256dh\": \"pk\"}}")).andExpect(status().isBadRequest());

        verify(service, never()).subscribe(any(), any());
    }

    @Test
    void deveNegarSemToken() throws Exception {
        mockMvc.perform(post("/api/push/subscriptions").contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().is4xxClientError());

        verify(service, never()).subscribe(any(), any());
    }

    @Test
    void deveDevolverAChavePublica() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER);
        when(sender.publicKey()).thenReturn("BPublica");
        when(sender.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/push/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("BPublica"))
                .andExpect(jsonPath("$.configured").value(true));
    }
}
