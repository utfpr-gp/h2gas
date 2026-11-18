package com.h2gas.controller;

import com.h2gas.configuration.security.SecurityConfigurations;
import com.h2gas.configuration.security.SecurityFilter;
import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AdminCreateUserDto;
import com.h2gas.model.dto.AdminUserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.AdminUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfigurations.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    private static final String NOVO_ADMIN = """
            {"name": "Ana Admin", "email": "ana@teste.com", "password": "123456",
             "phoneNumber": "41988887777", "userType": "ADMIN"}""";

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private AdminUserService adminUserService;

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
    @DisplayName("HU12: rotas /api/admin/users são só do ADMIN (403 para os outros perfis)")
    void deveRetornar403ParaNaoAdmin() throws Exception {
        autenticarComo(UserTypeEnum.SUPPLIER);

        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(NOVO_ADMIN))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/users/u-1/enabled").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("HU12: lista paginada com filtro de perfil e busca")
    void deveListarUsuariosPaginados() throws Exception {
        autenticarComo(UserTypeEnum.ADMIN);
        Page<AdminUserDto> pagina = new PageImpl<>(List.of(
                new AdminUserDto("u-1", "Maria", "maria@teste.com", "41911111111", UserTypeEnum.CUSTOMER, true)));
        when(adminUserService.listUsers(eq(UserTypeEnum.CUSTOMER), eq("mar"), any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/api/admin/users").param("type", "CUSTOMER").param("q", "mar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Maria"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("HU12: criar usuário responde 201 com o usuário criado")
    void deveRetornar201AoCriarUsuario() throws Exception {
        autenticarComo(UserTypeEnum.ADMIN);
        when(adminUserService.createUser(any(AdminCreateUserDto.class))).thenReturn(
                new AdminUserDto("u-2", "Ana Admin", "ana@teste.com", "41988887777", UserTypeEnum.ADMIN, true));

        mockMvc.perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(NOVO_ADMIN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("u-2"))
                .andExpect(jsonPath("$.userType").value("ADMIN"));
    }

    @Test
    @DisplayName("HU12: corpo inválido na criação é 400 e não chega ao serviço")
    void deveRetornar400ComDadosInvalidos() throws Exception {
        autenticarComo(UserTypeEnum.ADMIN);

        mockMvc.perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"nao-e-email\", \"password\": \"1\"}"))
                .andExpect(status().isBadRequest());

        verify(adminUserService, never()).createUser(any());
    }

    @Test
    @DisplayName("HU12: desativar responde 204 e usa o admin logado como autor")
    void deveRetornar204AoDesativar() throws Exception {
        User admin = autenticarComo(UserTypeEnum.ADMIN);

        mockMvc.perform(patch("/api/admin/users/u-1/enabled").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isNoContent());

        verify(adminUserService).setEnabled(eq("u-1"), eq(false), eq(admin));
    }
}
