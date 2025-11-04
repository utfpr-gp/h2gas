package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.model.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<CityDTO> findAllCities(Pageable pageable) {
        return cityRepository.findAll(pageable)
                .map(cityMapper::toDto);
    }

    public CityDTO findByIdCity(Long id) {
        Optional<City> cityEntityOptional = cityRepository.findById(id);
        return cityEntityOptional.map(cityMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Cidade não encontra com esse id: " + id));
    }

    public City findByIdEntity(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
    }


    public CityDTO createCity(CityDTO cityDTO) {
        City city = cityMapper.toEntity(cityDTO);
        if (cityRepository.findByName(cityDTO.getName()).isPresent()) {
            throw new ResourceNotFoundException("Já existe uma cidade com esse nome.");
        }
        cityRepository.save(city);
        return cityMapper.toDto(city);
    }

    public void deleteCity(Long id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidade com id " + id + " não encontrada"));

        cityRepository.delete(city);
    }

    public CityDTO findByName(String name) {
        Optional<City> cityEntityOptional = cityRepository.findByName(name);
        return cityEntityOptional.map(cityMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Cidade não encontrada com esse nome: " + name));
    }
}
