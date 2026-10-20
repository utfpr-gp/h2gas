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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private ProductItemMapper novoMapper() {
        ProductItemMapper mapper = new ProductItemMapper();
        ReflectionTestUtils.setField(mapper, "modelMapper", new MapperConfig().modelMapper());
        return mapper;
    }

    private ProductItem produto(String entrega, String retirada) {
        ProductItem productItem = new ProductItem();
        productItem.setPriceDelivery(entrega == null ? null : new BigDecimal(entrega));
        productItem.setPricePickup(retirada == null ? null : new BigDecimal(retirada));
        productItem.setCategoryProduct(new CategoryProduct(77L, "Gás azul", new ProductType(1L, "Gás", null), null, null));
        return productItem;
    }

    @Test
    @DisplayName("HU10: preços iguais (mesmo com escala diferente) sinalizam Entrega Grátis")
    void deveSinalizarFreteGratisQuandoPrecosForemIguais() {
        assertTrue(novoMapper().toDto(produto("110.00", "110.0")).isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: preços diferentes não sinalizam Entrega Grátis")
    void naoDeveSinalizarFreteGratisQuandoPrecosDiferentes() {
        assertFalse(novoMapper().toDto(produto("110.00", "95.00")).isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: preço nulo não quebra o mapeamento nem sinaliza Entrega Grátis")
    void naoDeveQuebrarQuandoAlgumPrecoForNulo() {
        assertFalse(novoMapper().toDto(produto("110.00", null)).isFreeDelivery());
        assertFalse(novoMapper().toDto(produto(null, null)).isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: categoryProductId é o id da categoria do fornecedor, não o do tipo (Gás/Água)")
    void deveExporOIdDaCategoriaDoFornecedor() {
        ProductItemDTO dto = novoMapper().toDto(produto("110.00", "95.00"));

        assertEquals(77L, dto.getCategoryProductId());
        assertEquals("Gás", dto.getType());
    }
}
