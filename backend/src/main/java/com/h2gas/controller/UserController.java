package com.h2gas.controller;

import com.h2gas.model.dto.AuthenticationDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping
    @ResponseBody
    public ResponseEntity<UserDto> getUsersId(@RequestParam String id) {
        UserDto userDto = userService.findByIdUSer(id);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getUsuarioLogado(Authentication authentication) {
        User user = (User) authentication.getPrincipal(); // ou ((User) authentication.getPrincipal()).getEmail()
        UserDto userDto = userMapper.toDto(user);
        return ResponseEntity.ok(userDto);
    }

}
