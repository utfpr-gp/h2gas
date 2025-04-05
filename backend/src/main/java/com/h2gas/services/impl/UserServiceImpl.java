package com.h2gas.services.impl;

import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        Optional<User> userEntityOptional = userRepository.findById(id);
        return userEntityOptional.map(UserMapper::toDto).orElseThrow(()
                -> new RuntimeException("User not found with id: " + id));
    }
}
