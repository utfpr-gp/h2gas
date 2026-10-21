package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.SupplierRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private IbgeClient ibgeClient;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private AddressRepository addressRepository;

    public List<CityDTO> findAllCities() {
        return cityRepository.findAll(Sort.by("state", "name"))
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


    // F7 fatia 6: nome e UF oficiais do IBGE; a cidade antiga sem código conta como duplicada pelo nome + UF
    @Transactional
    public CityDTO createCity(Integer ibgeCode) {
        IbgeClient.Municipality municipality = ibgeClient.findMunicipality(ibgeCode)
                .orElseThrow(() -> new BusinessRuleException("Município não encontrado no IBGE."));
        if (cityRepository.existsByIbgeCode(municipality.code())
                || cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(municipality.name(), municipality.state())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    municipality.name() + " - " + municipality.state() + " já é atendida.");
        }
        City city = new City(null, municipality.name(), municipality.state());
        city.setIbgeCode(municipality.code());
        return cityMapper.toDto(cityRepository.save(city));
    }

    @Transactional
    public void deleteCity(Long id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cidade com id " + id + " não encontrada"));
        if (supplierRepository.existsByCityId(id) || addressRepository.existsByCityId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    city.getName() + " - " + city.getState() + " tem fornecedores ou endereços cadastrados e não pode ser excluída.");
        }

        cityRepository.delete(city);
    }

    public CityDTO findByName(String name) {
        Optional<City> cityEntityOptional = cityRepository.findByName(name);
        return cityEntityOptional.map(cityMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Cidade não encontrada com esse nome: " + name));
    }
}
