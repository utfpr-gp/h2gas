package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.*;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.PasswordTokenRepository;
import com.h2gas.model.repository.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
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

    @Autowired
    private AddressService addressService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CityRepository cityRepository;

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
//        if (userEdit.getUserType() != UserTypeEnum.CUSTOMER) {
//            userEdit.setCnpj(userDto.getCnpj());
//        }
        userEdit.setPhoneNumber(userDto.getPhoneNumber());

        User saved = userRepository.save(userEdit);

        return userMapper.toDto(saved);
    }

    @Transactional
    public UserDto create(RegisterDTO data) {
        if (userRepository.existsByEmail(data.getEmail())) {
            throw new BusinessRuleException("Email já cadastrado.");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.getPassword());

        if (data.getUserType() == null || data.getUserType() == UserTypeEnum.ADMIN) {
            throw new BusinessRuleException("Cadastro de usuário ADMIN não permitido ou tipo vazio");
        }

        if (data.getUserType() == UserTypeEnum.SUPPLIER) {
            Supplier supplier = new Supplier();
            supplier.setName(data.getName());
            supplier.setEmail(data.getEmail());
            supplier.setCpf(data.getCpf());
            supplier.setPassword(encryptedPassword);
            supplier.setUserType(data.getUserType());
            supplier.setCnpj(data.getCnpj());
            supplier.setPhoneNumber(data.getPhoneNumber());
            supplier.setDescription("Fornecedor pré-criado");
            supplier.setOpeningTime(LocalTime.parse("08:00"));
            supplier.setClosingTime(LocalTime.parse("18:00"));
            supplier.setAverageDeliveryTime(Duration.ofMinutes(60));

            if (data.getAddress() != null) {
                City city = cityRepository.findById(data.getAddress().getCityId())
                        .orElseThrow(() -> new ResourceNotFoundException("City not found"));
                supplier.setCity(city);

                Supplier savedSupplier = supplierService.createSupplier(supplier);
                Address address = addressService.createAddress(data.getAddress(), savedSupplier, city);
                savedSupplier.setAddress(address);
                supplierService.createSupplier(savedSupplier);
                return userMapper.toDto(savedSupplier);
            }

            Supplier savedSupplier = supplierService.createSupplier(supplier);
            return userMapper.toDto(savedSupplier);
        } else {
            // Cria usuário normal — endereço é definido depois do login (HU01)
            User user = new User(
                    data.getName(),
                    data.getEmail(),
                    data.getPhoneNumber(),
                    data.getCpf(),
                    encryptedPassword,
                    data.getUserType()
            );
            userRepository.save(user);

            if (data.getAddress() != null) {
                City city = cityRepository.findById(data.getAddress().getCityId())
                        .orElseThrow(() -> new ResourceNotFoundException("City not found"));
                addressService.createAddress(data.getAddress(), user, city);
            }

            return userMapper.toDto(user);
        }
    }

    private static SupplierCreateDTO getSupplierCreateDTO(RegisterDTO data, User user, Address address) {
        SupplierCreateDTO supplierDTO = new SupplierCreateDTO();
        supplierDTO.setUserId(user.getId());
        supplierDTO.setName(user.getName());
        supplierDTO.setPhone(user.getPhoneNumber());
        supplierDTO.setCnpj(data.getCnpj()); // se você adicionar ao RegisterDTO
        supplierDTO.setAddressId(address.getId());
        supplierDTO.setDescription("Fornecedor pré-criado");
        supplierDTO.setOpeningTime("08:00");
        supplierDTO.setClosingTime("18:00");
        supplierDTO.setAverageDeliveryTime(60L);
        supplierDTO.setCityId(address.getCity().getId());
        return supplierDTO;
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
