package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AdminDashboardDto;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.OrderRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

// o H2 é compartilhado entre as classes de teste: os números são comparados com a foto tirada antes dos dados
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminDashboardServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 20);

    @Autowired private AdminDashboardService dashboardService;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager em;

    private City cidade;
    private User cliente;
    private AdminDashboardDto antes;

    @BeforeEach
    void setUp() {
        antes = dashboardService.buildDashboard(HOJE);
        cidade = cityRepository.save(new City(null, "Pato Branco", "PR"));
        cliente = userRepository.save(new User("Cliente", "cliente.painel@teste.com", "41911111111", null, "senha", UserTypeEnum.CUSTOMER));
    }

    private Supplier fornecedor(String email, boolean aprovado) {
        Supplier s = new Supplier();
        s.setName("Fornecedor " + email);
        s.setEmail(email);
        s.setPhoneNumber("41900000000");
        s.setPassword("senha");
        s.setUserType(UserTypeEnum.SUPPLIER);
        s.setCnpj("12345678000100");
        s.setOpeningTime(LocalTime.of(8, 0));
        s.setClosingTime(LocalTime.of(18, 0));
        s.setAverageDeliveryTime(Duration.ofMinutes(30));
        s.setActive(aprovado);
        s.setCity(cidade);
        return supplierRepository.save(s);
    }

    /** createdAt é @CreationTimestamp: o valor é ajustado por JPQL depois do insert. */
    private void pedido(Supplier supplier, OrderStatusEnum status, String total, LocalDateTime criadoEm) {
        Order o = new Order();
        o.setCustomer(cliente);
        o.setSupplier(supplier);
        o.setStatus(status);
        o.setPaymentMethod("PIX");
        o.setTotalPrice(new BigDecimal(total));
        o = orderRepository.saveAndFlush(o);
        em.createQuery("update Order o set o.createdAt = :d where o.id = :id")
                .setParameter("d", criadoEm).setParameter("id", o.getId()).executeUpdate();
        em.clear();
    }

    private long delta(java.util.Map<?, Long> depois, java.util.Map<?, Long> antes, Object chave) {
        return depois.get(chave) - antes.get(chave);
    }

    @Test
    @DisplayName("HU12: resume usuários, fornecedores, cidades e pedidos da plataforma")
    void deveResumirAPlataformaParaOAdmin() {
        Supplier aprovado = fornecedor("aprovado@teste.com", true);
        fornecedor("pendente@teste.com", false);
        userRepository.save(new User("Admin 2", "admin2@teste.com", "41900000001", null, "senha", UserTypeEnum.ADMIN));
        pedido(aprovado, OrderStatusEnum.PENDING, "100.00", HOJE.atTime(9, 0));
        pedido(aprovado, OrderStatusEnum.DELIVERED, "50.00", HOJE.withDayOfMonth(1).atTime(10, 0));
        pedido(aprovado, OrderStatusEnum.DELIVERED, "999.00", HOJE.minusMonths(1).atTime(10, 0));

        AdminDashboardDto depois = dashboardService.buildDashboard(HOJE);

        assertEquals(1, delta(depois.getUsersByType(), antes.getUsersByType(), UserTypeEnum.CUSTOMER));
        assertEquals(2, delta(depois.getUsersByType(), antes.getUsersByType(), UserTypeEnum.SUPPLIER));
        assertEquals(1, delta(depois.getUsersByType(), antes.getUsersByType(), UserTypeEnum.ADMIN));
        assertEquals(0, delta(depois.getUsersByType(), antes.getUsersByType(), UserTypeEnum.DELIVERER));
        assertEquals(1, depois.getActiveSuppliers() - antes.getActiveSuppliers());
        assertEquals(1, depois.getPendingSuppliers() - antes.getPendingSuppliers());
        assertEquals(1, depois.getCities() - antes.getCities());
        assertEquals(1, depois.getOrdersToday() - antes.getOrdersToday());
        assertEquals(2, depois.getOrdersThisMonth() - antes.getOrdersThisMonth());
        assertEquals(1, delta(depois.getOrdersByStatus(), antes.getOrdersByStatus(), OrderStatusEnum.PENDING));
        assertEquals(2, delta(depois.getOrdersByStatus(), antes.getOrdersByStatus(), OrderStatusEnum.DELIVERED));
        assertEquals(0, new BigDecimal("150.00").compareTo(depois.getRevenueThisMonth().subtract(antes.getRevenueThisMonth())));
    }

    @Test
    @DisplayName("HU12: pedido cancelado conta no status mas não no faturamento")
    void naoDeveSomarPedidosCanceladosNoFaturamento() {
        Supplier aprovado = fornecedor("aprovado@teste.com", true);
        pedido(aprovado, OrderStatusEnum.CANCELED, "80.00", HOJE.atTime(9, 0));
        pedido(aprovado, OrderStatusEnum.CONFIRMED, "20.00", HOJE.atTime(11, 0));

        AdminDashboardDto depois = dashboardService.buildDashboard(HOJE);

        assertEquals(1, delta(depois.getOrdersByStatus(), antes.getOrdersByStatus(), OrderStatusEnum.CANCELED));
        assertEquals(2, depois.getOrdersToday() - antes.getOrdersToday());
        assertEquals(0, new BigDecimal("20.00").compareTo(depois.getRevenueThisMonth().subtract(antes.getRevenueThisMonth())));
    }
}
