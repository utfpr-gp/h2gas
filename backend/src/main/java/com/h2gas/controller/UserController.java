package com.h2gas.controller;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.ResetPasswordRequestDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.services.MailService;
import com.h2gas.services.UserService;
import com.h2gas.util.GenerateTokenPasswordReset;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.UUID;


@Tag(name = "Usuários")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailService mailService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<UserDto> getUsersId(@RequestParam String id) {
        UserDto userDto = userService.findByIdUser(id);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getUsuarioLogado(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        UserDto userDto = userMapper.toDto(user);
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/{id}")
    @ResponseBody
    @RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @RequestBody UserDto dto) {
        UserDto userDto = userService.update(id, dto);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        User user = userMapper.toEntityUser(userService.findByEmail(email));

        if (user.getUserType() == UserTypeEnum.ADMIN) {
            throw new BusinessRuleException("Não é possível alterar a senha de um administrador.");
        }

        String token = GenerateTokenPasswordReset.generateTokenReset();
        userService.createPasswordResetTokenForUser(user, token);
        try {
            mailService.sendPasswordResetEmail(user, token);
        } catch (MessagingException e) {
            throw new BusinessRuleException(e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Email de redefinição enviado"));
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso"));
    }
}
