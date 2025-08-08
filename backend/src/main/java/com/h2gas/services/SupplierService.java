package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierSummaryDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.*;
import com.h2gas.model.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
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

    @Autowired
    private SupplierSummaryMapper supplierSummaryMapper;

//    public List<SupplierDTO> findAllSuppliers() {
//        return supplierRepository.findAll()
//                .stream()
//                .map(supplierMapper::toDto)
//                .collect(Collectors.toList());
//    }

    public Page<SupplierDTO> findAllSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable)
                .map(supplierMapper::toDto);
    }

    public Page<SupplierDTO> findAllSuppliersOpenAndActive(Pageable pageable) {
        return supplierRepository.findByActiveAndOpen(true, true, pageable)
                .map(supplierMapper::toDto);
    }

    public List<SupplierSummaryDTO> findAllSuppliersSummary() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierSummaryMapper::toDto)
                .collect(Collectors.toList());
    }

    public SupplierDTO findById(Long id) {
        Optional<Supplier> supplierOptional = supplierRepository.findById(id);
        return supplierOptional.map(supplierMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + id));
    }

    public Supplier findEntityById(Long id) {
        return supplierRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + id));
    }

    public SupplierDTO createSupplier(SupplierCreateDTO supplierDTO) {
        Supplier supplier = supplierMapper.toEntitySupplierCreate(supplierDTO);

        City city = cityMapper.toEntity(cityService.findByIdCity(supplierDTO.getCityId()));
        if (city == null) {
            throw new ResourceNotFoundException("City not found");
        }
        Address address = addressMapper.toEntity(addressService.findByIdAddress(
                supplierDTO.getAddressId())
        );

        if (address == null) {
            throw new ResourceNotFoundException("Address not found");
        }

        User user = userMapper.toEntityUser(userService.findByIdUser(supplierDTO.getUserId()));

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        supplier.setCity(city);
        supplier.setAddress(address);
        supplier.setActive(false);
        supplier.setUser(user);
        Supplier savedSupplier = supplierRepository.save(supplier);

        return supplierMapper.toDto(savedSupplier);
    }

    public SupplierDTO update(Long id, SupplierCreateDTO supplierDTO) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com id " + id + " não encontrado"));

        boolean currentStatus = supplier.isActive();
        City newCity = cityMapper.toEntity(cityService.findByIdCity(supplierDTO.getCityId()));
        if (!supplier.getCity().getId().equals(supplierDTO.getCityId())) {
            supplier.setCity(newCity);
        }

        supplier.setName(supplierDTO.getName());
        supplier.setPhone(supplierDTO.getPhone());
        supplier.setActive(currentStatus);
        supplier.setOpeningTime(LocalTime.parse(supplierDTO.getOpeningTime()));
        supplier.setClosingTime(LocalTime.parse(supplierDTO.getClosingTime()));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(supplierDTO.getAverageDeliveryTime()));

        supplierRepository.save(supplier);
        return supplierMapper.toDto(supplier);
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplierRepository.delete(supplier);
    }

    public void updateStatusSupplier(Long id, boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplier.setActive(active);
        supplierRepository.save(supplier);
    }

    public void updateStatusOpenClosed(Long id, boolean openClose) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplier.setOpen(openClose);
        supplierRepository.save(supplier);
    }

    public SupplierDTO findByUserIdSupplier(String userId) {
        Optional<Supplier> supplierOptional = supplierRepository.findByUserId(userId);
        return supplierOptional.map(supplierMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id de usuario: " + userId));
    }
}
