package com.h2gas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {
    private String customerId;
    private Long supplierId;
    private String paymentMethod;
    private String note;
    private List<OrderItemRequestDTO> items;
}
