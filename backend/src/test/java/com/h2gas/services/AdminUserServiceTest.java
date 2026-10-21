package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.AdminCreateUserDto;
import com.h2gas.model.dto.AdminUserDto;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.Deliverer;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.CityRepository;
import com.h2gas.model.repository.DelivererRepository;
import com.h2gas.model.repository.SupplierRepository;
import com.h2gas.model.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminUserServiceTest {

    @Autowired private AdminUserService adminUserService;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private DelivererRepository delivererRepository;
    @Autowired private CityRepository cityRepository;

    private User admin;
    private City cidade;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(new User("Admin", "admin.hu12@teste.com", "41900000000", null, "senha", UserTypeEnum.ADMIN));
        cidade = cityRepository.save(new City(null, "Pato Branco", "PR"));
    }

    private AdminCreateUserDto novo(UserTypeEnum tipo, String nome, String email) {
        AdminCreateUserDto dto = new AdminCreateUserDto();
        dto.setName(nome);
        dto.setEmail(email);
        dto.setPassword("123456");
        dto.setPhoneNumber("41988887777");
        dto.setUserType(tipo);
        return dto;
    }

    private Supplier fornecedorAprovado() {
        AdminCreateUserDto dto = novo(UserTypeEnum.SUPPLIER, "Gás Centro", "gas.hu12@teste.com");
        dto.setCnpj("12345678000100");
        dto.setCityId(cidade.getId());
        AdminUserDto criado = adminUserService.createUser(dto);
        return supplierRepository.findById(criado.getId()).orElseThrow();
    }

    @Test
    @DisplayName("HU12: lista filtrando por perfil e por parte do nome ou do e-mail")
    void deveListarFiltrandoPorPerfilEBusca() {
        userRepository.save(new User("Maria Souza", "maria@teste.com", "41911111111", null, "senha", UserTypeEnum.CUSTOMER));
        userRepository.save(new User("João Lima", "joao.souza@teste.com", "41922222222", null, "senha", UserTypeEnum.CUSTOMER));
        userRepository.save(new User("Souza Entregas", "souza@teste.com", "41933333333", null, "senha", UserTypeEnum.DELIVERER));

        Page<AdminUserDto> clientesSouza = adminUserService.listUsers(UserTypeEnum.CUSTOMER, "souza", PageRequest.of(0, 10));
        Page<AdminUserDto> todosSouza = adminUserService.listUsers(null, "SOUZA", PageRequest.of(0, 10));
        Page<AdminUserDto> todos = adminUserService.listUsers(null, null, PageRequest.of(0, 10));

        assertEquals(List.of("João Lima", "Maria Souza"), clientesSouza.map(AdminUserDto::getName).getContent());
        assertEquals(3, todosSouza.getTotalElements());
        assertEquals(4, todos.getTotalElements());
        assertTrue(todos.getContent().stream().allMatch(AdminUserDto::isEnabled));
    }

    @Test
    @DisplayName("HU12: o admin cria outro admin (o cadastro público continua sem permitir)")
    void deveCriarOutroAdmin() {
        AdminUserDto criado = adminUserService.createUser(novo(UserTypeEnum.ADMIN, "Ana Admin", "ana.admin@teste.com"));

        User salvo = userRepository.findById(criado.getId()).orElseThrow();
        assertEquals(UserTypeEnum.ADMIN, salvo.getUserType());
        assertTrue(salvo.isEnabled());
        assertNotEquals("123456", salvo.getPassword(), "a senha é gravada criptografada");
    }

    @Test
    @DisplayName("HU12: e-mail repetido é recusado")
    void deveRecusarEmailJaCadastrado() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> adminUserService.createUser(novo(UserTypeEnum.CUSTOMER, "Outro", admin.getEmail())));
        assertEquals("E-mail já cadastrado.", erro.getMessage());
    }

    @Test
    @DisplayName("HU12: fornecedor exige CNPJ e cidade e nasce aguardando aprovação")
    void deveExigirCnpjECidadeParaFornecedor() {
        AdminCreateUserDto semCnpj = novo(UserTypeEnum.SUPPLIER, "Sem CNPJ", "sem.cnpj@teste.com");
        semCnpj.setCityId(cidade.getId());
        assertThrows(BusinessRuleException.class, () -> adminUserService.createUser(semCnpj));

        AdminCreateUserDto semCidade = novo(UserTypeEnum.SUPPLIER, "Sem Cidade", "sem.cidade@teste.com");
        semCidade.setCnpj("12345678000100");
        assertThrows(BusinessRuleException.class, () -> adminUserService.createUser(semCidade));

        AdminCreateUserDto cidadeInexistente = novo(UserTypeEnum.SUPPLIER, "Cidade X", "cidade.x@teste.com");
        cidadeInexistente.setCnpj("12345678000100");
        cidadeInexistente.setCityId(999_999L);
        assertThrows(ResourceNotFoundException.class, () -> adminUserService.createUser(cidadeInexistente));

        Supplier criado = fornecedorAprovado();
        assertEquals(cidade.getId(), criado.getCity().getId());
        assertFalse(criado.isActive(), "fornecedor criado pelo admin também passa pela aprovação");
        assertTrue(criado.isEnabled());
    }

    @Test
    @DisplayName("HU12: entregador exige o fornecedor a que pertence")
    void deveExigirFornecedorAoCriarEntregador() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> adminUserService.createUser(novo(UserTypeEnum.DELIVERER, "Carlos", "carlos@teste.com")));
        assertEquals("Informe o fornecedor do entregador.", erro.getMessage());
    }

    @Test
    @DisplayName("HU12: entregador criado pelo admin entra na frota do fornecedor escolhido")
    void deveCriarEntregadorVinculadoAoFornecedor() {
        Supplier fornecedor = fornecedorAprovado();
        AdminCreateUserDto dto = novo(UserTypeEnum.DELIVERER, "Carlos", "carlos@teste.com");
        dto.setSupplierId(fornecedor.getId());

        AdminUserDto criado = adminUserService.createUser(dto);

        Deliverer vinculo = delivererRepository.findByUserId(criado.getId()).orElseThrow();
        assertEquals(fornecedor.getId(), vinculo.getSupplier().getId());
        assertEquals(UserTypeEnum.DELIVERER, criado.getUserType());
    }

    @Test
    @DisplayName("HU12: desativa e reativa a conta")
    void deveDesativarEReativarUsuario() {
        User cliente = userRepository.save(new User("Cliente", "cliente.hu12@teste.com", "41911111111", null, "senha", UserTypeEnum.CUSTOMER));

        adminUserService.setEnabled(cliente.getId(), false, admin);
        assertFalse(userRepository.findById(cliente.getId()).orElseThrow().isEnabled());

        adminUserService.setEnabled(cliente.getId(), true, admin);
        assertTrue(userRepository.findById(cliente.getId()).orElseThrow().isEnabled());

        assertThrows(ResourceNotFoundException.class, () -> adminUserService.setEnabled("nao-existe", false, admin));
    }

    @Test
    @DisplayName("HU12: o admin não desativa a própria conta")
    void naoDevePermitirAdminDesativarASiMesmo() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> adminUserService.setEnabled(admin.getId(), false, admin));

        assertEquals("Você não pode desativar a sua própria conta.", erro.getMessage());
        assertTrue(userRepository.findById(admin.getId()).orElseThrow().isEnabled());
    }
}
