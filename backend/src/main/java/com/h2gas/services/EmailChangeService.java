package com.h2gas.services;

import com.h2gas.configuration.security.TokenService;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.entity.EmailChangeToken;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.EmailChangeTokenRepository;
import com.h2gas.model.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

// HU12: o admin troca o próprio e-mail só depois de confirmar o código enviado ao e-mail novo.
// Sem transação única de propósito (como no LoginTokenService): a tentativa errada precisa ficar
// gravada mesmo quando a confirmação termina em exceção.
@Service
public class EmailChangeService {
    private static final Logger log = LoggerFactory.getLogger(EmailChangeService.class);
    private static final String DUPLICATED_EMAIL = "E-mail já cadastrado.";
    private static final String SAME_EMAIL = "Esse já é o seu e-mail.";
    private static final String INVALID_CODE = "Código inválido.";
    private static final String EXHAUSTED_CODE = "Código expirado ou esgotado. Solicite um novo.";

    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final TokenService tokenService;
    private final Environment environment;
    private final SecureRandom random = new SecureRandom();

    public EmailChangeService(EmailChangeTokenRepository emailChangeTokenRepository, UserRepository userRepository,
                              MailService mailService, TokenService tokenService, Environment environment) {
        this.emailChangeTokenRepository = emailChangeTokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.tokenService = tokenService;
        this.environment = environment;
    }

    public void requestChange(User actor, String newEmail) {
        User user = reload(actor);
        String email = newEmail.trim();
        if (email.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessRuleException(SAME_EMAIL);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException(DUPLICATED_EMAIL);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        EmailChangeToken token = emailChangeTokenRepository.findByUserId(user.getId())
                .map(existing -> {
                    existing.refresh(email, code);
                    return existing;
                })
                .orElseGet(() -> new EmailChangeToken(user, email, code));
        emailChangeTokenRepository.save(token);

        sendCode(email, code);
    }

    public String confirmChange(User actor, String code) {
        User user = reload(actor);
        EmailChangeToken token = emailChangeTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessRuleException(INVALID_CODE));

        if (token.isExpiredOrExhausted()) {
            throw new BusinessRuleException(EXHAUSTED_CODE);
        }
        if (!token.getCode().equals(code)) {
            token.setAttempts(token.getAttempts() + 1);
            emailChangeTokenRepository.save(token);
            throw new BusinessRuleException(INVALID_CODE);
        }
        if (userRepository.existsByEmailIgnoreCase(token.getNewEmail())) {
            throw new BusinessRuleException(DUPLICATED_EMAIL);
        }

        user.setEmail(token.getNewEmail());
        User saved = userRepository.save(user);
        token.setUsed(true);
        emailChangeTokenRepository.save(token);
        return tokenService.generateToken(saved);
    }

    private User reload(User actor) {
        return userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    // SMTP indisponível não derruba o pedido: o código já está gravado e pode ser pedido de novo
    private void sendCode(String email, String code) {
        if (!environment.matchesProfiles("prod")) {
            log.info("HU12 (ambiente de desenvolvimento): código de troca de e-mail é {}", code);
        }
        try {
            mailService.sendMail(email, "Confirme seu novo e-mail na H2Gás",
                    "Seu código para confirmar este e-mail é: " + code + ". Válido por 15 minutos.");
        } catch (Exception e) {
            log.error("Não foi possível enviar o código de troca de e-mail", e);
        }
    }
}
