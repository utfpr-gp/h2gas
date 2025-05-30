package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "jwt.secret=segredoFake123")
@Transactional
class SupplierServiceTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    private Long validId;


    @BeforeEach
    public void setUp() {
        User user = new User(null,
                "Pablo Teste",
                "pablo@gmail.com",
                "1234545",
                "123435345",
                "234234234",
                "4534534",
                UserTypeEnum.CUSTOMER,
                null
        );

        userRepository.save(user);

        User user2 = new User(null,
                "Teste novo",
                "pablodddd@gmail.com",
                "12dd34545",
                "123435345dd",
                "234234234",
                "453453334",
                UserTypeEnum.CUSTOMER,
                null
        );

        userRepository.save(user2);

        City cityFirst = new City(
                null,
                "Curitiba",
                "PR"
        );
        cityRepository.save(cityFirst);

        Address address = new Address(
                null,
                "rua teste fornecedor",
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
                "rua teste fornecedor testando",
                "1234123433333",
                "perto do lugar teste 33",
                "bairro do testado2",
                "924839847",
                cityFirst,
                null
        );

        addressRepository.save(address2);

        Supplier supplierFirst = new Supplier(
                null,
                "Fornecedor Teste",
                "1234567890",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                Duration.ofMinutes(30),
                false,
                address,
                cityFirst,
                user,
                null
        );

        supplierRepository.save(supplierFirst);

        Supplier supplierSecond = new Supplier(
                null,
                "Fornecedor Teste 2",
                "1234567890433",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                Duration.ofMinutes(30),
                false,
                address2,
                cityFirst,
                user2,
                null
        );

        supplierRepository.save(supplierSecond);

        validId = supplierFirst.getId();
    }

    @Test
    @DisplayName("Busca todos os fornecedores")
    void findAllSuppliers() {
        List<SupplierDTO> result = supplierService.findAllSuppliers();
        assertEquals(2, result.size());
        assertNotNull(result);
        assertEquals("Fornecedor Teste", result.get(0).getName());
        assertEquals("Fornecedor Teste 2", result.get(1).getName());
    }

    @Test
    @DisplayName("Busca os fornecedores pelo id")
    void findByIdSuccess() {
        SupplierDTO result = supplierService.findById(validId);
        assertNotNull(result);
        assertEquals("Fornecedor Teste", result.getName());
    }

    @Test
    @DisplayName("Retorna erro ao informa id invalido")
    void findByIdError() {
        Long idInvalid = 400L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            supplierService.findById(idInvalid);
        });
        assertEquals("Não encontrado nenhum Fornecedor com esse id: " +
                idInvalid, exception.getMessage());
    }

//    @Test
//    void createSupplier() {
//    }
//
//    @Test
//    void deleteSupplier() {
//    }

    @Test
    @DisplayName("Deve atualizar o status de ativo do fornecedor com sucesso")
    void updateStatusSupplier() {
        supplierService.updateStatusSupplier(validId, false);
        Supplier atualizado = supplierRepository.findById(validId).orElse(null);
        assertNotNull(atualizado);
        assertFalse(atualizado.isActive());
    }
}