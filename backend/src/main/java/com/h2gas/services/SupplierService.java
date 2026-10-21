package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.dto.SupplierSettingsDTO;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Map;
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

//    @Autowired
//    private UserService userService;

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

    public Page<SupplierDTO> searchSuppliers(String name, Pageable pageable) {
        return supplierRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(supplierMapper::toDto);
    }

    public Page<SupplierDTO> findAllSuppliersOpenAndActive(Pageable pageable) {
        return supplierRepository.findByActiveAndOpen(true, true, pageable)
                .map(supplierMapper::toDto);
    }

    public Page<SupplierDTO> findAllSuppliersOpenAndActive(Long cityId, Pageable pageable) {
        Page<Supplier> result = (cityId != null)
                ? supplierRepository.findByCityIdAndActiveAndOpen(cityId, true, true, pageable)
                : supplierRepository.findByActiveAndOpen(true, true, pageable);
        return result.map(supplierMapper::toDto);
    }

    public List<SupplierSummaryDTO> findAllSuppliersSummary() {
        return supplierRepository.findAll()
                .stream()
                .map(supplierSummaryMapper::toDto)
                .collect(Collectors.toList());
    }

    public SupplierDTO findById(String id) {
        Optional<Supplier> supplierOptional = supplierRepository.findById(id);
        return supplierOptional.map(supplierMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + id));
    }

    public Supplier findEntityById(String id) {
        return supplierRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id: " + id));
    }

    public Supplier findEntityByName(String name) {
        return supplierRepository.findByName(name).orElseThrow(()
                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse nome: " + name));
    }

    public SupplierDTO createSupplier(SupplierCreateDTO supplierDTO) {
        Supplier supplier = supplierMapper.toEntitySupplierCreate(supplierDTO);

        City city = cityService.findByIdEntity(supplierDTO.getCityId());
        Address address = null;

        if (city == null) {
            throw new ResourceNotFoundException("Cidade não encontrada.");
        }
        if (supplierDTO.getAddressId() != null) {
             address = addressService.findByIdEntity(supplierDTO.getAddressId());
        }
//        if (address == null) {
//            throw new ResourceNotFoundException("Address not found");
//        }

//        User user = userMapper.toEntityUser(userService.findByIdUser(supplierDTO.getUserId()));

//        if (user == null) {
//            throw new ResourceNotFoundException("User not found");
//        }

        supplier.setCity(city);
        supplier.setAddress(address);
        supplier.setActive(false);
        supplier.setCnpj(supplierDTO.getCnpj());
        supplier.setName(supplierDTO.getName());
//        supplier.setUser(user);
        Supplier savedSupplier = supplierRepository.save(supplier);

        return supplierMapper.toDto(savedSupplier);
    }

    public Supplier createSupplier(Supplier supplier) {
//        if (supplier.getCity() == null || supplier.getAddress() == null) {
//            throw new BusinessRuleException("Cidade e endereço devem estar definidos antes de salvar.");
//        }

        supplier.setActive(false);
        return supplierRepository.save(supplier);
    }


    public SupplierDTO update(String id, SupplierCreateDTO supplierDTO) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com id " + id + " não encontrado"));

        boolean currentStatus = supplier.isActive();
        City newCity = cityMapper.toEntity(cityService.findByIdCity(supplierDTO.getCityId()));
        if (!supplier.getCity().getId().equals(supplierDTO.getCityId())) {
            supplier.setCity(newCity);
        }

        supplier.setName(supplierDTO.getName());
        supplier.setPhoneNumber(supplierDTO.getPhone());
        supplier.setActive(currentStatus);
        supplier.setDescription(supplierDTO.getDescription());
        supplier.setOpeningTime(LocalTime.parse(supplierDTO.getOpeningTime()));
        supplier.setClosingTime(LocalTime.parse(supplierDTO.getClosingTime()));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(supplierDTO.getAverageDeliveryTime()));

        supplierRepository.save(supplier);
        return supplierMapper.toDto(supplier);
    }

    public void deleteSupplier(String id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplierRepository.delete(supplier);
    }

    public void updateStatusSupplier(String id, boolean active) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplier.setActive(active);
        supplierRepository.save(supplier);
    }

//    public void updateTimesSupplier(String id, Map<String, String> times) {
//        Supplier supplier = supplierRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));
//
//        supplier.setOpeningTime(LocalTime.parse(times.get("openingTime")));
//        supplier.setClosingTime(LocalTime.parse(times.get("closingTime")));
//        supplierRepository.save(supplier);
//    }

    public void updateTimesSupplier(String id, Map<String, String> times) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        if (times.containsKey("openingTime") && times.get("openingTime") != null && !times.get("openingTime").isEmpty()) {
            supplier.setOpeningTime(LocalTime.parse(times.get("openingTime")));
        }

        if (times.containsKey("closingTime") && times.get("closingTime") != null && !times.get("closingTime").isEmpty()) {
            supplier.setClosingTime(LocalTime.parse(times.get("closingTime")));
        }

        if (times.containsKey("averageDeliveryTime") && times.get("averageDeliveryTime") != null && !times.get("averageDeliveryTime").isEmpty()) {
            try {
                long minutes = Long.parseLong(times.get("averageDeliveryTime"));
                supplier.setAverageDeliveryTime(Duration.ofMinutes(minutes));
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("Tempo médio de entrega inválido: informe os minutos.");
            }
        }
        supplierRepository.save(supplier);
    }



    // F7 fatia 6: configurações da loja; o id vem sempre do token (controller)
    public SupplierSettingsDTO findMySettings(String supplierId) {
        return toSettings(findSupplier(supplierId));
    }

    @Transactional
    public SupplierSettingsDTO updateMySettings(String supplierId, SupplierSettingsDTO settings) {
        LocalTime opening = LocalTime.parse(settings.openingTime());
        LocalTime closing = LocalTime.parse(settings.closingTime());
        if (opening.equals(closing)) {
            throw new BusinessRuleException("O horário de fechamento precisa ser diferente da abertura.");
        }
        Supplier supplier = findSupplier(supplierId);
        supplier.setOpeningTime(opening);
        supplier.setClosingTime(closing);
        supplier.setAverageDeliveryTime(Duration.ofMinutes(settings.averageDeliveryTime()));
        supplier.setLowStockThreshold(settings.lowStockThreshold());
        return toSettings(supplierRepository.save(supplier));
    }

    private Supplier findSupplier(String id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));
    }

    private static SupplierSettingsDTO toSettings(Supplier supplier) {
        return new SupplierSettingsDTO(
                supplier.getOpeningTime() == null ? null : supplier.getOpeningTime().toString(),
                supplier.getClosingTime() == null ? null : supplier.getClosingTime().toString(),
                supplier.getAverageDeliveryTime() == null ? null : (int) supplier.getAverageDeliveryTime().toMinutes(),
                supplier.getLowStockThreshold());
    }

    public void updateStatusOpenClosed(String id, boolean openClose) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor com " + id + " não encontrado"));

        supplier.setOpen(openClose);
        supplierRepository.save(supplier);
    }

//    public SupplierDTO findByUserIdSupplier(String userId) {
//        Optional<Supplier> supplierOptional = supplierRepository.findByUserId(userId);
//        return supplierOptional.map(supplierMapper::toDto).orElseThrow(()
//                -> new ResourceNotFoundException("Não encontrado nenhum Fornecedor com esse id de usuario: " + userId));
//    }
}
