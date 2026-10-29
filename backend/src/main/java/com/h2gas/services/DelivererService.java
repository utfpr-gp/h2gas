package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.DelivererCreateDTO;
import com.h2gas.model.dto.DelivererDTO;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.DelivererRepository;
import com.h2gas.model.repository.OrderRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DelivererService {

    private final DelivererRepository delivererRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final OrderRepository orderRepository;

    public DelivererService(DelivererRepository delivererRepository, UserRepository userRepository,
                            SupplierRepository supplierRepository, OrderRepository orderRepository) {
        this.delivererRepository = delivererRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.orderRepository = orderRepository;
    }

    // HU07: o fornecedor cadastra a própria frota (par User(DELIVERER) + Deliverer)
    @Transactional
    public DelivererDTO createDeliverer(DelivererCreateDTO dto, String supplierId) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("E-mail já cadastrado.");
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado com id: " + supplierId));

        User user = new User(dto.getName(), dto.getEmail(), dto.getPhoneNumber(), null,
                new BCryptPasswordEncoder().encode(dto.getPassword()), UserTypeEnum.DELIVERER);
        userRepository.save(user);

        Deliverer deliverer = new Deliverer();
        deliverer.setUser(user);
        deliverer.setSupplier(supplier);
        deliverer.setActive(true);
        deliverer.setVehicleInfo(dto.getVehicleInfo());
        return toDto(delivererRepository.save(deliverer));
    }

    @Transactional(readOnly = true)
    public List<DelivererDTO> findMine(String supplierId) {
        return delivererRepository.findBySupplierIdOrderByUserNameAsc(supplierId).stream()
                .map(this::toDto)
                .toList();
    }

    // HU22: o fornecedor define uma senha nova para um entregador da própria frota (a antiga não é recuperável: BCrypt)
    @Transactional
    public void resetPassword(Long delivererId, String supplierId, String password) {
        Deliverer deliverer = findEntityById(delivererId);
        if (deliverer.getSupplier() == null || !deliverer.getSupplier().getId().equals(supplierId)) {
            throw new ForbiddenResourceException("Entregador não pertence à sua frota.");
        }
        User user = deliverer.getUser();
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        userRepository.save(user);
    }

    public Deliverer findEntityById(Long id) {
        return delivererRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com id: " + id));
    }

    public Deliverer findByUserId(String userId) {
        return delivererRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não é um entregador cadastrado."));
    }

    private DelivererDTO toDto(Deliverer deliverer) {
        boolean busy = orderRepository.existsByDelivererIdAndStatus(deliverer.getId(), OrderStatusEnum.OUT_FOR_DELIVERY);
        User user = deliverer.getUser();
        return new DelivererDTO(deliverer.getId(), user.getName(), user.getEmail(), user.getPhoneNumber(),
                deliverer.getVehicleInfo(), deliverer.isActive(), busy);
    }
}
