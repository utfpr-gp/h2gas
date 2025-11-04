package com.h2gas.model.dto;

import com.h2gas.enums.UserTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private String cpf;
    private String cnpj;
    private UserTypeEnum userType;
    private AddressDTO address;
}
