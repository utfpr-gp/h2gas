package com.h2gas.model.mapper;

import com.h2gas.model.dto.*;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.Supplier;
import com.h2gas.services.SupplierService;
import com.h2gas.util.SupplierMapperHelper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class SupplierMapper {
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserMapper userMapper;

//    @Autowired
//    private ProductItemMapper productItemMapper;

    public SupplierDTO toDto(Supplier data) {
         return assemblerRelationships(
                    modelMapper.map(data, SupplierDTO.class),
                    data
        );
    }

    public Supplier toEntity(SupplierDTO dto) {
        return mapCommonFields(dto);
    }

    public Supplier toEntitySupplierCreate(SupplierCreateDTO dto) {
        return mapCommonFields(dto);
    }

    public Supplier toEntitySupplierSummary(SupplierSummaryDTO dto) {
        return mapCommonFields(dto);
    }

    private Supplier mapCommonFields(Object dto) {
        Supplier supplier = modelMapper.map(dto, Supplier.class);

        String openingTime = null;
        String closingTime = null;
        Long averageDeliveryTime = null;

        if (dto instanceof SupplierDTO d) {
            openingTime = d.getOpeningTime();
            closingTime = d.getClosingTime();
            averageDeliveryTime = d.getAverageDeliveryTime();
        } else if (dto instanceof SupplierCreateDTO d || dto instanceof SupplierSummaryDTO) {
            SupplierCreateDTO d1 = (SupplierCreateDTO) dto;
            openingTime = d1.getOpeningTime();
            closingTime = d1.getClosingTime();
            averageDeliveryTime = d1.getAverageDeliveryTime();
        }

        SupplierMapperHelper.applyTimeFields(
                supplier,
                openingTime,
                closingTime,
                averageDeliveryTime
        );

        return supplier;
    }



    public SupplierDTO assemblerRelationships(SupplierDTO supplierDTO, Supplier supplier) {
        if (supplier.getCity() != null) {
            supplierDTO.setCityName(supplier.getCity().getName());
            supplierDTO.setCityId(supplier.getCity().getId());
        }

        if (supplier.getAddress() != null) {
            supplierDTO.setAddressNeighborhood(supplier.getAddress().getNeighborhood());
            supplierDTO.setAddressComplement(supplier.getAddress().getComplement());
            supplierDTO.setAddressStreetName(supplier.getAddress().getStreetName());
            supplierDTO.setAddressNumber(supplier.getAddress().getNumber());
            supplierDTO.setAddressId(supplier.getAddress().getId());
        }

//        if (supplier.getUser() != null) {
//            UserDto userDto = userMapper.toDto(supplier.getUser());
//            supplierDTO.setUser(userDto);
//        }

        if (supplier.getOpeningTime() != null) {
            supplierDTO.setOpeningTime(supplier.getOpeningTime().toString());
        }

        if (supplier.getClosingTime() != null) {
            supplierDTO.setClosingTime(supplier.getClosingTime().toString());
        }

        if (supplier.getAverageDeliveryTime() != null) {
            supplierDTO.setAverageDeliveryTime(supplier.getAverageDeliveryTime().toMinutes());
        }

//        if (supplier.getProductItems() != null) {
//            List<ProductItemDTO> productDTOs = new ArrayList<>();
//            for(ProductItem item: supplier.getProductItems()) {
//                productDTOs.add(productItemMapper.toDto(item));
//            }
//            supplierDTO.setProductItems(productDTOs);
//        }

//        if (supplier.getAverageDeliveryTime() != null) {
//            supplierDTO.setAverageDeliveryTime(formatDuration(supplier.getAverageDeliveryTime()));
//        }

        return supplierDTO;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%dh %02dmin", hours, minutes);
        } else {
            return String.format("%dmin", minutes);
        }
    }

}
