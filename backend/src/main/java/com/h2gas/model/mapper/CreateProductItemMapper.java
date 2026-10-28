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
        return modelMapper.map(data, CreateProductItemDTO.class);
    }

    public ProductItem toEntity(CreateProductItemDTO dto) {
        ProductItem productItem = modelMapper.map(dto, ProductItem.class);
        applyPrices(productItem, dto);
        return productItem;
    }

    // HU21: um preço só
    public void applyPrices(ProductItem productItem, CreateProductItemDTO dto) {
        productItem.setPrice(dto.getPrice());
    }
}
