package com.h2gas.controller;

import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.services.UserService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
}
