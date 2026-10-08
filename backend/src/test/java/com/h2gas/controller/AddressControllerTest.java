package com.h2gas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.AddressMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.AddressService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private AddressMapper addressMapper;

    private User usuarioLogado;

    @BeforeEach
    void setUp() {
        usuarioLogado = new User(null, "Cliente Logado", "logado@teste.com",
                "41999999999", "12345678901", "senha", UserTypeEnum.CUSTOMER);
        usuarioLogado.setId("user-logado-123");

        var authToken = new UsernamePasswordAuthenticationToken(
                usuarioLogado, null, usuarioLogado.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @Test
    void deveIgnorarUserIdDoCorpoEUsarOUsuarioAutenticado() throws Exception {
        AddressDTO payload = new AddressDTO();
        payload.setStreetName("Rua Teste");
        payload.setNumber("100");
        payload.setCityId(1L);
        payload.setUserId("user-de-outra-pessoa");

        when(addressService.createAddress(any(AddressDTO.class))).thenReturn(payload);

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(addressService).createAddress(argThat(dto ->
                "user-logado-123".equals(dto.getUserId())
        ));
    }
}
