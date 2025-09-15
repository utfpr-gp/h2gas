package com.h2gas.model.mapper;

import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.entity.CategoryProduct;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CategoryProductMapper {
    @Autowired
    private ModelMapper modelMapper;

    public CategoryProductDTO toDto(CategoryProduct data) {
        CategoryProductDTO categoryProductDTO = modelMapper.map(data, CategoryProductDTO.class);
        if (data.getProductType().getName() != null) {
            categoryProductDTO.setType(data.getProductType().getName());
            categoryProductDTO.setProductTypeId(data.getProductType().getId());
        }

        return categoryProductDTO;
    }

    public CategoryProduct toEntity(CategoryProductDTO dto) {
        return modelMapper.map(dto, CategoryProduct.class);
    }
}
