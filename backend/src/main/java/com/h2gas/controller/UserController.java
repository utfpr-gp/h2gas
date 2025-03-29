package com.h2gas.controller;

import com.h2gas.dto.UserDto;
import com.h2gas.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<UserDto> getUsersId(@RequestParam String id) {
        UserDto userDto = userService.findByIdUSer(id);
        return ResponseEntity.ok(userDto);
    }
}
