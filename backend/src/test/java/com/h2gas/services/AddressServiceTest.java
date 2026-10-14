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
        // Long.MAX_VALUE em vez de um número "pequeno" fixo: o contador de
        // auto-incremento do H2 não é resetado por rollback entre testes
        // (@Transactional por método, mas a sequência é compartilhada pela
        // classe inteira), então um id pequeno fixo pode colidir com um id
        // real gerado por outro teste da mesma classe, dependendo da ordem
        // de execução — causava falha intermitente no CI.
        Long idInvalid = Long.MAX_VALUE;
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
    @DisplayName("Não deve quebrar ao buscar por usuário com múltiplos endereços")
    void naoDeveQuebrarAoBuscarPorUsuarioComMultiplosEnderecos() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        User user = userRepository.save(new User(
                "Cliente Multi Endereco 2",
                "cliente.multi2@h2gas.test",
                "41988887778",
                "12345678902",
                "senha",
                UserTypeEnum.CUSTOMER
        ));

        Address primeiro = new Address(
                null, "Rua Um", "Casa", "10", null, "Centro", "80000-000", city, user
        );
        Address segundo = new Address(
                null, "Rua Dois", "Trabalho", "20", null, "Batel", "80420-000", city, user
        );

        addressRepository.saveAndFlush(primeiro);
        Address maisRecente = addressRepository.saveAndFlush(segundo);

        AddressDTO result = assertDoesNotThrow(() -> addressService.findByUserIdAddress(user.getId()));

        assertNotNull(result);
        assertEquals(maisRecente.getId(), result.getId());
    }

    @Test
    @DisplayName("HU04: deve listar todos os endereços do usuário")
    void deveListarTodosEnderecosDoUsuario() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        User user = userRepository.save(new User(
                "Cliente Lista Enderecos",
                "cliente.lista@h2gas.test",
                "41988887779",
                "12345678903",
                "senha",
                UserTypeEnum.CUSTOMER
        ));
        User outro = userRepository.save(new User(
                "Outro Cliente",
                "outro.cliente@h2gas.test",
                "41988887770",
                "12345678904",
                "senha",
                UserTypeEnum.CUSTOMER
        ));

        addressRepository.saveAndFlush(new Address(
                null, "Rua Um", "Casa", "10", null, "Centro", "80000-000", city, user));
        addressRepository.saveAndFlush(new Address(
                null, "Rua Dois", "Trabalho", "20", null, "Batel", "80420-000", city, user));
        addressRepository.saveAndFlush(new Address(
                null, "Rua Três", "Casa", "30", null, "Centro", "80000-000", city, outro));

        List<AddressDTO> result = addressService.findAllByUserIdAddress(user.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(a -> "Casa".equals(a.getAddressName())));
        assertTrue(result.stream().anyMatch(a -> "Trabalho".equals(a.getAddressName())));
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