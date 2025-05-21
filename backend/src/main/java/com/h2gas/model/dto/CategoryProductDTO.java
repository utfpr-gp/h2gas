package com.h2gas.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryProductDTO {
    private Long id;
    private String name;
    private String type;
    private Long productTypeId;
}
