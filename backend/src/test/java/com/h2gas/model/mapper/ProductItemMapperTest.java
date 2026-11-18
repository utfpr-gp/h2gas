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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductItemMapperTest {

    private ProductItemMapper novoMapper() {
        ProductItemMapper mapper = new ProductItemMapper();
        ReflectionTestUtils.setField(mapper, "modelMapper", new MapperConfig().modelMapper());
        return mapper;
    }

    private ProductItem produto(String preco) {
        ProductItem productItem = new ProductItem();
        productItem.setPrice(preco == null ? null : new BigDecimal(preco));
        productItem.setCategoryProduct(new CategoryProduct(77L, "Gás azul", new ProductType(1L, "Gás", null), null));
        return productItem;
    }

    @Test
    @DisplayName("HU21: o produto tem um preço só, sem frete embutido")
    void deveExporUmPrecoSo() {
        ProductItemDTO dto = novoMapper().toDto(produto("95.00"));

        assertEquals(95.0, dto.getPrice());
        java.util.Set<String> campos = java.util.Arrays.stream(ProductItemDTO.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName).collect(java.util.stream.Collectors.toSet());
        assertFalse(campos.contains("priceDelivery"));
        assertFalse(campos.contains("pricePickup"));
        assertFalse(campos.contains("shippingFee"));
        assertFalse(campos.contains("freeDelivery"));
    }

    @Test
    @DisplayName("HU21: preço nulo não quebra o mapeamento")
    void naoDeveQuebrarComPrecoNulo() {
        assertNull(novoMapper().toDto(produto(null)).getPrice());
    }

    @Test
    @DisplayName("HU10: categoryProductId é o id da categoria do fornecedor, não o do tipo (Gás/Água)")
    void deveExporOIdDaCategoriaDoFornecedor() {
        ProductItemDTO dto = novoMapper().toDto(produto("95.00"));

        assertEquals(77L, dto.getCategoryProductId());
        assertEquals("Gás", dto.getType());
    }

    // ---- HU15 §5.5: o card de produto mostra a logo redonda da loja que vende ----

    @Test
    @DisplayName("HU15: o produto leva a logo da loja que vende")
    void deveLevarALogoDaLojaQueVende() {
        ProductItem productItem = produto("95.00");
        com.h2gas.model.entity.Supplier loja = new com.h2gas.model.entity.Supplier();
        loja.setName("Gás Centro");
        loja.setLogoUrl("https://res.cloudinary.com/h2gas/logo.png");
        productItem.setSupplier(loja);

        ProductItemDTO dto = novoMapper().toDto(productItem);

        assertEquals("https://res.cloudinary.com/h2gas/logo.png", dto.getSupplierLogoUrl());
        assertEquals("Gás Centro", dto.getSupllierName());
    }

    @Test
    @DisplayName("HU15: loja sem logo deixa a logo do produto nula")
    void deveDeixarALogoNulaSemLogo() {
        ProductItem productItem = produto("95.00");
        com.h2gas.model.entity.Supplier loja = new com.h2gas.model.entity.Supplier();
        loja.setName("Sem Logo");
        productItem.setSupplier(loja);

        assertNull(novoMapper().toDto(productItem).getSupplierLogoUrl());
    }
}
