package com.h2gas.services;

import com.h2gas.configuration.ClockConfig;
import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.*;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

// F12 item (b), Cenário 7: o pedido é gravado com a hora de Brasília
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FusoHorarioTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private CityRepository cityRepository;

    @Test
    void deveGravarOPedidoNoHorarioDeBrasilia() {
        City city = cityRepository.save(new City(null, "Guarapuava", "PR"));
        Supplier loja = new Supplier();
        loja.setName("Loja Fuso");
        loja.setEmail("loja.fuso@teste.com");
        loja.setPhoneNumber("42900000000");
        loja.setCpf("88888888888");
        loja.setPassword("senha");
        loja.setUserType(UserTypeEnum.SUPPLIER);
        loja.setCnpj("12345678000300");
        loja.setDescription("Loja de teste");
        loja.setOpeningTime(LocalTime.of(8, 0));
        loja.setClosingTime(LocalTime.of(18, 0));
        loja.setAverageDeliveryTime(Duration.ofMinutes(30));
        loja.setActive(true);
        loja.setCity(city);
        loja = supplierRepository.save(loja);
        User cliente = userRepository.save(new User(null, "Cliente Fuso", "cliente.fuso@teste.com", "42999999999",
                "99999999999", "senha", UserTypeEnum.CUSTOMER));

        Order pedido = new Order();
        pedido.setCustomer(cliente);
        pedido.setSupplier(loja);
        pedido.setStatus(OrderStatusEnum.PENDING);
        pedido.setDeliveryMode(DeliveryModeEnum.PICKUP);
        pedido.setPaymentMethod("PIX");
        pedido.setTotalPrice(new BigDecimal("110.00"));
        pedido = orderRepository.saveAndFlush(pedido);

        LocalDateTime agoraEmBrasilia = LocalDateTime.now(ClockConfig.BRASILIA);
        Duration diferenca = Duration.between(pedido.getCreatedAt(), agoraEmBrasilia).abs();
        assertTrue(diferenca.toMinutes() < 1,
                "createdAt " + pedido.getCreatedAt() + " deveria estar perto de " + agoraEmBrasilia);
    }
}
