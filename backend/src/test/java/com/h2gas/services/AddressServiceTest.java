package com.h2gas.services;

import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.CityRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "jwt.secret=segredoFake123")
@Transactional
class AddressServiceTest {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

    private Long validAddressId;


    @BeforeAll
    public void setUp() {
        City cityFirst = new City(
                null,
                "Curitiba",
                "PR"
        );
        cityRepository.save(cityFirst);

        Address address = new Address(
            null,
            "rua teste 2",
            "123412343",
            "perto do lugar teste 3",
            "bairro do testado",
            "924839847",
            cityFirst,
            null
        );

        addressRepository.save(address);

        Address address2 = new Address(
                null,
                "rua teste 3",
                "1234123433333",
                "perto do lugar teste 33",
                "bairro do testado2",
                "924839847",
                cityFirst,
                null
        );

        addressRepository.save(address2);

        validAddressId = address2.getId();
    }

    @Test
    @DisplayName("Procura todos os endereços")
    void findAllAddresses() {
        List<AddressDTO> result = addressService.findAllAddresses();
        assertEquals(2, result.size());
        assertNotNull(result);
        assertEquals("rua teste 2", result.get(0).getStreetName());
        assertEquals("1234123433333", result.get(1).getNumber());
    }

    @Test
    @DisplayName("Procura o endereço pelo id com sucesso")
    void findByIdAddressSuccess() {
        AddressDTO result = addressService.findByIdAddress(validAddressId);
        assertNotNull(result);
        assertEquals("rua teste 3", result.getStreetName());
    }

    @Test
    @DisplayName("Retorna erro ao passar o id incorreto do endereço")
    void findByIdAddressError() {
        Long idInvalid = 50L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            addressService.findByIdAddress(idInvalid);
        });
        assertEquals("Endereço não encontrado com esse id: " + idInvalid, exception.getMessage());
    }

//    @Test
//    void createAddress() {
//    }

    @Test
    @DisplayName("Deve encontrar o endereço com sucesso")
    void findByStreetNameSuccess() {
        AddressDTO result = addressService.findByStreetName("rua teste 2");
        assertNotNull(result);
        assertEquals("rua teste 2", result.getStreetName());
    }

//    @Test
//    void updateAddress() {
//    }
}