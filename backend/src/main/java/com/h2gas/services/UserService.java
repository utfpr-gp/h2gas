package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    public List<UserDto> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public UserDto findByIdUser(String id) {
        Optional<User> userEntityOptional = userRepository.findById(id);
        return userEntityOptional.map(userMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public UserDto findByName(String name) {
        Optional<User> userEntityOptional = userRepository.findByName(name);
        return userEntityOptional.map(userMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum usuário com esse nome: " + name));
    }
}
