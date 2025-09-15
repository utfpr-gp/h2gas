package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
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
                UserTypeEnum.SUPPLIER,
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
                UserTypeEnum.SUPPLIER,
                null
        );

        userRepository.save(user2);

        City cityFirst = new City(
                null,
                "Pato Branco",
                "PR"
        );
        cityRepository.save(cityFirst);

        Address address = new Address(
                null,
                "rua teste fornecedor",
                "casa aqui",
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
                "trabalho teste",
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
                "Testando descição",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                Duration.ofMinutes(30),
                false,
                address,
                cityFirst,
                user,
                null,
                false
        );

        supplierRepository.save(supplierFirst);

        Supplier supplierSecond = new Supplier(
                null,
                "Fornecedor Teste 2",
                "1234567890433",
                "teste teste",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                Duration.ofMinutes(30),
                false,
                address2,
                cityFirst,
                user2,
                null,
                false
        );

        supplierRepository.save(supplierSecond);

        validId = supplierFirst.getId();
    }

//    @Test
//    @DisplayName("Busca todos os fornecedores")
//    void findAllSuppliers() {
//        List<SupplierDTO> result = supplierService.findAllSuppliers();
//        assertEquals(2, result.size());
//        assertNotNull(result);
//        assertEquals("Fornecedor Teste", result.get(0).getName());
//        assertEquals("Fornecedor Teste 2", result.get(1).getName());
//    }

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
//    @DisplayName("Deve criar um fornecedor com sucesso")
//    void createSupplierSuccess() {
//        // Arrange
//        User user = userRepository.findAll().get(0); // pega o primeiro usuário salvo no setUp
//        City city = cityRepository.findAll().get(0); // pega a cidade salva
//
//        // Criar um novo endereço para não violar a constraint UNIQUE
//        Address newAddress = new Address(
//                null,
//                "Rua Nova do Teste",
//                "casa 123",
//                "999999",
//                "Perto da praça",
//                "Bairro Central",
//                "8888888",
//                city,
//                null
//        );
//        addressRepository.save(newAddress);
//
//        SupplierCreateDTO dto = new SupplierCreateDTO();
//        dto.setName("Fornecedor Novo");
//        dto.setDescription("Fornecedor criado no teste");
//        dto.setOpeningTime(String.valueOf(LocalTime.of(9, 0)));
//        dto.setClosingTime(String.valueOf(LocalTime.of(17, 0)));
//        dto.setCityId(city.getId());
//        dto.setAddressId(newAddress.getId()); // usa endereço novo
//        dto.setUserId(user.getId());
//
//        // Act
//        SupplierDTO result = supplierService.createSupplier(dto);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals("Fornecedor Novo", result.getName());
//
//        // garante que salvou no banco
//        Supplier fornecedorSalvo = supplierRepository.findById(result.getId()).orElse(null);
//        assertNotNull(fornecedorSalvo);
//        assertEquals("Fornecedor Novo", fornecedorSalvo.getName());
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