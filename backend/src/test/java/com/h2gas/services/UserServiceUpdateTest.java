package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateTest {

    @InjectMocks private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    private User maria;
    private User admin;

    private User usuario(String id, String nome, String email, UserTypeEnum tipo) {
        User u = new User(null, nome, email, "41999999999", "11111111111", "senha", tipo);
        u.setId(id);
        return u;
    }

    private UserDto dto(String nome, String email, String telefone) {
        UserDto d = new UserDto();
        d.setName(nome);
        d.setEmail(email);
        d.setPhoneNumber(telefone);
        return d;
    }

    @BeforeEach
    void setUp() {
        maria = usuario("u-maria", "Maria Souza", "maria@h2gas.local", UserTypeEnum.CUSTOMER);
        admin = usuario("u-admin", "Admin", "admin@h2gas.local", UserTypeEnum.ADMIN);
        lenient().when(userRepository.findById("u-maria")).thenReturn(Optional.of(maria));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(userMapper.toDto(any(User.class))).thenReturn(new UserDto());
    }

    @Test
    @DisplayName("F7: o usuário atualiza o próprio nome e telefone")
    void deveAtualizarNomeETelefoneDoProprioUsuario() {
        userService.update("u-maria", dto("Maria S. Lima", "maria@h2gas.local", "41988887777"), maria);

        assertEquals("Maria S. Lima", maria.getName());
        assertEquals("41988887777", maria.getPhoneNumber());
        verify(userRepository).save(maria);
    }

    @Test
    @DisplayName("F7: ninguém edita os dados de outro usuário (403)")
    void deveRejeitarEdicaoDeOutroUsuario() {
        User intruso = usuario("u-intruso", "Intruso", "intruso@h2gas.local", UserTypeEnum.CUSTOMER);

        assertThrows(ForbiddenResourceException.class,
                () -> userService.update("u-maria", dto("Hackeada", "intruso@h2gas.local", "000"), intruso));

        assertEquals("Maria Souza", maria.getName());
        assertEquals("maria@h2gas.local", maria.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("F7: ADMIN pode editar qualquer usuário")
    void devePermitirAdminEditarQualquerUsuario() {
        userService.update("u-maria", dto("Maria Admin", "maria@h2gas.local", "41977776666"), admin);

        assertEquals("Maria Admin", maria.getName());
        verify(userRepository).save(maria);
    }

    @Test
    @DisplayName("F7: e-mail ausente ou igual ao atual é mantido")
    void deveManterOEmailQuandoOCampoVierIgualOuAusente() {
        userService.update("u-maria", dto("Maria", null, "41988887777"), maria);
        assertEquals("maria@h2gas.local", maria.getEmail());

        userService.update("u-maria", dto("Maria", "maria@h2gas.local", "41988887777"), maria);
        assertEquals("maria@h2gas.local", maria.getEmail());
    }

    @Test
    @DisplayName("F7: usuário comum não troca o e-mail (identidade do login por código)")
    void deveRejeitarTrocaDeEmailPorUsuarioComum() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> userService.update("u-maria", dto("Maria", "outro@h2gas.local", "41988887777"), maria));

        assertEquals("O e-mail não pode ser alterado por aqui.", erro.getMessage());
        assertEquals("maria@h2gas.local", maria.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("F7: ADMIN troca o e-mail respeitando a unicidade")
    void devePermitirAdminTrocarEmailRespeitandoUnicidade() {
        userService.update("u-maria", dto("Maria", "novo@h2gas.local", "41988887777"), admin);
        assertEquals("novo@h2gas.local", maria.getEmail());

        when(userRepository.existsByEmail("ocupado@h2gas.local")).thenReturn(true);
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> userService.update("u-maria", dto("Maria", "ocupado@h2gas.local", "41988887777"), admin));
        assertEquals("E-mail já cadastrado.", erro.getMessage());
    }

    @Test
    @DisplayName("HU12: o ADMIN não troca o próprio e-mail pelo PUT (só com o código em Meus Dados)")
    void naoDeveTrocarOProprioEmailDoAdminPeloPut() {
        when(userRepository.findById("u-admin")).thenReturn(Optional.of(admin));

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> userService.update("u-admin", dto("Admin", "outro.admin@h2gas.local", "41999999999"), admin));

        assertEquals("Para trocar o seu e-mail, confirme pelo código em Meus Dados.", erro.getMessage());
        assertEquals("admin@h2gas.local", admin.getEmail());
        verify(userRepository, never()).save(any(User.class));

        userService.update("u-admin", dto("Admin Novo", "admin@h2gas.local", "41999999999"), admin);
        assertEquals("Admin Novo", admin.getName());
    }
}
