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
        if (data.getPriceDelivery() != null) {
            productItemDTO.setPrice(data.getPriceDelivery().doubleValue());
            productItemDTO.setPriceDelivery(data.getPriceDelivery().doubleValue());
        }
        if (data.getPricePickup() != null) {
            productItemDTO.setPricePickup(data.getPricePickup().doubleValue());
        }
        if (data.getCategoryProduct().getName() != null) {
            productItemDTO.setCategory(data.getCategoryProduct().getName());
        }

        if (data.getCategoryProduct().getProductType().getName() != null) {
            productItemDTO.setType(data.getCategoryProduct().getProductType().getName());
            productItemDTO.setCategoryProductId(data.getCategoryProduct().getProductType().getId());
        }

        if (data.getSupplier() != null) {
            productItemDTO.setSupllierName(data.getSupplier().getName());
            productItemDTO.setSupplierId(data.getSupplier().getId());
        }

        return productItemDTO;
    }

    public ProductItem toEntity(ProductItemDTO dto) {
        return modelMapper.map(dto, ProductItem.class);
    }
}
