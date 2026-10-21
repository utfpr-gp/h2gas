package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AdminDashboardDto;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.OrderRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

// HU12: números da plataforma para o painel do administrador (RN01: tudo calculado aqui)
@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final CityRepository cityRepository;
    private final OrderRepository orderRepository;

    public AdminDashboardService(UserRepository userRepository, SupplierRepository supplierRepository,
                                 CityRepository cityRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.cityRepository = cityRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDto buildDashboard(LocalDate today) {
        Map<UserTypeEnum, Long> usersByType = new EnumMap<>(UserTypeEnum.class);
        Arrays.stream(UserTypeEnum.values()).forEach(type -> usersByType.put(type, userRepository.countByUserType(type)));

        Map<OrderStatusEnum, Long> ordersByStatus = new EnumMap<>(OrderStatusEnum.class);
        Arrays.stream(OrderStatusEnum.values()).forEach(status -> ordersByStatus.put(status, orderRepository.countByStatus(status)));

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        return new AdminDashboardDto(
                usersByType,
                supplierRepository.countByActive(true),
                supplierRepository.countByActive(false),
                cityRepository.count(),
                orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfDay, startOfDay.plusDays(1)),
                orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfMonth, startOfNextMonth),
                ordersByStatus,
                orderRepository.sumTotalPriceCreatedBetweenExcludingStatus(startOfMonth, startOfNextMonth, OrderStatusEnum.CANCELED));
    }
}
