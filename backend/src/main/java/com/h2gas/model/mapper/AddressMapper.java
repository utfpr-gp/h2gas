package com.h2gas.model.mapper;

import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.entity.Address;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    @Autowired
    private ModelMapper modelMapper;

    public AddressDTO toDto(Address data) {
        AddressDTO addressDTO = modelMapper.map(data, AddressDTO.class);
        if (data.getCity() != null) {
            addressDTO.setCityName(data.getCity().getName());
        }

        return addressDTO;
    }

    public Address toEntity(AddressDTO data) {
        return modelMapper.map(data, Address.class);
    }

}
