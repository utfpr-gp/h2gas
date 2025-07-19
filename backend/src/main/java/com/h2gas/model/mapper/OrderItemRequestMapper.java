package com.h2gas.model.mapper;

import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderItemResponseDTO;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.OrderItem;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderItemRequestMapper {
    @Autowired
    private ModelMapper modelMapper;

    public OrderItemRequestDTO toDto(OrderItem data) {
        return modelMapper.map(data, OrderItemRequestDTO.class);
    }

    public OrderItem toEntity(OrderItemRequestDTO dto) {
        return modelMapper.map(dto, OrderItem.class);
    }
}
