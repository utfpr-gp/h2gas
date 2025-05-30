package com.h2gas.model.dto;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDTO {
    private Long id;
    private String name;
    private String phone;
    // Novo formato
    private String openingTime;  // ex: "08:00"
    private String closingTime;  // ex: "18:00"

    private Long averageDeliveryTime;
    private boolean active;
    private String cityName;
    private Long cityId;
    private UserDto user;

    //    address relacionamento
    private String addressNeighborhood;
    private String addressComplement;
    private String addressStreetName;
    private String addressNumber;
    private Long addressId;
}
