package com.h2gas.model.mapper;

import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderItemResponseDTO;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {
    @Autowired
    private ModelMapper modelMapper;

    public OrderItemRequestDTO toRequestDto(OrderItem data) {
        OrderItemRequestDTO orderItemRequestDTO = modelMapper.map(data, OrderItemRequestDTO.class);
        if (data.getProduct() != null) {
            orderItemRequestDTO.setProductItemRef(data.getProduct().getId());
        }

        return orderItemRequestDTO;
    }

    public OrderItemResponseDTO toResponseDto(OrderItem data) {
        OrderItemResponseDTO orderItemResponseDTO = modelMapper.map(data, OrderItemResponseDTO.class);
        if (data.getProduct() != null) {
            orderItemResponseDTO.setProductBrand(data.getProduct().getBrand());
            orderItemResponseDTO.setProductNameCategory(data.getProduct().getCategoryProduct().getName());
            orderItemResponseDTO.setProductId(data.getProduct().getId());
        }

        return orderItemResponseDTO;
    }

    public OrderItem toResponseEntity(OrderItemResponseDTO dto) {
        OrderItem orderItem = modelMapper.map(dto, OrderItem.class);
        if (dto.getProductId() != null) {
            ProductItem productItem = new ProductItem();
            productItem.setId(dto.getProductId());
            orderItem.setProduct(productItem);
        }

        return orderItem;
    }

    public OrderItem toRequestEntity(OrderItemRequestDTO dto) {
        OrderItem orderItem = modelMapper.map(dto, OrderItem.class);

        if (dto.getProductItemRef() != null) {
            ProductItem productItem = new ProductItem();
            productItem.setId(dto.getProductItemRef());
            orderItem.setProduct(productItem);
        }

        return orderItem;
    }
}
