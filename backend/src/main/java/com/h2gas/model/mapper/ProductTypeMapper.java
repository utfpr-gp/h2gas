package com.h2gas.model.mapper;

import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.ProductType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductTypeMapper {
    @Autowired
    private ModelMapper modelMapper;

    public ProductTypeDTO toDto(ProductType data) {
        return modelMapper.map(data, ProductTypeDTO.class);
    }

    public ProductType toEntity(ProductTypeDTO dto) {
        return modelMapper.map(dto, ProductType.class);
    }
}
