package com.h2gas.model.mapper;

import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderItemResponseDTO;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderItemRequestMapper {
    @Autowired
    private ModelMapper modelMapper;

    public OrderItemRequestDTO toDto(OrderItem data) {
        OrderItemRequestDTO orderItemRequestDTO = modelMapper.map(data, OrderItemRequestDTO.class);
        if (data.getProduct() != null) {
            orderItemRequestDTO.setProductItemId(data.getProduct().getId());
        }

        return orderItemRequestDTO;
    }

    public OrderItem toEntity(OrderItemRequestDTO dto) {
        OrderItem orderItem = modelMapper.map(dto, OrderItem.class);

        if (dto.getProductItemId() != null) {
            ProductItem productItem = new ProductItem();
            productItem.setId(dto.getProductItemId());
        }

        return orderItem;
    }
}
