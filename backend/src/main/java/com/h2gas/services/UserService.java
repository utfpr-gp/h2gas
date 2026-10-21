package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.ForbiddenResourceException;
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
                -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    public User findEntityByIdUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
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

    // F7: só o próprio usuário (ou ADMIN) edita; o e-mail é a identidade do login por código (HU11),
    // então usuário comum não o troca por aqui
    public UserDto update(String id, UserDto userDto, User actor) {
        boolean isAdmin = actor.getUserType() == UserTypeEnum.ADMIN;
        if (!isAdmin && !actor.getId().equals(id)) {
            throw new ForbiddenResourceException("Você só pode editar os seus próprios dados.");
        }
        User userEdit = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        boolean emailChanged = userDto.getEmail() != null && !userDto.getEmail().equals(userEdit.getEmail());
        if (emailChanged && !isAdmin) {
            throw new BusinessRuleException("O e-mail não pode ser alterado por aqui.");
        }
        if (emailChanged && userRepository.existsByEmail(userDto.getEmail())) {
            throw new BusinessRuleException("E-mail já cadastrado.");
        }

        userEdit.setName(userDto.getName());
        if (emailChanged) {
            userEdit.setEmail(userDto.getEmail());
        }
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
            throw new BusinessRuleException("E-mail já cadastrado.");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.getPassword());

        if (data.getUserType() == null || data.getUserType() == UserTypeEnum.ADMIN) {
            throw new BusinessRuleException("Cadastro de usuário ADMIN não permitido ou tipo vazio");
        }

        if (data.getUserType() == UserTypeEnum.SUPPLIER) {
            Supplier supplier = newSupplier(data.getName(), data.getEmail(), data.getPhoneNumber(),
                    data.getCpf(), data.getCnpj(), encryptedPassword);

            if (data.getAddress() != null) {
                City city = cityRepository.findById(data.getAddress().getCityId())
                        .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada."));
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
                        .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada."));
                addressService.createAddress(data.getAddress(), user, city);
            }

            return userMapper.toDto(user);
        }
    }

    // padrões do fornecedor recém-cadastrado; ele ajusta horário e prazo depois de aprovado
    static Supplier newSupplier(String name, String email, String phoneNumber, String cpf,
                                String cnpj, String encryptedPassword) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setEmail(email);
        supplier.setCpf(cpf);
        supplier.setPassword(encryptedPassword);
        supplier.setUserType(UserTypeEnum.SUPPLIER);
        supplier.setCnpj(cnpj);
        supplier.setPhoneNumber(phoneNumber);
        supplier.setDescription("Fornecedor pré-criado");
        supplier.setOpeningTime(LocalTime.parse("08:00"));
        supplier.setClosingTime(LocalTime.parse("18:00"));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(60));
        return supplier;
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
