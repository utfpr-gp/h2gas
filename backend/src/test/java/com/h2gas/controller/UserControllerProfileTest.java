package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.MailService;
import com.h2gas.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerProfileTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserService userService;
    @MockitoBean private UserMapper userMapper;
    @MockitoBean private MailService mailService;

    private static final String CORPO = "{\"name\":\"Maria Souza\",\"email\":\"maria@h2gas.local\",\"phoneNumber\":\"41988887777\"}";

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private User autenticarComo(UserTypeEnum tipo, String id) {
        User usuario = new User(null, "Usuário", tipo.name().toLowerCase() + "@teste.com", "41999999999",
                "12345678900", "senha", tipo);
        usuario.setId(id);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + tipo.name()))));
        return usuario;
    }

    @Test
    @DisplayName("F7: o PUT repassa o usuário do token ao service, que decide o dono")
    void deveRetornar200AoEditarOProprioPerfil() throws Exception {
        User cliente = autenticarComo(UserTypeEnum.CUSTOMER, "u-maria");
        when(userService.update(eq("u-maria"), any(UserDto.class), same(cliente))).thenReturn(new UserDto());

        mockMvc.perform(put("/api/users/u-maria").contentType("application/json").content(CORPO))
                .andExpect(status().isOk());

        verify(userService).update(eq("u-maria"), any(UserDto.class), same(cliente));
    }

    @Test
    @DisplayName("F7: editar outro usuário devolve 403")
    void deveRetornar403AoEditarOutroUsuario() throws Exception {
        autenticarComo(UserTypeEnum.CUSTOMER, "u-intruso");
        when(userService.update(eq("u-maria"), any(UserDto.class), any(User.class)))
                .thenThrow(new ForbiddenResourceException("Você só pode editar os seus próprios dados."));

        mockMvc.perform(put("/api/users/u-maria").contentType("application/json").content(CORPO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F7: o entregador também edita o próprio perfil")
    void devePermitirEntregadorEditarOProprioPerfil() throws Exception {
        User entregador = autenticarComo(UserTypeEnum.DELIVERER, "u-carlos");
        when(userService.update(eq("u-carlos"), any(UserDto.class), same(entregador))).thenReturn(new UserDto());

        mockMvc.perform(put("/api/users/u-carlos").contentType("application/json").content(CORPO))
                .andExpect(status().isOk());
    }
}
