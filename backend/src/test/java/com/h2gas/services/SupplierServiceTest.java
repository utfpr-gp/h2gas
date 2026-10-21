package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.dto.SupplierCreateDTO;
import com.h2gas.model.dto.SupplierDTO;
import com.h2gas.model.entity.Address;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.mapper.SupplierMapper;
import com.h2gas.model.repository.AddressRepository;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.repository.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    private SupplierMapper supplierMapper;

    private String validId;


    @BeforeEach
    public void setUp() {
        User user = new User(null,
                "Pablo Teste",
                "pablo@gmail.com",
                "1234545",
                "234234234",
                "4534534",
                UserTypeEnum.SUPPLIER
        );

        userRepository.save(user);

        User user2 = new User(null,
                "Teste novo",
                "pablodddd@gmail.com",
                "12dd34545",
                "123435345dd",
                "234234234",
                UserTypeEnum.SUPPLIER
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
        Supplier supplierFirst = getSupplier(
                "Fornecedor Teste",
                "fornecedor@gmail",
                "1234567890",
                "12343445",
                "senhateste",
                "12345678901234",
                "Testando descricao",
                true,
                false,
                address2,
                cityFirst
        );


        supplierRepository.save(supplierFirst);

        Supplier supplierSecond = getSupplier(
                "Fornecedor Teste 2",
                "fornecedor2@gmail",
                "1234567890333",
                "12343445333",
                "senhateste2",
                "12345678901234",
                "Testando descricao 2",
                true,
                false,
                address,
                cityFirst
        );

        supplierRepository.save(supplierSecond);

        validId = supplierFirst.getId();
    }

    @NotNull
    private static Supplier getSupplier(
            String name,
            String email,
            String phoneNumber,
            String cpf,
            String password,
            String cnpj,
            String description,
            boolean active,
            boolean open,
            Address address,
            City city
    ) {
        Supplier supplier = new Supplier();

        // Campos da superclasse User
        supplier.setId(null);
        supplier.setName(name);
        supplier.setEmail(email);
        supplier.setPhoneNumber(phoneNumber);
        supplier.setCpf(cpf);
        supplier.setPassword(password);
        supplier.setUserType(UserTypeEnum.SUPPLIER);

        // Campos específicos do Supplier
        supplier.setCnpj(cnpj);
        supplier.setDescription(description);
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplier.setActive(active);
        supplier.setAddress(address);
        supplier.setCity(city);
        supplier.setProductItems(null);
        supplier.setOpen(open);

        return supplier;
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
        String idInvalid = "7fb34e6f-6c77-453f-8915-b814483f65bd";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            supplierService.findById(idInvalid);
        });
        assertEquals("Não encontrado nenhum Fornecedor com esse id: " +
                idInvalid, exception.getMessage());
    }

    @Test
    @DisplayName("Deve criar um fornecedor com sucesso")
    void createSupplierSuccess() {
        City city = cityRepository.findAll().get(0);

        User newUser = new User(
                null,
                "Usuário Novo",
                "usernovo@gmail.com",
                "senha123",
                "123456789",
                "555666777",
                UserTypeEnum.SUPPLIER
        );
        userRepository.save(newUser);

        Address newAddress = new Address(
                null,
                "Rua Nova do Test333333e",
                "casa 123",
                "999999",
                "Perto da praça",
                "Bairro Central",
                "8888888",
                city,
                null
        );
        addressRepository.save(newAddress);
        Supplier supplierCreate = getSupplier(
                "Fornecedor Novo",
                "fornecedorNovo@gmail",
                "1234567890333",
                "12343445333",
                "senhateste2",
                "12345678901234",
                "Testando descricao 2",
                true,
                false,
                newAddress,
                city
        );
//        SupplierCreateDTO dto = new SupplierCreateDTO();
//        dto.setName("Fornecedor Novo");
//        dto.setDescription("Fornecedor criado no teste");
//        dto.setOpeningTime(String.valueOf(LocalTime.of(9, 0)));
//        dto.setClosingTime(String.valueOf(LocalTime.of(17, 0)));
//        dto.setCityId(city.getId());
//        dto.setAddressId(newAddress.getId());
//        dto.setUserId(newUser.getId());

        SupplierDTO result = supplierMapper.toDto(supplierService.createSupplier(supplierCreate));

        assertNotNull(result);
        assertEquals("Fornecedor Novo", result.getName());

        Supplier fornecedorSalvo = supplierRepository.findById(result.getId()).orElse(null);
        assertNotNull(fornecedorSalvo);
        assertEquals("Fornecedor Novo", fornecedorSalvo.getName());
    }



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

    @Test
    @DisplayName("HU01: deve filtrar fornecedores por cidade quando cityId é informado")
    void deveFiltrarFornecedoresPorCidadeQuandoCityIdInformado() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));

        User userCuritiba = userRepository.save(new User(null,
                "Fornecedor Curitiba", "fornecedorcuritiba@gmail.com",
                "senha123", "111222333", "444555666", UserTypeEnum.SUPPLIER));

        Address addressCuritiba = addressRepository.save(new Address(
                null, "Rua Curitiba", "loja", "100", "perto do centro",
                "Centro", "80000-000", curitiba, null));

        Supplier supplierCuritiba = getSupplier(
                "Fornecedor Curitiba", "fornecedorcuritiba@gmail",
                "111222333", "999888777", "senhateste",
                "98765432101234", "Fornecedor de Curitiba",
                true, true, addressCuritiba, curitiba);
        supplierRepository.save(supplierCuritiba);

        Pageable pageable = PageRequest.of(0, 10);
        Page<SupplierDTO> result = supplierService.findAllSuppliersOpenAndActive(curitiba.getId(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Fornecedor Curitiba", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("HU01: deve retornar todos os ativos e abertos quando cityId é nulo")
    void deveRetornarTodosAtivosEAbertosQuandoCityIdNulo() {
        City cityAberta = cityRepository.save(new City(null, "Guarapuava", "PR"));

        User userAberto = userRepository.save(new User(null,
                "Fornecedor Aberto", "fornecedoraberto@gmail.com",
                "senha123", "222333444", "555666777", UserTypeEnum.SUPPLIER));

        Address addressAberta = addressRepository.save(new Address(
                null, "Rua Aberta", "loja", "200", "perto do centro",
                "Centro", "85000-000", cityAberta, null));

        Supplier supplierAberto = getSupplier(
                "Fornecedor Aberto", "fornecedoraberto@gmail",
                "222333444", "888777666", "senhateste",
                "12312312312345", "Fornecedor sempre aberto",
                true, true, addressAberta, cityAberta);
        supplierRepository.save(supplierAberto);

        Pageable pageable = PageRequest.of(0, 10);
        Page<SupplierDTO> result = supplierService.findAllSuppliersOpenAndActive(null, pageable);

        assertTrue(result.getContent().stream()
                .anyMatch(dto -> "Fornecedor Aberto".equals(dto.getName())));
    }

    @Test
    @DisplayName("F7: tempo médio de entrega não numérico é 400 com mensagem clara")
    void deveRejeitarTempoMedioDeEntregaNaoNumericoComMensagemClara() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> supplierService.updateTimesSupplier(validId, java.util.Map.of("averageDeliveryTime", "meia hora")));

        assertEquals("Tempo médio de entrega inválido: informe os minutos.", erro.getMessage());
    }

    private Supplier salvarFornecedor(String nome, String email, String cnpj, boolean active) {
        City city = cityRepository.save(new City(null, "Francisco Beltrão", "PR"));
        Address address = addressRepository.save(new Address(
                null, "Rua " + nome, "loja", "10", "centro", "Centro", "85600-000", city, null));
        return supplierRepository.save(getSupplier(nome, email, "46999990000", "000111222",
                "senhateste", cnpj, "descricao", active, true, address, city));
    }

    @Test
    @DisplayName("F7 fatia 6: active=false traz só quem aguarda aprovação")
    void deveFiltrarFornecedoresAguardandoAprovacao() {
        salvarFornecedor("Zeta Gás Aguardando", "zeta@h2gas.test", "11222333000144", false);

        Page<SupplierDTO> aguardando = supplierService.findAllSuppliers(false, null, PageRequest.of(0, 50));
        Page<SupplierDTO> ativos = supplierService.findAllSuppliers(true, null, PageRequest.of(0, 50));

        assertTrue(aguardando.getContent().stream().noneMatch(SupplierDTO::isActive));
        assertTrue(aguardando.getContent().stream().anyMatch(dto -> "Zeta Gás Aguardando".equals(dto.getName())));
        assertTrue(ativos.getContent().stream().noneMatch(dto -> "Zeta Gás Aguardando".equals(dto.getName())));
        assertTrue(ativos.getContent().stream().anyMatch(dto -> "Fornecedor Teste".equals(dto.getName())));
    }

    @Test
    @DisplayName("F7 fatia 6: busca por nome, e-mail ou CNPJ sem diferenciar maiúsculas")
    void deveBuscarFornecedoresPorNomeEmailOuCnpj() {
        salvarFornecedor("Ômega Água Mineral", "contato@omega.test", "99888777000166", false);

        assertEquals(1, supplierService.findAllSuppliers(null, "ômega água", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, supplierService.findAllSuppliers(null, "CONTATO@OMEGA", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, supplierService.findAllSuppliers(false, "99888777", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, supplierService.findAllSuppliers(true, "99888777", PageRequest.of(0, 10)).getTotalElements());
    }
}
