package com.h2gas.services.impl;

import com.h2gas.dto.UserDto;
import com.h2gas.entity.UserEntity;
import com.h2gas.mapper.UserMapper;
import com.h2gas.repository.UserRepository;
import com.h2gas.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<UserDto> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto findByIdUSer(String id) {
        Optional<UserEntity> userEntityOptional = userRepository.findById(id);
        return userEntityOptional.map(UserMapper::toDto).orElseThrow(()
                -> new RuntimeException("User not found with id: " + id));
    }
}
