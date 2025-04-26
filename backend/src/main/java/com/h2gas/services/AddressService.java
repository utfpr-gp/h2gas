package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.mapper.AddressMapper;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.CityRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AddressService {
    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private CityService cityService;

    @Autowired
    private CityMapper cityMapper;

    public List<AddressDTO> findAllAddresses() {
        return addressRepository.findAll()
                .stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());
    }

    public AddressDTO findByIdAddress(Long id) {
        Optional<Address> adrresEntityOptional = addressRepository.findById(id);
        return adrresEntityOptional.map(addressMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public AddressDTO createAddress(AddressDTO addressDTO) {
        City city = cityMapper.toEntity(cityService.findByName(addressDTO.getCityName()));
        Address address = addressMapper.toEntity(addressDTO);
        address.setCity(city);
        addressRepository.save(address);
        return addressMapper.toDto(address);
    }

    public AddressDTO findByStreetName(String streetName) {
        Optional<Address> addressOptional = addressRepository.findByStreetName(streetName);
        return addressOptional.map(addressMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Endereço não encontrada com esse nome de rua: " + streetName));
    }

    public AddressDTO updateAddress(AddressDTO addressDTO) {
        Address addressExisting = addressRepository.findById(addressDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        City city = cityMapper.toEntity(cityService.findByName(addressDTO.getCityName()));
        addressExisting.setStreetName(addressDTO.getStreetName());
        addressExisting.setNumber(addressDTO.getNumber());
        addressExisting.setComplement(addressDTO.getComplement());
        addressExisting.setNeighborhood(addressDTO.getNeighborhood());
        addressExisting.setZipCode(addressDTO.getZipCode());
        addressExisting.setCity(city);
        addressRepository.save(addressExisting);

        Address updatedAddress = addressRepository.save(addressExisting);
        return addressMapper.toDto(updatedAddress);
    }

}
