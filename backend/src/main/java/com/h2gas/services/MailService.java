package com.h2gas.services;

import com.h2gas.model.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class MailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${frontend.url}")
    private String frontendUrl;

    public void sendMail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(User user, String token) throws MessagingException {
        String url = this.frontendUrl + "/resetar-senha?token=" + token;
        String body = "<!DOCTYPE html>" +
                "<html>" +
                "<body>" +
                "<h2>Olá " + user.getName() + ",</h2>" +
                "<p>Recebemos uma solicitação para redefinir sua senha.</p>" +
                "<p>Clique no botão abaixo para criar uma nova senha:</p>" +
                "<a href='" + url + "' style='display:inline-block;padding:10px 20px;" +
                "background-color:#007bff;color:white;text-decoration:none;border-radius:5px;'>Redefinir Senha</a>" +
                "<p>Se você não solicitou a alteração, ignore este email.</p>" +
                "<br>" +
                "<p>Atenciosamente,<br>Equipe H2Gas</p>" +
                "</body>" +
                "</html>";

//        SimpleMailMessage message = new SimpleMailMessage();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(user.getEmail());
        helper.setSubject("Redefinição de senha");
        helper.setText(body, true);

        mailSender.send(message);
    }
}
