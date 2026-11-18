package com.h2gas.services;

import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.entity.EmailChangeToken;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.EmailChangeTokenRepository;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmailChangeServiceTest {

    @Autowired private EmailChangeService emailChangeService;
    @Autowired private EmailChangeTokenRepository emailChangeTokenRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean private MailService mailService;
    @MockitoBean private TokenService tokenService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(new User(null, "Admin", "admin.hu12@teste.com", "41999999999",
                null, "senha", UserTypeEnum.ADMIN));
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-novo");
    }

    private EmailChangeToken pedido() {
        return emailChangeTokenRepository.findByUserId(admin.getId()).orElseThrow();
    }

    @Test
    @DisplayName("HU12: o código vai para o e-mail novo, e o e-mail atual não muda ainda")
    void deveEnviarCodigoParaONovoEmail() {
        emailChangeService.requestChange(admin, "  novo.admin@teste.com ");

        EmailChangeToken token = pedido();
        assertEquals("novo.admin@teste.com", token.getNewEmail());
        assertTrue(token.getCode().matches("\\d{6}"));
        verify(mailService).sendMail(eq("novo.admin@teste.com"), anyString(), contains(token.getCode()));
        assertEquals("admin.hu12@teste.com", userRepository.findById(admin.getId()).orElseThrow().getEmail());
    }

    @Test
    @DisplayName("HU12: e-mail de outra conta ou igual ao atual é recusado, sem enviar código")
    void deveRecusarEmailJaCadastradoOuIgualAoAtual() {
        userRepository.save(new User(null, "Outro", "ocupado.hu12@teste.com", "41988888888", null, "senha",
                UserTypeEnum.CUSTOMER));

        BusinessRuleException ocupado = assertThrows(BusinessRuleException.class,
                () -> emailChangeService.requestChange(admin, "OCUPADO.hu12@teste.com"));
        assertEquals("E-mail já cadastrado.", ocupado.getMessage());

        BusinessRuleException igual = assertThrows(BusinessRuleException.class,
                () -> emailChangeService.requestChange(admin, "admin.hu12@teste.com"));
        assertEquals("Esse já é o seu e-mail.", igual.getMessage());

        verify(mailService, never()).sendMail(anyString(), anyString(), anyString());
        assertTrue(emailChangeTokenRepository.findByUserId(admin.getId()).isEmpty());
    }

    @Test
    @DisplayName("HU12: código certo troca o e-mail, marca o código como usado e devolve token novo")
    void deveTrocarEmailEDevolverTokenComCodigoCerto() {
        emailChangeService.requestChange(admin, "novo.admin@teste.com");

        String jwt = emailChangeService.confirmChange(admin, pedido().getCode());

        assertEquals("jwt-novo", jwt);
        User salvo = userRepository.findById(admin.getId()).orElseThrow();
        assertEquals("novo.admin@teste.com", salvo.getEmail());
        verify(tokenService).generateToken(argThat(u -> "novo.admin@teste.com".equals(u.getEmail())));
        assertTrue(pedido().isUsed());
    }

    @Test
    @DisplayName("HU12: código errado é recusado e conta a tentativa")
    void deveContarTentativaComCodigoErrado() {
        emailChangeService.requestChange(admin, "novo.admin@teste.com");
        String errado = pedido().getCode().equals("000000") ? "111111" : "000000";

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> emailChangeService.confirmChange(admin, errado));

        assertEquals("Código inválido.", erro.getMessage());
        assertEquals(1, pedido().getAttempts());
        assertEquals("admin.hu12@teste.com", userRepository.findById(admin.getId()).orElseThrow().getEmail());
    }

    @Test
    @DisplayName("HU12: código expirado, esgotado ou já usado é recusado")
    void deveRecusarCodigoExpiradoOuEsgotado() {
        emailChangeService.requestChange(admin, "novo.admin@teste.com");
        EmailChangeToken token = pedido();
        token.setExpiryDate(new Date(System.currentTimeMillis() - 1000));
        emailChangeTokenRepository.save(token);

        BusinessRuleException expirado = assertThrows(BusinessRuleException.class,
                () -> emailChangeService.confirmChange(admin, token.getCode()));
        assertEquals("Código expirado ou esgotado. Solicite um novo.", expirado.getMessage());

        token.setExpiryDate(new Date(System.currentTimeMillis() + 60_000));
        token.setAttempts(EmailChangeToken.MAX_ATTEMPTS);
        emailChangeTokenRepository.save(token);
        assertThrows(BusinessRuleException.class, () -> emailChangeService.confirmChange(admin, token.getCode()));

        BusinessRuleException semPedido = assertThrows(BusinessRuleException.class,
                () -> emailChangeService.confirmChange(userRepository.save(new User(null, "Sem", "sem.hu12@teste.com",
                        "41977777777", null, "senha", UserTypeEnum.ADMIN)), "123456"));
        assertEquals("Código inválido.", semPedido.getMessage());
    }

    @Test
    @DisplayName("HU12: se outra conta ocupou o e-mail antes da confirmação, a troca é recusada")
    void deveRecusarSeOEmailFoiOcupadoAntesDeConfirmar() {
        emailChangeService.requestChange(admin, "disputado.hu12@teste.com");
        userRepository.save(new User(null, "Rápido", "disputado.hu12@teste.com", "41966666666", null, "senha",
                UserTypeEnum.CUSTOMER));

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> emailChangeService.confirmChange(admin, pedido().getCode()));

        assertEquals("E-mail já cadastrado.", erro.getMessage());
        assertEquals("admin.hu12@teste.com", userRepository.findById(admin.getId()).orElseThrow().getEmail());
    }
}
