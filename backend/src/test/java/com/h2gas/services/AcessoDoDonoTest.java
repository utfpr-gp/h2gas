package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.model.dto.AddressDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

// F12 item (a): ninguém lê ou altera, pelo id, o que não é seu
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AcessoDoDonoTest {

    @Autowired private ProductItemService productItemService;
    @Autowired private OrderService orderService;
    @Autowired private AddressService addressService;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private DelivererRepository delivererRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private ProductTypeRepository productTypeRepository;
    @Autowired private CategoryProductRepository categoryProductRepository;
    @Autowired private ProductItemRepository productItemRepository;

    private Supplier lojaA;
    private Supplier lojaB;
    private User cliente;
    private User outroCliente;
    private User usuarioEntregador;
    private User outroEntregador;
    private User admin;
    private ProductItem produtoDaLojaB;
    private Order pedidoDaLojaA;
    private Address enderecoDoCliente;

    @BeforeEach
    void setUp() {
        City city = cityRepository.save(new City(null, "Guarapuava", "PR"));
        lojaA = loja("a", city);
        lojaB = loja("b", city);
        cliente = usuario("cliente", "11111111111", UserTypeEnum.CUSTOMER);
        outroCliente = usuario("outro.cliente", "22222222222", UserTypeEnum.CUSTOMER);
        usuarioEntregador = usuario("entregador", "33333333333", UserTypeEnum.DELIVERER);
        outroEntregador = usuario("outro.entregador", "44444444444", UserTypeEnum.DELIVERER);
        admin = usuario("admin", "55555555555", UserTypeEnum.ADMIN);

        Deliverer entregador = new Deliverer();
        entregador.setUser(usuarioEntregador);
        entregador.setSupplier(lojaA);
        entregador = delivererRepository.save(entregador);

        ProductType tipo = productTypeRepository.save(new ProductType(null, "Gás", null));
        CategoryProduct categoria = categoryProductRepository.save(new CategoryProduct(null, "P13", tipo, null));
        produtoDaLojaB = new ProductItem();
        produtoDaLojaB.setBrand("UltragazFixture");
        produtoDaLojaB.setPrice(new BigDecimal("110.00"));
        produtoDaLojaB.setStockQuantity(5);
        produtoDaLojaB.setCategoryProduct(categoria);
        produtoDaLojaB.setSupplier(lojaB);
        produtoDaLojaB.setVersion(null);
        produtoDaLojaB = productItemRepository.save(produtoDaLojaB);

        pedidoDaLojaA = new Order();
        pedidoDaLojaA.setCustomer(cliente);
        pedidoDaLojaA.setSupplier(lojaA);
        pedidoDaLojaA.setStatus(OrderStatusEnum.OUT_FOR_DELIVERY);
        pedidoDaLojaA.setDeliveryMode(DeliveryModeEnum.DELIVERY);
        pedidoDaLojaA.setPaymentMethod("PIX");
        pedidoDaLojaA.setTotalPrice(new BigDecimal("113.96"));
        pedidoDaLojaA.setDeliverer(entregador);
        pedidoDaLojaA = orderRepository.save(pedidoDaLojaA);

        enderecoDoCliente = addressRepository.save(new Address(null, "Rua XV", "Casa", "100", null,
                "Centro", "85010100", city, cliente));
    }

    private Supplier loja(String sufixo, City city) {
        Supplier s = new Supplier();
        s.setName("Loja " + sufixo);
        s.setEmail("loja." + sufixo + "@teste.com");
        s.setPhoneNumber("42900000000");
        s.setCpf(sufixo.equals("a") ? "66666666666" : "77777777777");
        s.setPassword("senha");
        s.setUserType(UserTypeEnum.SUPPLIER);
        s.setCnpj(sufixo.equals("a") ? "12345678000100" : "12345678000200");
        s.setDescription("Loja de teste");
        s.setOpeningTime(LocalTime.of(8, 0));
        s.setClosingTime(LocalTime.of(18, 0));
        s.setAverageDeliveryTime(Duration.ofMinutes(30));
        s.setActive(true);
        s.setCity(city);
        return supplierRepository.save(s);
    }

    private User usuario(String nome, String cpf, UserTypeEnum tipo) {
        return userRepository.save(new User(null, nome, nome + "@teste.com", "42999999999", cpf, "senha", tipo));
    }

    // Cenário 1
    @Test
    void deveNegarProdutoDeOutraLoja() {
        assertThrows(ForbiddenResourceException.class,
                () -> productItemService.findOwnById(produtoDaLojaB.getId(), lojaA));
    }

    @Test
    void deveAbrirOProdutoDaPropriaLoja() {
        assertEquals("UltragazFixture", productItemService.findOwnById(produtoDaLojaB.getId(), lojaB).getBrand());
    }

    // Cenário 2
    @Test
    void deveNegarPedidoAQuemNaoParticipa() {
        String id = pedidoDaLojaA.getId();
        assertThrows(ForbiddenResourceException.class, () -> orderService.findByIdOrder(id, outroCliente));
        assertThrows(ForbiddenResourceException.class, () -> orderService.findByIdOrder(id, lojaB));
        assertThrows(ForbiddenResourceException.class, () -> orderService.findByIdOrder(id, outroEntregador));
    }

    // Cenário 6
    @Test
    void deveMostrarPedidoAoClienteALojaAoEntregadorEAoAdmin() {
        String id = pedidoDaLojaA.getId();
        assertEquals(id, orderService.findByIdOrder(id, cliente).getId());
        assertEquals(id, orderService.findByIdOrder(id, lojaA).getId());
        assertEquals(id, orderService.findByIdOrder(id, usuarioEntregador).getId());
        assertEquals(id, orderService.findByIdOrder(id, admin).getId());
    }

    // Cenário 3
    @Test
    void deveNegarStatusDePedidoDeOutraLoja() {
        String id = pedidoDaLojaA.getId();
        assertThrows(ForbiddenResourceException.class,
                () -> orderService.updateOrderStatus(id, "DELIVERED", lojaB));
        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void deveDeixarALojaDoPedidoEOAdminMudarOStatus() {
        String id = pedidoDaLojaA.getId();
        assertEquals("Entregue", orderService.updateOrderStatus(id, "DELIVERED", lojaA));
        assertEquals("Cancelado", orderService.updateOrderStatus(id, "CANCELED", admin));
    }

    // Cenário 5
    @Test
    void deveNegarEnderecoDeOutroUsuario() {
        String dono = cliente.getId();
        Long enderecoId = enderecoDoCliente.getId();
        assertThrows(ForbiddenResourceException.class, () -> addressService.findByUserIdAddress(dono, outroCliente));
        assertThrows(ForbiddenResourceException.class,
                () -> addressService.updateAddress(enderecoId, new AddressDTO(), outroCliente));
        assertEquals("Rua XV", addressRepository.findById(enderecoId).orElseThrow().getStreetName());
    }

    @Test
    void deveMostrarOEnderecoAoDonoEAoAdmin() {
        String dono = cliente.getId();
        assertEquals("Rua XV", addressService.findByUserIdAddress(dono, cliente).getStreetName());
        assertEquals("Rua XV", addressService.findByUserIdAddress(dono, admin).getStreetName());
    }
}
