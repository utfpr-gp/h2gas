package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.sse.SseEmitterService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SseController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class SseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SseEmitterService sseEmitterService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    private void autenticarComo(String userId) {
        User usuario = new User(null, "Cliente", "cliente@teste.com", "41999999999", "12345678900", "senha", UserTypeEnum.CUSTOMER);
        usuario.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU08: o cliente assina o próprio canal customer:{id} e recebe event-stream")
    void devePermitirAssinarCanalDoProprioCliente() throws Exception {
        autenticarComo("cliente-1");
        SseEmitter emitter = new SseEmitter(5000L);
        when(sseEmitterService.subscribe(eq("customer:cliente-1"))).thenReturn(emitter);

        MvcResult result = mockMvc.perform(get("/api/sse/subscribe/{channel}", "customer:cliente-1")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("status").data("ok"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    @DisplayName("HU08: assinar o canal de outro cliente é 403 e não abre o stream")
    void deveRetornar403AoAssinarCanalDeOutroCliente() throws Exception {
        autenticarComo("cliente-1");

        mockMvc.perform(get("/api/sse/subscribe/{channel}", "customer:cliente-2")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isForbidden());

        verify(sseEmitterService, never()).subscribe(any());
    }

    @Test
    @DisplayName("HU08: canal fora da convenção customer:{id} é 403")
    void deveRetornar403ParaCanalForaDaConvencao() throws Exception {
        autenticarComo("cliente-1");

        mockMvc.perform(get("/api/sse/subscribe/{channel}", "order:123")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isForbidden());

        verify(sseEmitterService, never()).subscribe(any());
    }

    // ---- HU27: canal da loja ----
    private void autenticarComoLoja(String userId) {
        User loja = new User(null, "Loja", "loja@teste.com", "41999999999", "12345678901", "senha", UserTypeEnum.SUPPLIER);
        loja.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                loja, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
    }

    @Test
    @DisplayName("HU27: a loja assina o próprio canal supplier:{id}")
    void deveDeixarALojaAssinarOProprioCanal() throws Exception {
        autenticarComoLoja("loja-1");
        when(sseEmitterService.subscribe(eq("supplier:loja-1"))).thenReturn(new org.springframework.web.servlet.mvc.method.annotation.SseEmitter());

        mockMvc.perform(get("/api/sse/subscribe/{channel}", "supplier:loja-1").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());

        verify(sseEmitterService).subscribe("supplier:loja-1");
    }

    @Test
    @DisplayName("HU27: assinar o canal de outra loja é 403")
    void deveNegarCanalDeOutraLoja() throws Exception {
        autenticarComoLoja("loja-1");

        mockMvc.perform(get("/api/sse/subscribe/{channel}", "supplier:loja-2").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isForbidden());

        verify(sseEmitterService, never()).subscribe(any());
    }
}
