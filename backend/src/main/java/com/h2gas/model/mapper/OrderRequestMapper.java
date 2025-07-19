package com.h2gas.model.mapper;

import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderRequestMapper {
    @Autowired
    private ModelMapper modelMapper;

    public OrderRequestDTO toDto(Order data) {
        OrderRequestDTO orderRequestDTO = modelMapper.map(data, OrderRequestDTO.class);
        if (data.getCustomer().getId() != null) {
            orderRequestDTO.setCustomerId(data.getCustomer().getId());
        }

        if (data.getDistributor().getSupplier().getId() != null) {
            orderRequestDTO.setSupplierId(data.getDistributor().getSupplier().getId());
        }

        if (data.getItems() != null) {
            List<OrderItemRequestDTO> itemsDto = new ArrayList<>();

            for (OrderItem item : data.getItems()) {
                OrderItemRequestDTO orderItemRequestDTO = new OrderItemRequestDTO();
                orderItemRequestDTO.setProductItemId(item.getId());
                orderItemRequestDTO.setQuantity(item.getQuantity());
                itemsDto.add(orderItemRequestDTO);
            }
            orderRequestDTO.setItems(itemsDto);
        }

        return orderRequestDTO;
    }

    public Order toEntity(OrderRequestDTO dto) {
        Order order = modelMapper.map(dto, Order.class);
        if (dto.getCustomerId() != null) {
            User customerUser = new User();
            customerUser.setId(dto.getCustomerId());
            order.setCustomer(customerUser);
        }

        if (dto.getSupplierId() != null) {
            Supplier supplier = new Supplier();
            supplier.setId(dto.getSupplierId());
        }

        if (dto.getItems() != null) {
            List<OrderItem> items = new ArrayList<>();

            for (OrderItemRequestDTO oderItemRequestDto : dto.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setId(oderItemRequestDto.getProductItemId());
                orderItem.setQuantity(oderItemRequestDto.getQuantity());
                items.add(orderItem);
            }

            order.setItems(items);
        }

        return order;
    }
}
