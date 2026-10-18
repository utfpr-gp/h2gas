package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.DelivererCreateDTO;
import com.h2gas.model.dto.DelivererDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.DelivererService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DelivererController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class DelivererControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private DelivererService delivererService;

    @BeforeEach
    void autenticarComoFornecedor() {
        User fornecedor = new User(null, "Gás Centro", "gas@teste.com", "41999999999", "12345678900", "senha", UserTypeEnum.SUPPLIER);
        fornecedor.setId("fornecedor-1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                fornecedor, null, List.of(new SimpleGrantedAuthority("ROLE_SUPPLIER"))));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornar400ComDadosInvalidos() throws Exception {
        mockMvc.perform(post("/api/deliverers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"nao-e-email\", \"phoneNumber\": \"\", \"password\": \"123\"}"))
                .andExpect(status().isBadRequest());

        verify(delivererService, never()).createDeliverer(any(), any());
    }

    @Test
    void deveRetornar200AoCadastrarEntregadorDoFornecedorLogado() throws Exception {
        when(delivererService.createDeliverer(any(DelivererCreateDTO.class), eq("fornecedor-1")))
                .thenReturn(new DelivererDTO(7L, "Carlos Silva", "carlos@teste.com", "41988887777", "Moto - ABC-1234", true, false));

        mockMvc.perform(post("/api/deliverers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Carlos Silva\", \"email\": \"carlos@teste.com\", \"phoneNumber\": \"41988887777\","
                                + " \"password\": \"senha123\", \"vehicleInfo\": \"Moto - ABC-1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.busy").value(false));
    }

    @Test
    void deveListarFrotaDoFornecedorLogado() throws Exception {
        when(delivererService.findMine("fornecedor-1")).thenReturn(List.of(
                new DelivererDTO(7L, "Carlos Silva", "carlos@teste.com", "41988887777", "Moto", true, true)));

        mockMvc.perform(get("/api/deliverers/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Carlos Silva"))
                .andExpect(jsonPath("$[0].busy").value(true));
    }
}
