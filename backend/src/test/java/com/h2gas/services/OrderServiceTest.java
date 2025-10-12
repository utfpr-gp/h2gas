package com.h2gas.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@TestPropertySource(properties = "jwt.secret=segredoFake123")
@TestPropertySource(properties = {
        "jwt.secret=segredoFake123",
        "frontend.url=http://localhost:4200"
})
@Transactional
class OrderServiceTest {

    @Test
    void createOrder() {
    }

    @Test
    void findAllOrders() {
    }

    @Test
    void findByIdOrder() {
    }

    @Test
    void findByCustomerId() {
    }

    @Test
    void findBySupplierId() {
    }

    @Test
    void findBySupplierIdAndStatus() {
    }

    @Test
    void findByStatus() {
    }

    @Test
    void updateOrderStatus() {
    }
}