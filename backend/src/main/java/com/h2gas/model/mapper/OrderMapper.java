package com.h2gas.model.mapper;

import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderItemResponseDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {
    @Autowired
    private ModelMapper modelMapper;

    public OrderRequestDTO toRequestDto(Order data) {
        OrderRequestDTO orderRequestDTO = modelMapper.map(data, OrderRequestDTO.class);
        if (data.getCustomer().getId() != null) {
            orderRequestDTO.setCustomerId(data.getCustomer().getId());
        }

        if (data.getSupplier().getId() != null) {
            orderRequestDTO.setSupplierId(data.getSupplier().getId());
        }

        if (data.getItems() != null) {
            List<OrderItemRequestDTO> itemsDto = data.getItems().stream()
                    .map(item -> new OrderItemRequestDTO(
                            item.getId(),
                            item.getQuantity()
                    )).toList();

            orderRequestDTO.setItems(itemsDto);
        }

        return orderRequestDTO;
    }

    public OrderResponseDTO toResponseDto(Order data) {
        OrderResponseDTO orderResponseDTO = modelMapper.map(data, OrderResponseDTO.class);

        if (data.getCustomer().getId() != null) {
            orderResponseDTO.setCustomerId(data.getCustomer().getId());
        }

        if (data.getCustomer().getName() != null) {
            orderResponseDTO.setCustomerName(data.getCustomer().getName());
        }

        if (data.getSupplier().getId() != null) {
            orderResponseDTO.setSupplierId(data.getSupplier().getId());
        }

        if (data.getSupplier().getName() != null) {
            orderResponseDTO.setSupplierName(data.getSupplier().getName());
        }

        if (data.getItems() != null) {
            List<OrderItemResponseDTO> itemsDto = data.getItems().stream()
                    .map(item -> new OrderItemResponseDTO(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getBrand(),
                            item.getProduct().getCategoryProduct().getName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getTotalPrice()
                    )).toList();

            orderResponseDTO.setOrderItems(itemsDto);
        }

        return orderResponseDTO;
    }

    public Order toResponseEntity(OrderResponseDTO dto) {
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

        if (dto.getOrderItems() != null) {
            List<OrderItem> items = new ArrayList<>();
            for (OrderItemResponseDTO orderItemResponseDTO : dto.getOrderItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setId(orderItemResponseDTO.getId());
                items.add(orderItem);
            }

            order.setItems(items);
        }


        return null;
    }

    public Order toRequestEntity(OrderRequestDTO dto) {
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
