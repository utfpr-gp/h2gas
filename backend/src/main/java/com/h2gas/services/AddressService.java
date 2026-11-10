package com.h2gas.services;

import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.AddressMapper;
import com.h2gas.model.mapper.CityMapper;
import com.h2gas.model.mapper.UserMapper;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.UserRepository;
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
    private UserRepository userRepository;

    // HU17: coordenadas do CEP gravadas junto do endereço
    @Autowired
    private GeocodingService geocodingService;

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

    public Address findByIdEntity(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com esse id: " + id));
    }

    public AddressDTO createAddress(AddressDTO addressDTO) {
        Address address = addressMapper.toEntity(addressDTO);
        City city;
        if (addressDTO.getCityId() != null) {
            city = cityService.findByIdEntity(addressDTO.getCityId());
        } else if (addressDTO.getCityName() != null && !addressDTO.getCityName().isBlank()) {
            city = cityMapper.toEntity(cityService.findByName(addressDTO.getCityName()));
        } else {
            throw new ResourceNotFoundException("Cidade é obrigatória: informe cityId ou cityName.");
        }
//        User user = userMapper.toEntityUser(userService.findByIdUser(addressDTO.getUserId()));
        User user = userRepository.findById(addressDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + addressDTO.getUserId()));
        address.setCity(city);
        address.setUser(user);
        applyCoordinates(address);
        addressRepository.save(address);
        return addressMapper.toDto(address);
    }

    public Address createAddress(AddressDTO addressDTO, User user, City city) {
        Address address = addressMapper.toEntity(addressDTO);

        if (city == null) {
            throw new ResourceNotFoundException("Cidade não encontrada.");
        }

        address.setUser(user);
        address.setCity(city);
        applyCoordinates(address);
        return addressRepository.save(address);
    }

    public AddressDTO findByStreetName(String streetName) {
        Optional<Address> addressOptional = addressRepository.findByStreetName(streetName);
        return addressOptional.map(addressMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Endereço não encontrada com esse nome de rua: " + streetName));
    }

    // F12: só o dono do endereço (ou o admin) edita
    public AddressDTO updateAddress(Long id, AddressDTO addressDTO, User actor) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));
        requireOwnerOrAdmin(address.getUser() == null ? null : address.getUser().getId(), actor);
        return updateAddress(id, addressDTO);
    }

    public AddressDTO findByUserIdAddress(String userId, User actor) {
        requireOwnerOrAdmin(userId, actor);
        return findByUserIdAddress(userId);
    }

    private void requireOwnerOrAdmin(String ownerId, User actor) {
        if (actor.getUserType() != UserTypeEnum.ADMIN && !actor.getId().equals(ownerId)) {
            throw new ForbiddenResourceException("Este endereço não é seu.");
        }
    }

    public AddressDTO updateAddress(Long id,AddressDTO addressDTO) {
        Address addressExisting = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado"));

        City city = cityMapper.toEntity(cityService.findByIdCity(addressDTO.getCityId()));
        // HU17: CEP novo (ou endereço ainda sem coordenada) = busca de novo
        boolean refreshCoordinates = !java.util.Objects.equals(addressExisting.getZipCode(), addressDTO.getZipCode())
                || addressExisting.getLatitude() == null;
        addressExisting.setStreetName(addressDTO.getStreetName());
        addressExisting.setNumber(addressDTO.getNumber());
        addressExisting.setComplement(addressDTO.getComplement());
        addressExisting.setNeighborhood(addressDTO.getNeighborhood());
        addressExisting.setZipCode(addressDTO.getZipCode());
        addressExisting.setCity(city);
        if (refreshCoordinates) {
            applyCoordinates(addressExisting);
        }

        Address updatedAddress = addressRepository.save(addressExisting);
        return addressMapper.toDto(updatedAddress);
    }

    public AddressDTO findByUserIdAddress(String userId) {
        Optional<Address> addressOptional = addressRepository.findFirstByUserIdOrderByIdDesc(userId);
        return addressOptional.map(addressMapper::toDto).orElseThrow(()
                -> new ResourceNotFoundException("Endereço não encontrada para esse usuario com esse id: " + userId));

    }

    public List<AddressDTO> findAllByUserIdAddress(String userId) {
        return addressRepository.findAllByUserIdOrderByIdDesc(userId)
                .stream()
                .map(addressMapper::toDto)
                .collect(Collectors.toList());
    }

    public Address findEntityByUserId(String userId) {
        return addressRepository.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado para esse usuário com id: " + userId));
    }



    // HU17: falha na consulta não impede salvar; o endereço fica sem coordenada e o frete cai para a taxa fixa
    private void applyCoordinates(Address address) {
        address.applyCoordinates(geocodingService.findByZipCode(address.getZipCode()).orElse(null));
    }
}
