package com.h2gas.model.mapper;

import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierSummaryDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.Supplier;
import com.h2gas.util.SupplierMapperHelper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class SupplierSummaryMapper {
    @Autowired
    private ModelMapper modelMapper;

    public SupplierSummaryDTO toDto(Supplier data) {
        return assemblerRelationships(
                modelMapper.map(data, SupplierSummaryDTO.class),
                data
        );
    }

    public Supplier toEntity(SupplierSummaryDTO dto) {
        return mapCommonFields(dto);
    }

    public SupplierSummaryDTO assemblerRelationships(SupplierSummaryDTO supplierDTO, Supplier supplier) {
        if (supplier.getCity() != null) {
            supplierDTO.setCityName(supplier.getCity().getName());
            supplierDTO.setCityId(supplier.getCity().getId());
        }

        if (supplier.getOpeningTime() != null) {
            supplierDTO.setOpeningTime(supplier.getOpeningTime().toString());
        }

        if (supplier.getClosingTime() != null) {
            supplierDTO.setClosingTime(supplier.getClosingTime().toString());
        }

        if (supplier.getAverageDeliveryTime() != null) {
            supplierDTO.setAverageDeliveryTime(supplier.getAverageDeliveryTime().toMinutes());
        }

        return supplierDTO;
    }


    private Supplier mapCommonFields(SupplierSummaryDTO dto) {
        Supplier supplier = modelMapper.map(dto, Supplier.class);

        String openingTime = dto.getOpeningTime();
        String closingTime = dto.getClosingTime();
        Long averageDeliveryTime = dto.getAverageDeliveryTime();

        SupplierMapperHelper.applyTimeFields(
                supplier,
                openingTime,
                closingTime,
                averageDeliveryTime
        );

        return supplier;
    }


}
