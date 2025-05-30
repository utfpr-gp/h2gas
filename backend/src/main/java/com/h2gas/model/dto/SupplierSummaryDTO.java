package com.h2gas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierSummaryDTO {
    private Long id;
    private String name;
    private String phone;
    // Novo formato
    private String openingTime;
    private String closingTime;

    private Long averageDeliveryTime;
    private boolean active;
    private String cityName;
    private Long cityId;
}
