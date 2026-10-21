package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AdminCreateUserDto;
import com.h2gas.model.dto.AdminUserDto;
import com.h2gas.model.dto.DelivererCreateDTO;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.AdminUserMapper;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

// HU12: gestão de usuários pelo administrador
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final SupplierService supplierService;
    private final DelivererService delivererService;
    private final AdminUserMapper adminUserMapper;

    public AdminUserService(UserRepository userRepository, CityRepository cityRepository,
                            SupplierService supplierService, DelivererService delivererService,
                            AdminUserMapper adminUserMapper) {
        this.userRepository = userRepository;
        this.cityRepository = cityRepository;
        this.supplierService = supplierService;
        this.delivererService = delivererService;
        this.adminUserMapper = adminUserMapper;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listUsers(UserTypeEnum type, String query, Pageable pageable) {
        String term = StringUtils.hasText(query) ? query.trim().toLowerCase(Locale.ROOT) : null;
        Pageable byName = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        return userRepository.searchForAdmin(type, term, byName).map(adminUserMapper::toDto);
    }

    @Transactional
    public AdminUserDto createUser(AdminCreateUserDto data) {
        if (userRepository.existsByEmail(data.getEmail())) {
            throw new BusinessRuleException("E-mail já cadastrado.");
        }
        return switch (data.getUserType()) {
            case SUPPLIER -> adminUserMapper.toDto(createSupplier(data));
            case DELIVERER -> adminUserMapper.toDto(createDeliverer(data));
            case ADMIN, CUSTOMER -> adminUserMapper.toDto(userRepository.save(new User(data.getName(), data.getEmail(),
                    data.getPhoneNumber(), data.getCpf(), encode(data.getPassword()), data.getUserType())));
        };
    }

    @Transactional
    public void setEnabled(String userId, boolean enabled, User actor) {
        if (actor.getId().equals(userId)) {
            throw new BusinessRuleException("Você não pode desativar a sua própria conta.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    private Supplier createSupplier(AdminCreateUserDto data) {
        if (!StringUtils.hasText(data.getCnpj()) || data.getCityId() == null) {
            throw new BusinessRuleException("Fornecedor precisa de CNPJ e cidade.");
        }
        City city = cityRepository.findById(data.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada."));
        Supplier supplier = UserService.newSupplier(data.getName(), data.getEmail(), data.getPhoneNumber(),
                data.getCpf(), data.getCnpj(), encode(data.getPassword()));
        supplier.setCity(city);
        return supplierService.createSupplier(supplier);
    }

    private User createDeliverer(AdminCreateUserDto data) {
        if (!StringUtils.hasText(data.getSupplierId())) {
            throw new BusinessRuleException("Informe o fornecedor do entregador.");
        }
        delivererService.createDeliverer(new DelivererCreateDTO(data.getName(), data.getEmail(),
                data.getPhoneNumber(), data.getPassword(), null), data.getSupplierId());
        return userRepository.findByEmail(data.getEmail()).orElseThrow();
    }

    private static String encode(String password) {
        return new BCryptPasswordEncoder().encode(password);
    }
}
