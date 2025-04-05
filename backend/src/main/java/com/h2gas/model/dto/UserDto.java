package com.h2gas.model.dto;

import com.h2gas.enums.UserTypeEnum;

public record UserDto(String id,String name, String email, String phoneNumber,
                      String cpf, String cnpj, UserTypeEnum userType) {

}
