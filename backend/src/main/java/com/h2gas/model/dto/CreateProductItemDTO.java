package com.h2gas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductItemDTO {
    private Long id;
    private String brand;
    private Double price;
    private Long categoryProductId;
    private Long distributorId;
}
