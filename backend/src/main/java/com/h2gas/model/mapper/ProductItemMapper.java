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
            productItemDTO.setPriceDelivery(data.getPriceDelivery().doubleValue());
        }
        if (data.getPricePickup() != null) {
            productItemDTO.setPricePickup(data.getPricePickup().doubleValue());
        }
        if (data.getPriceDelivery() != null && data.getPricePickup() != null) {
            productItemDTO.setPrice(data.getPricePickup().doubleValue());
            productItemDTO.setShippingFee(data.getPriceDelivery().subtract(data.getPricePickup()).doubleValue());
        }
        productItemDTO.setFreeDelivery(data.getPriceDelivery() != null && data.getPricePickup() != null
                && data.getPriceDelivery().compareTo(data.getPricePickup()) == 0);

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
        }

        return productItemDTO;
    }

    public ProductItem toEntity(ProductItemDTO dto) {
        return modelMapper.map(dto, ProductItem.class);
    }
}
