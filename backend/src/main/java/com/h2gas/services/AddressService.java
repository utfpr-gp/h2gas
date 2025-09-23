package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.AddressMapper;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.AddressRepository;
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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    public List<AddressDTO> findAllAddresses() {
        return addressRepository.findAll()
                .stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());
    }

    public AddressDTO findByIdAddress(Long id) {
        Optional<Address> adrresEntityOptional = addressRepository.findById(id);
        return adrresEntityOptional.map(addressMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Endereço não encontrado com esse id: " + id));
    }

    public AddressDTO createAddress(AddressDTO addressDTO) {
        Address address = addressMapper.toEntity(addressDTO);
        City city = cityMapper.toEntity(cityService.findByName(addressDTO.getCityName()));
        User user = userMapper.toEntityUser(userService.findByIdUser(addressDTO.getUserId()));
        address.setCity(city);
        address.setUser(user);
        addressRepository.save(address);
        return addressMapper.toDto(address);
    }

    public AddressDTO findByStreetName(String streetName) {
        Optional<Address> addressOptional = addressRepository.findByStreetName(streetName);
        return addressOptional.map(addressMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Endereço não encontrada com esse nome de rua: " + streetName));
    }

    public AddressDTO updateAddress(Long id,AddressDTO addressDTO) {
        Address addressExisting = addressRepository.findById(id)
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

    public AddressDTO findByUserIdAddress(String userId) {
        Optional<Address> addressOptional = addressRepository.findByUserId(userId);
        return addressOptional.map(addressMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Endereço não encontrada para esse usuario com esse id: " + userId));

    }

    public Address findEntityByUserId(String userId) {
        return addressRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado para esse usuário com id: " + userId));
    }


}
