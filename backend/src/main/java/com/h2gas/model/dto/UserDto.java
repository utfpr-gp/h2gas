package com.h2gas.model.dto;

import com.h2gas.enums.UserTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String cpf;
    private String cnpj;
    private UserTypeEnum userType;
}
