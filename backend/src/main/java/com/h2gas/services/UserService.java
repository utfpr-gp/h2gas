package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
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

    public User findEntityByIdUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public UserDto findByName(String name) {
        Optional<User> userEntityOptional = userRepository.findByName(name);
        return userEntityOptional.map(userMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum usuário com esse nome: " + name));
    }

    public UserDto update(String id, UserDto userDto) {
        User userEdit = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userEdit.setName(userDto.getName());
        if (userRepository.existsByEmail(userDto.getEmail()) &&
                !userEdit.getEmail().equals(userDto.getEmail())) {
            throw new BusinessRuleException("Email já cadastrado.");
        }

        userEdit.setEmail(userDto.getEmail());
        if (userEdit.getUserType() != UserTypeEnum.CUSTOMER) {
            userEdit.setCnpj(userDto.getCnpj());
        }
        userEdit.setPhoneNumber(userDto.getPhoneNumber());

        User saved = userRepository.save(userEdit);

        return userMapper.toDto(saved);
    }
}
