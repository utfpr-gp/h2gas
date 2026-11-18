package com.h2gas.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

// HU12: código de 6 dígitos enviado ao e-mail novo; um registro por usuário, renovado a cada pedido
@Entity
@Table(name = "email_change_tokens")
@Getter
@Setter
@NoArgsConstructor
public class EmailChangeToken {
    private static final int EXPIRATION_SECONDS = 15 * 60;
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id", unique = true)
    private User user;

    @Column(name = "new_email", nullable = false)
    private String newEmail;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expiry_date", nullable = false)
    private Date expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private int attempts = 0;

    public EmailChangeToken(User user, String newEmail, String code) {
        this.user = user;
        refresh(newEmail, code);
    }

    public void refresh(String newEmail, String code) {
        this.newEmail = newEmail;
        this.code = code;
        this.expiryDate = new Date(System.currentTimeMillis() + EXPIRATION_SECONDS * 1000L);
        this.used = false;
        this.attempts = 0;
    }

    public boolean isExpiredOrExhausted() {
        return used || attempts >= MAX_ATTEMPTS || expiryDate.before(new Date());
    }
}
