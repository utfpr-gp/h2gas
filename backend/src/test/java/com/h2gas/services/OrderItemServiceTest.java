package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.model.entity.ProductItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemServiceTest {

    private ProductItem produtoComPrecosDistintos() {
        ProductItem productItem = new ProductItem();
        productItem.setPriceDelivery(new BigDecimal("110.00"));
        productItem.setPricePickup(new BigDecimal("95.00"));
        return productItem;
    }

    @Test
    @DisplayName("HU03: deve usar o preço de entrega quando a modalidade for DELIVERY")
    void deveUsarPrecoDeEntregaQuandoModalidadeForDelivery() {
        BigDecimal unitPrice = OrderItemService.unitPriceFor(produtoComPrecosDistintos(), DeliveryModeEnum.DELIVERY);

        assertEquals(new BigDecimal("110.00"), unitPrice);
    }

    @Test
    @DisplayName("HU03: deve usar o preço de retirada quando a modalidade for PICKUP")
    void deveUsarPrecoDeRetiradaQuandoModalidadeForPickup() {
        BigDecimal unitPrice = OrderItemService.unitPriceFor(produtoComPrecosDistintos(), DeliveryModeEnum.PICKUP);

        assertEquals(new BigDecimal("95.00"), unitPrice);
    }
}
