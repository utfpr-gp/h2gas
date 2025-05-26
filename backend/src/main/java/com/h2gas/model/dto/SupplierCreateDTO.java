package com.h2gas.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierCreateDTO {
    private Long id;
    private String name;
    private String phone;
    private String openingTime;
    private String closingTime;
    private Long averageDeliveryTime;
//    private boolean active;
    private Long cityId;
    private String userId;
    private Long addressId;
}
