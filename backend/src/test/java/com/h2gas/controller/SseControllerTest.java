package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.sse.SseEmitterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    @DisplayName("Deve retornar content type event-stream ao inscrever no canal")
    void deveRetornarContentTypeEventStream() throws Exception {
        SseEmitter emitter = new SseEmitter(5000L);
        when(sseEmitterService.subscribe(eq("order:123"))).thenReturn(emitter);

        MvcResult result = mockMvc.perform(get("/api/sse/subscribe/{channel}", "order:123")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("status").data("ok"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
