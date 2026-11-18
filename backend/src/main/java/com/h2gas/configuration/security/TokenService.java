package com.h2gas.configuration.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.h2gas.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(User user) {
            try {
                Algorithm algorithm = Algorithm.HMAC256(secret);

                return JWT.create()
                        .withIssuer("h2gas")
                        .withSubject(user.getEmail())
                        .withExpiresAt(genExperirationDate())
                        .sign(algorithm);
            } catch (JWTCreationException e) {
                throw new IllegalStateException("Não foi possível gerar o acesso.", e);
            }
    }

    public String validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer("h2gas")
                .build()
                .verify(token)
                .getSubject();
    }

    private Instant genExperirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
