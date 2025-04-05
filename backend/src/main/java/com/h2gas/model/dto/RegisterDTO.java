package com.h2gas.model.dto;

import com.h2gas.enums.UserTypeEnum;

public record RegisterDTO(String name, String email, String password, String phoneNumber,
                          String cpf, String cnpj, UserTypeEnum userType) {
}
