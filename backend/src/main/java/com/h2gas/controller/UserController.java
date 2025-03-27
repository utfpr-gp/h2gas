package com.h2gas.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public String test() {
        System.out.println("testando");
        return "API funcionando!";
    }
}
