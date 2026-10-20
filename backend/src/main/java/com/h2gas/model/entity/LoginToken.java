package com.h2gas.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

// HU11: código de 6 dígitos enviado por e-mail; um registro por usuário, renovado a cada pedido
@Entity
@Table(name = "login_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginToken {
    private static final int EXPIRATION_SECONDS = 5 * 60;
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String code;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Date expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private int attempts = 0;

    public LoginToken(String code, User user) {
        this.code = code;
        this.user = user;
        this.expiryDate = newExpiryDate();
    }

    public void refresh(String code) {
        this.code = code;
        this.expiryDate = newExpiryDate();
        this.used = false;
        this.attempts = 0;
    }

    public boolean isExpiredOrExhausted() {
        return used || attempts >= MAX_ATTEMPTS || expiryDate.before(new Date());
    }

    private Date newExpiryDate() {
        return new Date(System.currentTimeMillis() + EXPIRATION_SECONDS * 1000L);
    }
}
