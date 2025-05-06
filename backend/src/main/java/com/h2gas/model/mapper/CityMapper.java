package com.h2gas.model.mapper;

import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CityMapper {
    @Autowired
    private ModelMapper modelMapper;

    public CityDTO toDto(City data) {
        return modelMapper.map(data, CityDTO.class);
    }

    public City toEntity(CityDTO dto) {
        return modelMapper.map(dto, City.class);
    }
}
