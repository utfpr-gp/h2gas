package com.h2gas.model.mapper;

import java.math.BigDecimal;

import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderItemResponseDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.entity.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderMapper {
    @Autowired
    private ModelMapper modelMapper;

    public OrderRequestDTO toRequestDto(Order data) {
        OrderRequestDTO orderRequestDTO = modelMapper.map(data, OrderRequestDTO.class);
        if (data.getCustomer().getId() != null) {
            orderRequestDTO.setCustomerUuid(data.getCustomer().getId());
        }

        if (data.getSupplier().getId() != null) {
            orderRequestDTO.setSupplierRef(data.getSupplier().getId());
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
        BigDecimal fee = data.getDeliveryFee() == null ? BigDecimal.ZERO : data.getDeliveryFee();
        orderResponseDTO.setDeliveryFee(fee);
        orderResponseDTO.setSubtotal(data.getTotalPrice() == null ? null : data.getTotalPrice().subtract(fee));

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

        if (data.getStatus() != null) {
            orderResponseDTO.setStatus(data.getStatus().getStatus());
        }

        if (data.getCustomer() != null) {
            orderResponseDTO.setCustomerPhone(data.getCustomer().getPhoneNumber());
        }
        orderResponseDTO.setCancellationReason(data.getCancellationReason());
        if (data.getDeliverer() != null) {
            orderResponseDTO.setDelivererName(data.getDeliverer().getUser().getName());
        }

        if (data.getDeliveryMode() != null) {
            orderResponseDTO.setDeliveryMode(data.getDeliveryMode().name());
        }

        if (data.getDeliveryAddress() != null) {
            orderResponseDTO.setNumber(data.getDeliveryAddress().getNumber());
            orderResponseDTO.setStreetName(data.getDeliveryAddress().getStreetName());
            orderResponseDTO.setComplement(data.getDeliveryAddress().getComplement());
            orderResponseDTO.setNeighborhood(data.getDeliveryAddress().getNeighborhood());
            if (data.getDeliveryAddress().getCity() != null) {
                orderResponseDTO.setCityName(data.getDeliveryAddress().getCity().getName());
            }
        }

        // HU07: a recusa mais recente só interessa enquanto o pedido espera um novo entregador
        if (data.getDeliverer() == null && data.getRefusals() != null && !data.getRefusals().isEmpty()) {
            DeliveryRefusal lastRefusal = data.getRefusals().get(0);
            orderResponseDTO.setLastRefusalReason(lastRefusal.getReason());
            orderResponseDTO.setLastRefusalBy(lastRefusal.getDeliverer().getUser().getName());
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
        // o ModelMapper casa addressId com Order.id; pedido novo nunca tem id,
        // e o endereço de entrega é resolvido (com checagem de posse) no OrderService
        order.setId(null);
        order.setDeliveryAddress(null);
        if (dto.getCustomerUuid() != null) {
            User customerUser = new User();
            customerUser.setId(dto.getCustomerUuid());
            order.setCustomer(customerUser);
        }

        if (dto.getSupplierRef() != null) {
            Supplier supplier = new Supplier();
            supplier.setId(dto.getSupplierRef());
        }

        return order;
    }
}
