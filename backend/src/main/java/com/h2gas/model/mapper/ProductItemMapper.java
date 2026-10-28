package com.h2gas.model.mapper;

import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.entity.ProductItem;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductItemMapper {
    @Autowired
    private ModelMapper modelMapper;

//    @Autowired
//    private SupplierMapper supplierMapper;

    public ProductItemDTO toDto(ProductItem data) {
        ProductItemDTO productItemDTO = modelMapper.map(data, ProductItemDTO.class);
        productItemDTO.setPrice(data.getPrice() != null ? data.getPrice().doubleValue() : null);

        if (data.getCategoryProduct().getName() != null) {
            productItemDTO.setCategory(data.getCategoryProduct().getName());
        }

        if (data.getCategoryProduct().getProductType().getName() != null) {
            productItemDTO.setType(data.getCategoryProduct().getProductType().getName());
        }
        // id da categoria do fornecedor (e não o do tipo): o formulário de edição reenvia esse valor
        productItemDTO.setCategoryProductId(data.getCategoryProduct().getId());

        if (data.getSupplier() != null) {
            productItemDTO.setSupllierName(data.getSupplier().getName());
            productItemDTO.setSupplierId(data.getSupplier().getId());
            productItemDTO.setSupplierLogoUrl(data.getSupplier().getLogoUrl());
        }

        return productItemDTO;
    }

    public ProductItem toEntity(ProductItemDTO dto) {
        return modelMapper.map(dto, ProductItem.class);
    }
}
