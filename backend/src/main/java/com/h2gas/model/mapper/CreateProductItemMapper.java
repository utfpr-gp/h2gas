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
        dto.setPrice(data.getPriceDelivery());
        return dto;
    }

    public ProductItem toEntity(CreateProductItemDTO dto) {
        ProductItem productItem = modelMapper.map(dto, ProductItem.class);
        productItem.setPriceDelivery(dto.getPrice());
        productItem.setPricePickup(dto.getPrice());
        return productItem;
    }
}
