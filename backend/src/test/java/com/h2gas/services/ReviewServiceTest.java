package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CreateReviewDTO;
import com.h2gas.model.dto.OrderReviewDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

// HU24: o cliente avalia a loja e o entregador do pedido entregue
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewServiceTest {

    @Autowired private ReviewService reviewService;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private DelivererRepository delivererRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CityRepository cityRepository;

    private Supplier loja;
    private User cliente;
    private User outroCliente;
    private Deliverer entregador;

    @BeforeEach
    void setUp() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        loja = new Supplier();
        loja.setName("Gás Centro");
        loja.setEmail("gas.centro@teste.com");
        loja.setPhoneNumber("41900000000");
        loja.setCpf("11111111111");
        loja.setPassword("senha");
        loja.setUserType(UserTypeEnum.SUPPLIER);
        loja.setCnpj("12345678000100");
        loja.setDescription("Loja de teste");
        loja.setOpeningTime(LocalTime.of(8, 0));
        loja.setClosingTime(LocalTime.of(18, 0));
        loja.setAverageDeliveryTime(Duration.ofMinutes(30));
        loja.setActive(true);
        loja.setCity(city);
        loja = supplierRepository.save(loja);

        cliente = userRepository.save(new User(null, "Maria", "maria@teste.com", "41999999999",
                "33333333333", "senha", UserTypeEnum.CUSTOMER));
        outroCliente = userRepository.save(new User(null, "João", "joao@teste.com", "41999999998",
                "44444444444", "senha", UserTypeEnum.CUSTOMER));
        User userEntregador = userRepository.save(new User(null, "Carlos", "carlos@teste.com", "41988887777",
                "55555555555", "senha", UserTypeEnum.DELIVERER));
        entregador = new Deliverer();
        entregador.setUser(userEntregador);
        entregador.setSupplier(loja);
        entregador = delivererRepository.save(entregador);
    }

    private Order pedido(OrderStatusEnum status, DeliveryModeEnum modo, Deliverer deliverer) {
        Order o = new Order();
        o.setCustomer(cliente);
        o.setSupplier(loja);
        o.setStatus(status);
        o.setDeliveryMode(modo);
        o.setPaymentMethod("PIX");
        o.setTotalPrice(new BigDecimal("110.00"));
        o.setDeliverer(deliverer);
        return orderRepository.save(o);
    }

    @Test
    void deveGravarAsDuasNotasDoPedidoEntregue() {
        Order o = pedido(OrderStatusEnum.DELIVERED, DeliveryModeEnum.DELIVERY, entregador);

        OrderReviewDTO dto = reviewService.reviewOrder(o.getId(), cliente.getId(),
                new CreateReviewDTO(4, 5, "Chegou rápido"));

        assertEquals(4, dto.storeRating());
        assertEquals(5, dto.delivererRating());
        assertEquals("Chegou rápido", dto.comment());
        Review salvo = reviewRepository.findByOrderId(o.getId()).orElseThrow();
        assertEquals(loja.getId(), salvo.getSupplier().getId());
        assertEquals(entregador.getId(), salvo.getDeliverer().getId());
        assertEquals(4, salvo.getRating());
        assertEquals(5, salvo.getDelivererRating());
    }

    @Test
    void deveGravarSoANotaDaLojaNaRetirada() {
        Order o = pedido(OrderStatusEnum.DELIVERED, DeliveryModeEnum.PICKUP, null);

        OrderReviewDTO dto = reviewService.reviewOrder(o.getId(), cliente.getId(), new CreateReviewDTO(3, null, null));

        assertEquals(3, dto.storeRating());
        assertNull(dto.delivererRating());
        assertNull(reviewRepository.findByOrderId(o.getId()).orElseThrow().getDeliverer());
    }

    @Test
    void deveRecusarPedidoNaoEntregue() {
        Order o = pedido(OrderStatusEnum.OUT_FOR_DELIVERY, DeliveryModeEnum.DELIVERY, entregador);

        assertThrows(BusinessRuleException.class,
                () -> reviewService.reviewOrder(o.getId(), cliente.getId(), new CreateReviewDTO(5, 5, null)));
        assertTrue(reviewRepository.findByOrderId(o.getId()).isEmpty());
    }

    @Test
    void deveRecusarPedidoDeOutroCliente() {
        Order o = pedido(OrderStatusEnum.DELIVERED, DeliveryModeEnum.DELIVERY, entregador);

        assertThrows(ForbiddenResourceException.class,
                () -> reviewService.reviewOrder(o.getId(), outroCliente.getId(), new CreateReviewDTO(1, null, null)));
    }

    @Test
    void deveRecusarPedidoInexistente() {
        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.reviewOrder("nao-existe", cliente.getId(), new CreateReviewDTO(5, null, null)));
    }

    @Test
    void deveRecusarSegundaAvaliacao() {
        Order o = pedido(OrderStatusEnum.DELIVERED, DeliveryModeEnum.DELIVERY, entregador);
        reviewService.reviewOrder(o.getId(), cliente.getId(), new CreateReviewDTO(5, 5, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.reviewOrder(o.getId(), cliente.getId(), new CreateReviewDTO(1, 1, null)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(5, reviewRepository.findByOrderId(o.getId()).orElseThrow().getRating());
    }

    @Test
    void deveRecusarNotaDoEntregadorSemEntregador() {
        Order o = pedido(OrderStatusEnum.DELIVERED, DeliveryModeEnum.PICKUP, null);

        assertThrows(BusinessRuleException.class,
                () -> reviewService.reviewOrder(o.getId(), cliente.getId(), new CreateReviewDTO(5, 4, null)));
    }

    @Test
    void deveGuardarComentarioEmBrancoComoNulo() {
        Order o = pedido(OrderStatusEnum.DELIVERED, DeliveryModeEnum.DELIVERY, entregador);

        OrderReviewDTO dto = reviewService.reviewOrder(o.getId(), cliente.getId(), new CreateReviewDTO(4, null, "   "));

        assertNull(dto.comment());
        assertNull(reviewRepository.findByOrderId(o.getId()).orElseThrow().getComment());
    }
}
