package com.h2gas.model.mapper;

import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.entity.ProductItem;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateProductItemMapper {
    @Autowired
    private ModelMapper modelMapper;

    public CreateProductItemDTO toDto(ProductItem data) {
        CreateProductItemDTO dto = modelMapper.map(data, CreateProductItemDTO.class);
        dto.setPrice(data.getPricePickup());
        if (data.getPriceDelivery() != null && data.getPricePickup() != null) {
            dto.setShippingFee(data.getPriceDelivery().subtract(data.getPricePickup()));
        }
        return dto;
    }

    public ProductItem toEntity(CreateProductItemDTO dto) {
        ProductItem productItem = modelMapper.map(dto, ProductItem.class);
        applyPrices(productItem, dto);
        return productItem;
    }

    // HU10: retirada = Preço; entrega = Preço + frete (as colunas do banco seguem as mesmas)
    public void applyPrices(ProductItem productItem, CreateProductItemDTO dto) {
        productItem.setPricePickup(dto.getPrice());
        productItem.setPriceDelivery(dto.getPrice().add(dto.getShippingFee()));
    }
}
