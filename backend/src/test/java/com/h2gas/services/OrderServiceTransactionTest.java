package com.h2gas.services;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sem @Transactional de propósito: o rollback de createOrder só é observável
 * fora de uma transação de teste. Os dados são criados e apagados aqui.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTransactionTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private ProductItemRepository productItemRepository;
    @Autowired private CategoryProductRepository categoryProductRepository;
    @Autowired private ProductTypeRepository productTypeRepository;

    private City city;
    private User customer;
    private Address customerAddress;
    private Address supplierAddress;
    private Supplier supplier;
    private ProductType productType;
    private CategoryProduct category;
    private ProductItem comEstoque;
    private ProductItem semEstoque;

    @BeforeEach
    void setUp() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        city = cityRepository.save(new City(null, "Curitiba", "PR"));
        supplierAddress = addressRepository.save(new Address(
                null, "Rua da Loja", "Loja", "100", null, "Centro", "80010-000", city, null));
        customer = userRepository.save(new User(
                null, "Cliente Tx", "cliente.tx." + sufixo + "@teste.com", "41911111111",
                "999" + sufixo.hashCode(), "senha", UserTypeEnum.CUSTOMER));
        customerAddress = addressRepository.save(new Address(
                null, "Rua do Cliente", "Casa", "200", null, "Centro", "80000-000", city, customer));

        supplier = new Supplier();
        supplier.setName("Fornecedor Tx");
        supplier.setEmail("fornecedor.tx." + sufixo + "@teste.com");
        supplier.setPhoneNumber("41933333333");
        supplier.setCpf("888" + sufixo.hashCode());
        supplier.setPassword("senha");
        supplier.setUserType(UserTypeEnum.SUPPLIER);
        supplier.setCnpj("12345678000100");
        supplier.setDescription("Fornecedor do teste de transação");
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplier.setActive(true);
        // HU18: aberta pelo horário (o relógio dos testes marca 10:00)
        supplier.setOpeningTime(java.time.LocalTime.of(8, 0));
        supplier.setClosingTime(java.time.LocalTime.of(18, 0));
        supplier.setAddress(supplierAddress);
        supplier.setCity(city);
        supplier = supplierRepository.save(supplier);

        productType = productTypeRepository.save(new ProductType(null, "Gás", null));
        category = categoryProductRepository.save(new CategoryProduct(null, "Gás 13kg", productType, null, null));
        comEstoque = productItemRepository.saveAndFlush(produto("Com estoque", 10));
        semEstoque = productItemRepository.saveAndFlush(produto("Sem estoque", 0));
    }

    private ProductItem produto(String marca, int estoque) {
        ProductItem p = new ProductItem();
        p.setBrand(marca);
        p.setPriceDelivery(new BigDecimal("100.00"));
        p.setPricePickup(new BigDecimal("90.00"));
        p.setStockQuantity(estoque);
        p.setCategoryProduct(category);
        p.setSupplier(supplier);
        return p;
    }

    @AfterEach
    void limpar() {
        orderRepository.deleteAll(orderRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId(), PageRequest.of(0, 50)).getContent());
        productItemRepository.deleteAllById(List.of(comEstoque.getId(), semEstoque.getId()));
        categoryProductRepository.delete(category);
        productTypeRepository.delete(productType);
        supplierRepository.delete(supplier);
        addressRepository.deleteAll(List.of(customerAddress, supplierAddress));
        userRepository.delete(customer);
        cityRepository.delete(city);
    }

    @Test
    @DisplayName("HU05 §5.3.2: item sem estoque desfaz o pedido inteiro e o estoque dos demais itens (RN02)")
    void deveDesfazerPedidoEEstoqueQuandoUmItemFalhaPorEstoque() {
        OrderItemRequestDTO itemOk = new OrderItemRequestDTO(comEstoque.getId(), 2);
        OrderItemRequestDTO itemSemEstoque = new OrderItemRequestDTO(semEstoque.getId(), 1);
        OrderRequestDTO request = new OrderRequestDTO();
        request.setCustomerUuid(customer.getId());
        request.setSupplierRef(supplier.getId());
        request.setPaymentMethod("PIX");
        request.setAddressId(customerAddress.getId());
        request.setDeliveryMode("DELIVERY");
        request.setItems(List.of(itemOk, itemSemEstoque));

        assertThrows(BusinessRuleException.class, () -> orderService.createOrder(request));

        assertEquals(10, productItemRepository.findById(comEstoque.getId()).orElseThrow().getStockQuantity(),
                "o estoque do item que passou não pode ficar decrementado");
        assertTrue(orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), PageRequest.of(0, 50)).isEmpty(),
                "nenhum pedido órfão pode ficar gravado");
    }
}
