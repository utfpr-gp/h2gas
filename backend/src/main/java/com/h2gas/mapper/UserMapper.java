package com.h2gas.mapper;

import com.h2gas.dto.RegisterDTO;
import com.h2gas.dto.UserDto;
import com.h2gas.entity.UserEntity;

public class UserMapper {
    public static UserDto toDto(UserEntity data) {
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

//    public static UserEntity toEntity(UserDto data) {
//        return new UserEntity(
//                data.id(),
//                data.name(),
//                data.email(),
//                data.phoneNumber(),
//                data.cpf(),
//                data.cnpj(),
//                data.userType()
//        );
//    }

    public static UserEntity toEntity(RegisterDTO data) {
        return new UserEntity(
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
