package com.h2gas.model.dto;

import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDTO {
    private Long id;
    private String name;
    private String phone;
    private String workingHours;
    private String averageDeliveryTime;
    private boolean active;
    private String cityName;
    private UserDto user;
    // ID do usuário para criação
    private String userId;

    //    address relacionamento
    private String addressNeighborhood;
    private String addressComplement;
    private String addressStreetName;
    private String addressNumber;
    private Long addressId;
}
