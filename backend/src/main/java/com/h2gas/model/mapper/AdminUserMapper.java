package com.h2gas.model.mapper;

import com.h2gas.model.dto.AdminUserDto;
import com.h2gas.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AdminUserMapper {

    public AdminUserDto toDto(User user) {
        return new AdminUserDto(user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber(),
                user.getUserType(), user.isEnabled());
    }
}
