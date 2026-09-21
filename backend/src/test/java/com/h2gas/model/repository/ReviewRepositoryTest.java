package com.h2gas.model.repository;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.Review;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private DelivererRepository delivererRepository;

    @Test
    @DisplayName("Deve salvar avaliação válida com nota entre 1 e 5")
    void deveSalvarAvaliacaoValida() {
        ReviewContext context = persistirPedidoEntregue();

        Review review = new Review();
        review.setOrder(context.order);
        review.setSupplier(context.supplier);
        review.setDeliverer(context.deliverer);
        review.setRating(5);
        review.setComment("Entrega rápida");

        Review saved = reviewRepository.saveAndFlush(review);

        assertNotNull(saved.getId());
        assertEquals(5, saved.getRating());
        assertEquals(context.order.getId(), saved.getOrder().getId());
    }

    @Test
    @DisplayName("Deve rejeitar avaliação com nota fora do intervalo")
    void deveRejeitarAvaliacaoComNotaForaDoIntervalo() {
        ReviewContext context = persistirPedidoEntregue();

        Review reviewInvalida = new Review();
        reviewInvalida.setOrder(context.order);
        reviewInvalida.setSupplier(context.supplier);
        reviewInvalida.setRating(0);

        assertThrows(DataIntegrityViolationException.class,
                () -> reviewRepository.saveAndFlush(reviewInvalida));
    }

    private ReviewContext persistirPedidoEntregue() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));

        User customer = userRepository.save(new User(
                "Cliente Review",
                "cliente.review@h2gas.test",
                "41911112222",
                "11122233344",
                "senha",
                UserTypeEnum.CUSTOMER
        ));

        Address address = addressRepository.save(new Address(
                null, "Rua Review", "Casa", "1", null, "Centro", "80000-000", city, customer
        ));

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor Review");
        supplier.setEmail("fornecedor.review@h2gas.test");
        supplier.setPassword("senha");
        supplier.setUserType(UserTypeEnum.SUPPLIER);
        supplier.setCnpj("99888777000166");
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplier.setActive(true);
        supplier.setCity(city);
        supplier.setAddress(address);
        supplier = supplierRepository.saveAndFlush(supplier);

        User delivererUser = userRepository.save(new User(
                "Entregador Review",
                "entregador.review@h2gas.test",
                "41933334444",
                "55566677788",
                "senha",
                UserTypeEnum.DELIVERER
        ));

        Deliverer deliverer = new Deliverer();
        deliverer.setUser(delivererUser);
        deliverer.setSupplier(supplier);
        deliverer = delivererRepository.saveAndFlush(deliverer);

        Order order = new Order();
        order.setCustomer(customer);
        order.setSupplier(supplier);
        order.setDeliveryAddress(address);
        order.setStatus(OrderStatusEnum.DELIVERED);
        order.setTotalPrice(new BigDecimal("50.00"));
        order.setOrderDate(LocalDateTime.now());
        order = orderRepository.saveAndFlush(order);

        return new ReviewContext(order, supplier, deliverer);
    }

    private record ReviewContext(Order order, Supplier supplier, Deliverer deliverer) {
    }
}
