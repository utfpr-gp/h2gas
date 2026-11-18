package com.h2gas.configuration.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class SecurityConfigurationsSwaggerRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void naoDeveExporRotaDeNegocioSemToken() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveManterLoginPublicoAposMudancaDeSeguranca() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"naoexiste@teste.com\",\"password\":\"errada\"}"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus(),
                        "Login não pode ser bloqueado pelo filtro de segurança (permitAll deve continuar valendo)"));
    }
}
