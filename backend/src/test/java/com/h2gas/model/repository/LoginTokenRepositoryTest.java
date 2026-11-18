package com.h2gas.model.repository;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.LoginToken;
import com.h2gas.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class LoginTokenRepositoryTest {

    @Autowired private LoginTokenRepository loginTokenRepository;
    @Autowired private UserRepository userRepository;

    private User novoUsuario(String email, String cpf) {
        return userRepository.saveAndFlush(new User(null, "Usuário", email, "41999999999", cpf, "senha", UserTypeEnum.CUSTOMER));
    }

    @Test
    @DisplayName("HU11: guarda o código do usuário e o encontra pelo id dele")
    void deveSalvarEBuscarOCodigoPeloUsuario() {
        User usuario = novoUsuario("codigo@h2gas.test", "11111111111");
        User outro = novoUsuario("outro@h2gas.test", "22222222222");
        loginTokenRepository.saveAndFlush(new LoginToken("123456", usuario));

        LoginToken salvo = loginTokenRepository.findByUserId(usuario.getId()).orElseThrow();

        assertEquals("123456", salvo.getCode());
        assertFalse(salvo.isUsed());
        assertEquals(0, salvo.getAttempts());
        assertTrue(salvo.getExpiryDate().after(new Date()));
        assertTrue(loginTokenRepository.findByUserId(outro.getId()).isEmpty());
    }

    @Test
    @DisplayName("HU11: o código expira em 5 minutos e refresh renova prazo, uso e tentativas")
    void deveRenovarPrazoUsoETentativasNoRefresh() {
        User usuario = novoUsuario("refresh@h2gas.test", "33333333333");
        LoginToken token = new LoginToken("111111", usuario);
        token.setUsed(true);
        token.setAttempts(4);
        loginTokenRepository.saveAndFlush(token);

        token.refresh("222222");
        loginTokenRepository.saveAndFlush(token);

        LoginToken salvo = loginTokenRepository.findByUserId(usuario.getId()).orElseThrow();
        assertEquals("222222", salvo.getCode());
        assertFalse(salvo.isUsed());
        assertEquals(0, salvo.getAttempts());
        assertFalse(salvo.isExpiredOrExhausted());
        long faltam = salvo.getExpiryDate().getTime() - System.currentTimeMillis();
        assertTrue(faltam > 4 * 60_000 && faltam <= 5 * 60_000, "prazo deve ser de ~5 minutos, mas faltam " + faltam + " ms");
    }

    @Test
    @DisplayName("HU11: código usado, expirado ou com tentativas esgotadas não vale mais")
    void deveMarcarCodigoComoInvalidoQuandoUsadoExpiradoOuEsgotado() {
        User usuario = novoUsuario("invalido@h2gas.test", "44444444444");
        LoginToken token = new LoginToken("555555", usuario);
        assertFalse(token.isExpiredOrExhausted());

        token.setUsed(true);
        assertTrue(token.isExpiredOrExhausted(), "código usado não vale");

        token.refresh("666666");
        token.setAttempts(5);
        assertTrue(token.isExpiredOrExhausted(), "tentativas esgotadas não valem");

        token.refresh("777777");
        token.setExpiryDate(new Date(System.currentTimeMillis() - 1_000));
        assertTrue(token.isExpiredOrExhausted(), "código expirado não vale");
    }
}
