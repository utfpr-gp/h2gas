package com.h2gas.model.mapper;

import com.h2gas.model.dto.RegisterDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;

public class UserMapper {
//    TODO alterar para usar o modelMapper
    public static UserDto toDto(User data) {
        return new UserDto(
                data.getId(),
                data.getName(),
                data.getEmail(),
                data.getPhoneNumber(),
                data.getCpf(),
                data.getCnpj(),
                data.getUserType()
        );
    }

    public static User toEntity(RegisterDTO data) {
        return new User(
                null,
                data.name(),
                data.email(),
                data.phoneNumber(),
                data.cpf(),
                data.cnpj(),
                data.password(),
                data.userType()
        );
    }
}
