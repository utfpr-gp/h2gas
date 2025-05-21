package com.h2gas.model.mapper;

import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.User;
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
        return modelMapper.map(dto, ProductItem.class);
    }
}
