package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.RegisterDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.PasswordResetToken;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.PasswordTokenRepository;
import com.h2gas.model.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordTokenRepository passwordTokenRepository;

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

    public UserDto findByEmail(String email) {
        Optional<User> userEntityOptional = userRepository.findByEmail(email);
        return userEntityOptional.map(userMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum usuário com esse email: " + email));
    }

    public void createPasswordResetTokenForUser(User user, String token) {
//        PasswordResetToken myToken = new PasswordResetToken(token, user);
        Optional<PasswordResetToken> existingToken = passwordTokenRepository.findByUser(user);

        PasswordResetToken myToken = existingToken.orElse(new PasswordResetToken());
        myToken.setUser(user);
        myToken.refreshToken(token);

        passwordTokenRepository.save(myToken);
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

    public UserDto create(RegisterDTO data) {
        if (this.userRepository.existsByEmail(data.getEmail())) {
            throw new BusinessRuleException("Email já cadastrado.");
        }

        if (data.getUserType() == null || data.getUserType() == UserTypeEnum.ADMIN) {
            throw new BusinessRuleException("Cadastro de usuário ADMIN não permitido ou tipo vazio");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.getPassword());

        User user = new User(data.getName(), data.getEmail(), data.getPhoneNumber(),
                data.getCpf(), data.getCnpj(), encryptedPassword, data.getUserType());

        this.userRepository.save(user);

        return userMapper.toDto(user);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken tokenEntity = passwordTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRuleException("Token inválido"));

        if (tokenEntity.getExpiryDate().before(new Date())) {
            throw new BusinessRuleException("Token expirado");
        }

        User user = tokenEntity.getUser();
        String encryptedPassword = new BCryptPasswordEncoder().encode(newPassword);
        user.setPassword(encryptedPassword);
        userRepository.save(user);
        passwordTokenRepository.delete(tokenEntity);
    }
}
