package com.h2gas.services;

import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.model.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CityService {
    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CityMapper cityMapper;

    public List<CityDTO> findAllCities() {
        return cityRepository.findAll()
                .stream()
                .map(cityMapper::toDto)
                .collect(Collectors.toList());
    }

    public CityDTO findByIdCity(Long id) {
        Optional<City> cityEntityOptional = cityRepository.findById(id);
        return cityEntityOptional.map(cityMapper::toDto).orElseThrow(()
                -> new RuntimeException("Cidade não encontra com esse id: " + id));
    }

    public CityDTO createCity(CityDTO cityDTO) {
        City city = cityMapper.toEntity(cityDTO);
        cityRepository.save(city);
        return cityMapper.toDto(city);
    }

    public void deleteCity(Long id) {
        Optional<City> cityEntityOptional = cityRepository.findById(id);
        if (cityEntityOptional.isPresent()) {
            cityRepository.deleteById(id);
        }
    }

    public CityDTO findByName(String name) {
        Optional<City> cityEntityOptional = cityRepository.findByName(name);
        return cityEntityOptional.map(cityMapper::toDto).orElseThrow(()
                -> new RuntimeException("Cidade não encontrada com esse nome: " + name));
    }
}
