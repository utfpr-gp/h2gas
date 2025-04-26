package com.h2gas.model.mapper;

import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Supplier;
import com.h2gas.services.SupplierService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserMapper userMapper;

    public SupplierDTO toDto(Supplier data) {
         return assemblerRelationships(
                    modelMapper.map(data, SupplierDTO.class),
                    data
        );
    }

    public Supplier toEntity(SupplierDTO data) {
        return modelMapper.map(data, Supplier.class);
    }

    public SupplierDTO assemblerRelationships(SupplierDTO supplierDTO, Supplier supplier) {
        if (supplier.getCity() != null) {
            supplierDTO.setCityName(supplier.getCity().getName());
        }

        if (supplier.getAddress() != null) {
            supplierDTO.setAddressNeighborhood(supplier.getAddress().getNeighborhood());
            supplierDTO.setAddressComplement(supplier.getAddress().getComplement());
            supplierDTO.setAddressStreetName(supplier.getAddress().getStreetName());
            supplierDTO.setAddressNumber(supplier.getAddress().getNumber());
        }

        if (supplier.getUser() != null) {
            UserDto userDto = userMapper.toDto(supplier.getUser());
            supplierDTO.setUser(userDto);
        }

        return supplierDTO;
    }

}
