package com.h2gas.services;

import com.h2gas.model.dto.UserDto;

import java.util.List;

public interface UserService {
    public List<UserDto> findAllUsers();

    public UserDto findByIdUSer(String id);
}
