package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.*;
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

    @Autowired
    private UserRepository userRepository;

    private Long validAddressId;


    @BeforeEach
    public void setUp() {
        City cityFirst = new City(
                null,
                "Laranjeiras",
                "PR"
        );
        cityRepository.save(cityFirst);

        Address address = new Address(
            null,
            "rua teste 2",
            "casa 2",
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
                "casa 3",
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
    @DisplayName("Deve aceitar dois endereços para o mesmo usuário")
    void deveAceitarDoisEnderecosParaMesmoUsuario() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        User user = userRepository.save(new User(
                "Cliente Multi Endereco",
                "cliente.multi@h2gas.test",
                "41988887777",
                "12345678901",
                "senha",
                UserTypeEnum.CUSTOMER
        ));

        Address primeiro = new Address(
                null,
                "Rua Um",
                "Casa",
                "10",
                null,
                "Centro",
                "80000-000",
                city,
                user
        );
        Address segundo = new Address(
                null,
                "Rua Dois",
                "Trabalho",
                "20",
                null,
                "Batel",
                "80420-000",
                city,
                user
        );

        addressRepository.saveAndFlush(primeiro);
        Address salvo = addressRepository.saveAndFlush(segundo);

        assertNotNull(salvo.getId());
        long enderecosDoUsuario = addressRepository.findAll().stream()
                .filter(address -> address.getUser() != null && user.getId().equals(address.getUser().getId()))
                .count();
        assertEquals(2, enderecosDoUsuario);
    }

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