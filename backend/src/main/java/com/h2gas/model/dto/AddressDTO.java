package com.h2gas.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.h2gas.model.entity.City;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO {
    private Long id;
    private String streetName;
    private String number;
    private String complement;
    private String neighborhood;
    private String zipCode;
    private String cityName;
}
