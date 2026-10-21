package com.h2gas.services;

import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.entity.LoginToken;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.LoginTokenRepository;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * HU11: sem @Transactional no teste, para que cada chamada confirme de verdade.
 * O incremento de tentativas precisa sobreviver à exceção de código inválido —
 * dentro de uma transação única o rollback apagaria a contagem.
 */
@SpringBootTest
@ActiveProfiles("test")
class LoginTokenServiceAttemptsTest {

    @Autowired private LoginTokenService loginTokenService;
    @Autowired private LoginTokenRepository loginTokenRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean private MailService mailService;
    @MockitoBean private TokenService tokenService;

    private User usuario;

    @BeforeEach
    void setUp() {
        loginTokenRepository.deleteAll();
        usuario = userRepository.save(new User(null, "Cliente", "tentativas.hu11@teste.com", "41999999999",
                "99999999911", "senha", UserTypeEnum.CUSTOMER));
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-de-teste");
    }

    @AfterEach
    void tearDown() {
        loginTokenRepository.deleteAll();
        userRepository.delete(usuario);
    }

    @Test
    @DisplayName("HU11: tentativas erradas ficam gravadas e bloqueiam o código mesmo com commit real")
    void deveGravarTentativasMesmoComRollbackDaExcecao() {
        loginTokenService.requestToken(usuario.getEmail());
        String codigoCerto = loginTokenRepository.findByUserId(usuario.getId()).orElseThrow().getCode();
        String codigoErrado = codigoCerto.equals("000000") ? "999999" : "000000";

        assertThrows(BusinessRuleException.class,
                () -> loginTokenService.validateAndLogin(usuario.getEmail(), codigoErrado));

        assertEquals(1, loginTokenRepository.findByUserId(usuario.getId()).orElseThrow().getAttempts(),
                "a tentativa errada precisa continuar contada depois da exceção");

        for (int i = 2; i <= LoginToken.MAX_ATTEMPTS; i++) {
            assertThrows(BusinessRuleException.class,
                    () -> loginTokenService.validateAndLogin(usuario.getEmail(), codigoErrado));
        }
        assertEquals(LoginToken.MAX_ATTEMPTS, loginTokenRepository.findByUserId(usuario.getId()).orElseThrow().getAttempts());

        BusinessRuleException bloqueado = assertThrows(BusinessRuleException.class,
                () -> loginTokenService.validateAndLogin(usuario.getEmail(), codigoCerto));
        assertTrue(bloqueado.getMessage().contains("esgotado"), bloqueado.getMessage());
    }

    @Test
    @DisplayName("HU11: o envio do e-mail acontece fora da transação (I/O lento não segura o banco)")
    void naoDeveSegurarTransacaoDuranteOEnvioDoEmail() {
        boolean[] transacaoAbertaNoEnvio = new boolean[1];
        doAnswer(invocation -> {
            transacaoAbertaNoEnvio[0] = TransactionSynchronizationManager.isActualTransactionActive();
            throw new RuntimeException("SMTP lento/indisponível");
        }).when(mailService).sendMail(anyString(), anyString(), anyString());

        loginTokenService.requestToken(usuario.getEmail());

        assertFalse(transacaoAbertaNoEnvio[0],
                "o código já deve estar gravado e a transação encerrada quando o e-mail é enviado");
        assertTrue(loginTokenRepository.findByUserId(usuario.getId()).orElseThrow().getCode().matches("\\d{6}"));
    }

    @Test
    @DisplayName("HU11: pedir de novo troca o código já gravado, mesmo com o envio falhando")
    void deveTrocarOCodigoGravadoAoPedirDeNovo() {
        doThrow(new RuntimeException("SMTP fora do ar")).when(mailService).sendMail(anyString(), anyString(), anyString());
        loginTokenService.requestToken(usuario.getEmail());
        String primeiro = loginTokenRepository.findByUserId(usuario.getId()).orElseThrow().getCode();

        loginTokenService.requestToken(usuario.getEmail());

        assertNotEquals(primeiro, loginTokenRepository.findByUserId(usuario.getId()).orElseThrow().getCode());
    }
}
