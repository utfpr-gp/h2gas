package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.UserDto;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.AddressMapper;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private CityMapper cityMapper;

    @Autowired
    private CityService cityService;

    @Autowired
    private AddressMapper addressMapper;

    @Autowired
    private AddressService addressService ;


    public List<SupplierDTO> findAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierMapper::toDto)
                .collect(Collectors.toList());
    }

    public SupplierDTO findById(Long id) {
        Optional<Supplier> supplierOptional = supplierRepository.findById(id);
        return supplierOptional.map(supplierMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + id));
    }

    public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
        City city = cityMapper.toEntity(cityService.findByName(supplierDTO.getCityName()));
        Address address = addressMapper.toEntity(addressService.findByStreetName(
                supplierDTO.getAddressStreetName())
        );

        User user = userMapper.toEntityUser(userService.findByName(supplierDTO.getUser().getName()));
        Supplier supplier = supplierMapper.toEntity(supplierDTO);
        supplier.setCity(city);
        supplier.setAddress(address);
        supplier.setUser(user);
        supplierRepository.save(supplier);

        return supplierMapper.toDto(supplier);
    }
}
