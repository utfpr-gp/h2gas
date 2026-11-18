package com.h2gas.services;

import com.h2gas.model.entity.ProductItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemServiceTest {

    @Test
    @DisplayName("HU21: o preço unitário é o mesmo na entrega e na retirada")
    void deveUsarOMesmoPrecoNaEntregaENaRetirada() {
        ProductItem productItem = new ProductItem();
        productItem.setPrice(new BigDecimal("95.00"));

        assertEquals(new BigDecimal("95.00"), OrderItemService.unitPriceFor(productItem));
    }
}
