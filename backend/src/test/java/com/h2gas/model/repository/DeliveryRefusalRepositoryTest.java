package com.h2gas.model.repository;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class DeliveryRefusalRepositoryTest {

    @Autowired private DeliveryRefusalRepository refusalRepository;
    @Autowired private DelivererRepository delivererRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private CityRepository cityRepository;

    private Supplier fornecedor;
    private User cliente;

    private Deliverer novoEntregador(String email, String cpf) {
        User user = userRepository.saveAndFlush(new User(null, "Entregador", email, "41977776666", cpf, "senha", UserTypeEnum.DELIVERER));
        Deliverer d = new Deliverer();
        d.setUser(user);
        d.setSupplier(fornecedor);
        return delivererRepository.saveAndFlush(d);
    }

    private Order novoPedido() {
        Order o = new Order();
        o.setCustomer(cliente);
        o.setSupplier(fornecedor);
        o.setStatus(OrderStatusEnum.IN_PREPARATION);
        o.setPaymentMethod("PIX");
        o.setTotalPrice(new BigDecimal("125.00"));
        return orderRepository.saveAndFlush(o);
    }

    private void prepararCenario() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        Supplier s = new Supplier();
        s.setName("Gás Centro");
        s.setEmail("gas.centro@h2gas.test");
        s.setCpf("11111111111");
        s.setPassword("senha");
        s.setUserType(UserTypeEnum.SUPPLIER);
        s.setCnpj("12345678000199");
        s.setOpeningTime(LocalTime.of(8, 0));
        s.setClosingTime(LocalTime.of(18, 0));
        s.setAverageDeliveryTime(Duration.ofMinutes(40));
        s.setActive(true);
        s.setCity(city);
        fornecedor = supplierRepository.saveAndFlush(s);
        cliente = userRepository.saveAndFlush(new User(null, "Cliente", "cliente@h2gas.test", "41999999999", "22222222222", "senha", UserTypeEnum.CUSTOMER));
    }

    @Test
    @DisplayName("HU07: lista as recusas do entregador da mais recente para a mais antiga")
    void deveListarRecusasDoEntregadorDaMaisRecenteParaAMaisAntiga() {
        prepararCenario();
        Deliverer carlos = novoEntregador("carlos@h2gas.test", "33333333333");
        Deliverer ana = novoEntregador("ana@h2gas.test", "44444444444");
        Order p1 = novoPedido();
        Order p2 = novoPedido();

        refusalRepository.saveAndFlush(new DeliveryRefusal(null, p1, carlos, "Moto quebrou", LocalDateTime.of(2026, 10, 18, 10, 0)));
        refusalRepository.saveAndFlush(new DeliveryRefusal(null, p2, carlos, "Endereço fora da rota", LocalDateTime.of(2026, 10, 18, 11, 0)));
        refusalRepository.saveAndFlush(new DeliveryRefusal(null, p1, ana, "Sem troco", LocalDateTime.of(2026, 10, 18, 12, 0)));

        List<DeliveryRefusal> doCarlos = refusalRepository.findByDelivererIdOrderByCreatedAtDesc(carlos.getId());

        assertEquals(List.of("Endereço fora da rota", "Moto quebrou"), doCarlos.stream().map(DeliveryRefusal::getReason).toList());
    }
}
