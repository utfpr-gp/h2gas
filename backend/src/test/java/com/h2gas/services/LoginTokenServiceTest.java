package com.h2gas.services;

import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.model.entity.LoginToken;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.LoginTokenRepository;
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
class LoginTokenServiceTest {

    @Autowired private LoginTokenService loginTokenService;
    @Autowired private LoginTokenRepository loginTokenRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean private MailService mailService;
    @MockitoBean private TokenService tokenService;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = userRepository.save(new User(null, "Cliente", "cliente.hu11@teste.com", "41999999999",
                "11111111111", "senha", UserTypeEnum.CUSTOMER));
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-de-teste");
    }

    private LoginToken tokenSalvo() {
        return loginTokenRepository.findByUserId(usuario.getId()).orElseThrow();
    }

    @Test
    @DisplayName("HU11: gera código de 6 dígitos e envia por e-mail")
    void deveGerarCodigoDeSeisDigitosEEnviarPorEmail() {
        loginTokenService.requestToken(usuario.getEmail());

        LoginToken token = tokenSalvo();
        assertTrue(token.getCode().matches("\\d{6}"), "código deve ter 6 dígitos: " + token.getCode());
        verify(mailService).sendMail(eq(usuario.getEmail()), anyString(), contains(token.getCode()));
    }

    @Test
    @DisplayName("HU11: e-mail inexistente não gera código, não envia e-mail e não dá erro")
    void naoDeveRevelarQuandoEmailNaoExiste() {
        assertDoesNotThrow(() -> loginTokenService.requestToken("nao.existe@teste.com"));

        verify(mailService, never()).sendMail(anyString(), anyString(), anyString());
        assertEquals(0, loginTokenRepository.count());
    }

    @Test
    @DisplayName("HU11: pedir de novo substitui o código anterior, zerando uso e tentativas")
    void deveSubstituirOCodigoAnteriorAoSolicitarNovamente() {
        loginTokenService.requestToken(usuario.getEmail());
        LoginToken primeiro = tokenSalvo();
        String codigoAntigo = primeiro.getCode();
        primeiro.setAttempts(3);
        primeiro.setUsed(true);
        loginTokenRepository.save(primeiro);

        loginTokenService.requestToken(usuario.getEmail());

        LoginToken atual = tokenSalvo();
        assertEquals(1, loginTokenRepository.count(), "deve haver um único código por usuário");
        assertNotEquals(codigoAntigo, atual.getCode());
        assertEquals(0, atual.getAttempts());
        assertFalse(atual.isUsed());
    }

    @Test
    @DisplayName("HU11: falha no envio do e-mail não derruba a solicitação (o código fica gravado)")
    void naoDeveFalharQuandoOEnvioDeEmailDerErro() {
        doThrow(new RuntimeException("SMTP fora do ar")).when(mailService).sendMail(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> loginTokenService.requestToken(usuario.getEmail()));

        assertTrue(tokenSalvo().getCode().matches("\\d{6}"));
    }

    @Test
    @DisplayName("HU11: código correto dentro do prazo emite o JWT e marca o código como usado")
    void deveValidarCodigoCorretoDentroDoPrazoEEmitirJwt() {
        loginTokenService.requestToken(usuario.getEmail());
        String codigo = tokenSalvo().getCode();

        String jwt = loginTokenService.validateAndLogin(usuario.getEmail(), codigo);

        assertEquals("jwt-de-teste", jwt);
        assertTrue(tokenSalvo().isUsed());
        verify(tokenService).generateToken(argThat(u -> usuario.getId().equals(u.getId())));
    }

    @Test
    @DisplayName("HU12: código certo de conta desativada não emite JWT (403)")
    void deveRecusarLoginPorCodigoDeContaDesativada() {
        loginTokenService.requestToken(usuario.getEmail());
        String codigo = tokenSalvo().getCode();
        usuario.setEnabled(false);
        userRepository.save(usuario);

        ForbiddenResourceException erro = assertThrows(ForbiddenResourceException.class,
                () -> loginTokenService.validateAndLogin(usuario.getEmail(), codigo));

        assertEquals("Conta desativada. Fale com o suporte.", erro.getMessage());
        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    @DisplayName("HU12: conta desativada não recebe código por e-mail (e a resposta segue igual)")
    void naoDeveEnviarCodigoParaContaDesativada() {
        usuario.setEnabled(false);
        userRepository.save(usuario);

        assertDoesNotThrow(() -> loginTokenService.requestToken(usuario.getEmail()));

        verify(mailService, never()).sendMail(anyString(), anyString(), anyString());
        assertEquals(0, loginTokenRepository.count());
    }

    @Test
    @DisplayName("HU11: código expirado é recusado")
    void deveRejeitarCodigoExpirado() {
        loginTokenService.requestToken(usuario.getEmail());
        LoginToken token = tokenSalvo();
        token.setExpiryDate(new Date(System.currentTimeMillis() - 1_000));
        loginTokenRepository.save(token);

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> loginTokenService.validateAndLogin(usuario.getEmail(), token.getCode()));
        assertTrue(erro.getMessage().contains("expirado"), erro.getMessage());
    }

    @Test
    @DisplayName("HU11: código já utilizado não vale de novo")
    void deveRejeitarCodigoJaUtilizado() {
        loginTokenService.requestToken(usuario.getEmail());
        String codigo = tokenSalvo().getCode();
        loginTokenService.validateAndLogin(usuario.getEmail(), codigo);

        assertThrows(BusinessRuleException.class,
                () -> loginTokenService.validateAndLogin(usuario.getEmail(), codigo));
    }

    @Test
    @DisplayName("HU11: conta as tentativas erradas e bloqueia o código depois do máximo")
    void deveContarTentativasEBloquearAposOMaximo() {
        loginTokenService.requestToken(usuario.getEmail());
        String codigoCerto = tokenSalvo().getCode();
        String codigoErrado = codigoCerto.equals("000000") ? "999999" : "000000";

        for (int tentativa = 1; tentativa <= LoginToken.MAX_ATTEMPTS; tentativa++) {
            BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                    () -> loginTokenService.validateAndLogin(usuario.getEmail(), codigoErrado));
            assertEquals("Código inválido.", erro.getMessage());
        }
        assertEquals(LoginToken.MAX_ATTEMPTS, tokenSalvo().getAttempts());

        BusinessRuleException bloqueado = assertThrows(BusinessRuleException.class,
                () -> loginTokenService.validateAndLogin(usuario.getEmail(), codigoCerto));
        assertTrue(bloqueado.getMessage().contains("esgotado"), bloqueado.getMessage());
    }

    @Test
    @DisplayName("HU11: e-mail inexistente na validação dá a mesma mensagem de código inválido")
    void deveRejeitarComMensagemGenericaQuandoEmailNaoExiste() {
        BusinessRuleException semUsuario = assertThrows(BusinessRuleException.class,
                () -> loginTokenService.validateAndLogin("nao.existe@teste.com", "123456"));
        assertEquals("Código inválido.", semUsuario.getMessage());

        BusinessRuleException semCodigo = assertThrows(BusinessRuleException.class,
                () -> loginTokenService.validateAndLogin(usuario.getEmail(), "123456"));
        assertEquals("Código inválido.", semCodigo.getMessage());
    }
}
