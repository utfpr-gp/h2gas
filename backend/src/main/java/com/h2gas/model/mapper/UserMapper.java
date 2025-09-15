package com.h2gas.model.mapper;

import com.h2gas.model.dto.RegisterDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    @Autowired
    private ModelMapper modelMapper;

    public UserDto toDto(User data) {
        UserDto userDto = modelMapper.map(data, UserDto.class);

        if (data.getSupplier() != null) {
            userDto.setSupplierId(data.getSupplier().getId());
        }

        return userDto;
    }

    public User toEntityRegisterUser(RegisterDTO data) {
        return modelMapper.map(data, User.class);
    }

    public User toEntityUser(UserDto dto) {
        return modelMapper.map(dto, User.class);
    }
}
