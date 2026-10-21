package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.CityService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CityController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CityService cityService;

    @MockitoBean
    private CityMapper cityMapper;

    @BeforeEach
    void autenticarComoAdmin() {
        User admin = new User(null, "Admin", "admin@teste.com", "41999999999", "12345678900", "senha", UserTypeEnum.ADMIN);
        admin.setId("admin-1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRetornar201AoCadastrarCidade() throws Exception {
        when(cityService.createCity(4113700)).thenReturn(new CityDTO(7L, "Londrina", "PR", 4113700));

        mockMvc.perform(post("/api/cities").contentType(MediaType.APPLICATION_JSON).content("{\"ibgeCode\": 4113700}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Londrina"))
                .andExpect(jsonPath("$.ibgeCode").value(4113700));
    }

    @Test
    void deveRetornar400SemCodigoIbge() throws Exception {
        mockMvc.perform(post("/api/cities").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Curitba\", \"state\": \"PR\"}"))
                .andExpect(status().isBadRequest());

        verify(cityService, never()).createCity(anyInt());
    }
}
