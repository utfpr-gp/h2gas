package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.DelivererCreateDTO;
import com.h2gas.model.dto.DelivererDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DelivererServiceTest {

    @Autowired private DelivererService delivererService;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private DelivererRepository delivererRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CityRepository cityRepository;

    private Supplier fornecedor;
    private Supplier outroFornecedor;
    private User cliente;

    @BeforeEach
    void setUp() {
        City city = cityRepository.save(new City(null, "Curitiba", "PR"));
        fornecedor = novoFornecedor("Gás Centro", "gas.centro@teste.com", "11111111111", city);
        outroFornecedor = novoFornecedor("Água Pura", "agua.pura@teste.com", "22222222222", city);
        cliente = userRepository.save(new User(null, "Cliente", "cliente@teste.com", "41999999999",
                "33333333333", "senha", UserTypeEnum.CUSTOMER));
    }

    private Supplier novoFornecedor(String nome, String email, String cpf, City city) {
        Supplier s = new Supplier();
        s.setName(nome);
        s.setEmail(email);
        s.setPhoneNumber("41900000000");
        s.setCpf(cpf);
        s.setPassword("senha");
        s.setUserType(UserTypeEnum.SUPPLIER);
        s.setCnpj("12345678000100");
        s.setDescription("Fornecedor de teste");
        s.setOpeningTime(LocalTime.of(8, 0));
        s.setClosingTime(LocalTime.of(18, 0));
        s.setAverageDeliveryTime(Duration.ofMinutes(30));
        s.setActive(true);
        s.setOpen(true);
        s.setCity(city);
        return supplierRepository.save(s);
    }

    private DelivererCreateDTO dto(String nome, String email) {
        return new DelivererCreateDTO(nome, email, "41988887777", "senha123", "Moto - ABC-1234");
    }

    @Test
    @DisplayName("HU07: cria entregador vinculado ao fornecedor, com senha criptografada")
    void deveCriarEntregadorVinculadoAoFornecedor() {
        DelivererDTO criado = delivererService.createDeliverer(dto("Carlos Silva", "carlos@teste.com"), fornecedor.getId());

        assertNotNull(criado.getId());
        assertEquals("Carlos Silva", criado.getName());
        assertEquals("Moto - ABC-1234", criado.getVehicleInfo());
        assertTrue(criado.isActive());
        assertFalse(criado.isBusy());

        Deliverer salvo = delivererRepository.findById(criado.getId()).orElseThrow();
        assertEquals(fornecedor.getId(), salvo.getSupplier().getId());
        assertEquals(UserTypeEnum.DELIVERER, salvo.getUser().getUserType());
        assertNotEquals("senha123", salvo.getUser().getPassword());
    }

    @Test
    @DisplayName("HU07: não cadastra entregador com e-mail já usado")
    void deveRejeitarEmailJaCadastrado() {
        delivererService.createDeliverer(dto("Carlos Silva", "carlos@teste.com"), fornecedor.getId());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> delivererService.createDeliverer(dto("Outro Carlos", "carlos@teste.com"), fornecedor.getId()));
        assertEquals("E-mail já cadastrado.", ex.getMessage());
    }

    @Test
    @DisplayName("HU07: a frota listada é só a do fornecedor")
    void deveListarApenasEntregadoresDoFornecedor() {
        delivererService.createDeliverer(dto("Bruno", "bruno@teste.com"), fornecedor.getId());
        delivererService.createDeliverer(dto("Ana", "ana@teste.com"), fornecedor.getId());
        delivererService.createDeliverer(dto("Zeca", "zeca@teste.com"), outroFornecedor.getId());

        List<DelivererDTO> frota = delivererService.findMine(fornecedor.getId());

        assertEquals(List.of("Ana", "Bruno"), frota.stream().map(DelivererDTO::getName).toList());
    }

    @Test
    @DisplayName("HU07: pedido só atribuído (entrega não iniciada) não torna o entregador ocupado")
    void naoDeveMarcarOcupadoQuandoPedidoSoEstaAtribuido() {
        DelivererDTO entregador = delivererService.createDeliverer(dto("Carlos", "carlos@teste.com"), fornecedor.getId());
        Order pedido = new Order();
        pedido.setCustomer(cliente);
        pedido.setSupplier(fornecedor);
        pedido.setStatus(OrderStatusEnum.IN_PREPARATION);
        pedido.setPaymentMethod("PIX");
        pedido.setTotalPrice(new BigDecimal("125.00"));
        pedido.setDeliverer(delivererRepository.findById(entregador.getId()).orElseThrow());
        orderRepository.save(pedido);

        assertFalse(delivererService.findMine(fornecedor.getId()).get(0).isBusy());
    }

    @Test
    @DisplayName("HU07: entregador com pedido em rota aparece como ocupado")
    void deveMarcarEntregadorComoOcupadoQuandoHaPedidoEmRota() {
        DelivererDTO ocupado = delivererService.createDeliverer(dto("Carlos", "carlos@teste.com"), fornecedor.getId());
        DelivererDTO livre = delivererService.createDeliverer(dto("Ana", "ana@teste.com"), fornecedor.getId());
        Order pedido = new Order();
        pedido.setCustomer(cliente);
        pedido.setSupplier(fornecedor);
        pedido.setStatus(OrderStatusEnum.OUT_FOR_DELIVERY);
        pedido.setPaymentMethod("PIX");
        pedido.setTotalPrice(new BigDecimal("125.00"));
        pedido.setDeliverer(delivererRepository.findById(ocupado.getId()).orElseThrow());
        orderRepository.save(pedido);

        List<DelivererDTO> frota = delivererService.findMine(fornecedor.getId());

        assertTrue(frota.stream().filter(d -> d.getId().equals(ocupado.getId())).findFirst().orElseThrow().isBusy());
        assertFalse(frota.stream().filter(d -> d.getId().equals(livre.getId())).findFirst().orElseThrow().isBusy());
    }
}
