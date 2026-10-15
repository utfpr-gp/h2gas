package com.h2gas.model.mapper;

import com.h2gas.configuration.mapperConfig.MapperConfig;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.ProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductItemMapperTest {

    @Test
    @DisplayName("HU03: deve expor o preço de entrega e o de retirada no DTO do produto")
    void deveExporPrecoDeEntregaEDeRetirada() {
        ProductItemMapper mapper = new ProductItemMapper();
        ReflectionTestUtils.setField(mapper, "modelMapper", new MapperConfig().modelMapper());

        ProductItem productItem = new ProductItem();
        productItem.setPriceDelivery(new BigDecimal("110.00"));
        productItem.setPricePickup(new BigDecimal("95.00"));
        productItem.setCategoryProduct(new CategoryProduct(null, "Gás azul", new ProductType(null, "Gás", null), null, null));

        ProductItemDTO dto = mapper.toDto(productItem);

        assertEquals(110.00, dto.getPriceDelivery());
        assertEquals(95.00, dto.getPricePickup());
    }
}
