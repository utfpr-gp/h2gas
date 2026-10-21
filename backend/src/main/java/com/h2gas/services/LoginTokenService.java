package com.h2gas.services;

import com.h2gas.configuration.security.TokenService;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.exceptions.GlobalExceptionHandler;
import com.h2gas.model.entity.LoginToken;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.LoginTokenRepository;
import com.h2gas.model.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

// HU11: login por código de 6 dígitos enviado por e-mail (sem senha)
@Service
public class LoginTokenService {
    private static final Logger log = LoggerFactory.getLogger(LoginTokenService.class);
    private static final String INVALID_CODE = "Código inválido.";
    private static final String EXHAUSTED_CODE = "Código expirado ou esgotado. Solicite um novo.";

    private final LoginTokenRepository loginTokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final TokenService tokenService;
    private final Environment environment;
    private final SecureRandom random = new SecureRandom();

    public LoginTokenService(LoginTokenRepository loginTokenRepository, UserRepository userRepository,
                             MailService mailService, TokenService tokenService, Environment environment) {
        this.loginTokenRepository = loginTokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.tokenService = tokenService;
        this.environment = environment;
    }

    // responde sempre igual: e-mail inexistente não gera código nem revela a ausência da conta.
    // Sem transação envolvendo o método: o save confirma sozinho e o envio do e-mail (I/O externo,
    // que pode demorar segundos para falhar) acontece com o código já gravado.
    public void requestToken(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty() || !userOptional.get().isEnabled()) {
            log.info("Pedido de código para e-mail não cadastrado ou conta desativada; nada é enviado.");
            return;
        }

        User user = userOptional.get();
        String code = String.format("%06d", random.nextInt(1_000_000));
        LoginToken token = loginTokenRepository.findByUserId(user.getId())
                .map(existing -> {
                    existing.refresh(code);
                    return existing;
                })
                .orElseGet(() -> new LoginToken(code, user));
        loginTokenRepository.save(token);

        sendCode(user, code);
    }

    // sem transação única de propósito: a contagem de tentativas precisa ficar gravada mesmo
    // quando a validação termina em exceção (cada save do repositório confirma por si)
    public String validateAndLogin(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException(INVALID_CODE));
        LoginToken token = loginTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessRuleException(INVALID_CODE));

        if (token.isExpiredOrExhausted()) {
            throw new BusinessRuleException(EXHAUSTED_CODE);
        }
        if (!token.getCode().equals(code)) {
            token.setAttempts(token.getAttempts() + 1);
            loginTokenRepository.save(token);
            throw new BusinessRuleException(INVALID_CODE);
        }

        token.setUsed(true);
        loginTokenRepository.save(token);
        if (!user.isEnabled()) {
            throw new ForbiddenResourceException(GlobalExceptionHandler.DISABLED_ACCOUNT);
        }
        return tokenService.generateToken(user);
    }

    // SMTP indisponível não pode derrubar o pedido: o código já está gravado e pode ser reenviado
    private void sendCode(User user, String code) {
        if (!environment.matchesProfiles("prod")) {
            log.info("HU11 (ambiente de desenvolvimento): código de acesso de {} é {}", user.getEmail(), code);
        }
        try {
            mailService.sendMail(user.getEmail(), "Seu código de acesso H2Gás",
                    "Seu código de acesso é: " + code + ". Válido por 5 minutos.");
        } catch (Exception e) {
            log.error("Não foi possível enviar o código de acesso por e-mail", e);
        }
    }
}
