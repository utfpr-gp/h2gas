package com.h2gas.configuration.mapperConfig;

import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierSummaryDTO;
import com.h2gas.model.entity.Supplier;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
//        return new ModelMapper();
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.typeMap(Supplier.class, SupplierDTO.class)
                .addMappings(mapper -> mapper.skip(SupplierDTO::setAverageDeliveryTime));

        modelMapper.typeMap(Supplier.class, SupplierSummaryDTO.class)
                .addMappings(mapper -> mapper.skip(SupplierSummaryDTO::setAverageDeliveryTime));
        return modelMapper;
    }
}
