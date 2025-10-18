package com.h2gas.services;

import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderRequestDTO;
import com.h2gas.model.dto.OrderResponseDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.entity.Order;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@TestPropertySource(properties = "jwt.secret=segredoFake123")
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ProductItemRepository productItemRepository;

    @Autowired
    private CategoryProductRepository categoryProductRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    private String validOrderId;
    private String validCustomerId;
    private String validSupplierId;
    private Long validProductItemId;


    @BeforeEach
    public void setUp() {
        // Criar cidade
        City city = new City(null, "Curitiba", "PR");
        cityRepository.save(city);

        // Criar endereço do fornecedor
        Address supplierAddress = new Address(
                null,
                "Rua do Fornecedor",
                "Loja 10",
                "80010-000",
                "Próximo ao shopping",
                "Batel",
                "41988888888",
                city,
                null
        );
        addressRepository.save(supplierAddress);

        // Criar cliente
        User customer = new User(
                null,
                "Cliente Teste",
                "cliente@teste.com",
                "41987654321",
                "12345678900",
                "senhaCliente123",
                UserTypeEnum.CUSTOMER
        );
        userRepository.save(customer);
        validCustomerId = customer.getId();

        // Criar endereço do cliente
        Address customerAddress = new Address(
                null,
                "Rua do Cliente",
                "Casa 1",
                "80000-000",
                "Próximo ao parque",
                "Centro",
                "41999999999",
                city,
                customer
        );
        addressRepository.save(customerAddress);

        // Criar fornecedor
        Supplier supplier = createSupplier(
                "Fornecedor Teste",
                "fornecedor@teste.com",
                "41912345678",
                "98765432100",
                "senhaFornecedor123",
                "12345678000199",
                "Fornecedor de testes",
                true,
                true,
                supplierAddress,
                city
        );
        supplierRepository.save(supplier);
        validSupplierId = supplier.getId();

        ProductType productTypeSecond = new ProductType(
                null,
                "Gás",
                null
        );

        productTypeRepository.save(productTypeSecond);

        // Criar categoria de produto
        CategoryProduct category = new CategoryProduct(
                null,
                "Gás azul",
                productTypeSecond,
                null,
                null
        );
        categoryProductRepository.save(category);

        // Criar produto
        ProductItem productItem = new ProductItem();
        productItem.setBrand("Marca Teste");
        productItem.setPrice(new BigDecimal("89.90"));
        productItem.setStockQuantity(100);
        productItem.setImageUrl("example.com/image.jpg");
        productItem.setCategoryProduct(category);
        productItem.setSupplier(supplier);
        productItemRepository.save(productItem);
        validProductItemId = productItem.getId();

        // Criar pedido de teste
        Order order = new Order();
        order.setCustomer(customer);
        order.setSupplier(supplier);
        order.setDeliveryAddress(customerAddress);
        order.setStatus(OrderStatusEnum.PENDING);
        order.setPaymentMethod("PIX");
        order.setNote("Entregar pela manhã");
        order.setTotalPrice(new BigDecimal("179.80"));
        orderRepository.save(order);
        validOrderId = order.getId();
    }


    @Test
    @DisplayName("Deve criar um pedido com sucesso")
    void createOrder() {
        // Arrange
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO();
        itemDTO.setProductItemRef(validProductItemId);
        itemDTO.setQuantity(2);

        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setCustomerUuid(validCustomerId);
        requestDTO.setSupplierRef(validSupplierId);
        requestDTO.setPaymentMethod("Cartão de Crédito");
        requestDTO.setNote("Deixar na portaria");
        requestDTO.setItems(List.of(itemDTO));

        // Act
        OrderResponseDTO result = orderService.createOrder(requestDTO);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(OrderStatusEnum.PENDING.getStatus(), result.getStatus());
        assertEquals("Cartão de Crédito", result.getPaymentMethod());
        assertEquals(new BigDecimal("179.80"), result.getTotalPrice());
        assertEquals(1, result.getOrderItems().size());
    }

    @Test
    @DisplayName("Deve buscar todos os pedidos com paginação")
    void findAllOrders() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<OrderResponseDTO> result = orderService.findAllOrders(pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 1);
    }

    @Test
    @DisplayName("Deve buscar pedido por ID com sucesso")
    void findByIdOrder() {
        // Act
        OrderResponseDTO result = orderService.findByIdOrder(validOrderId);

        // Assert
        assertNotNull(result);
        assertEquals(validOrderId, result.getId());
        assertEquals(OrderStatusEnum.PENDING.getStatus(), result.getStatus());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar pedido com ID inválido")
    void findByIdOrderNotFound() {
        // Arrange
        String invalidId = "invalid-uuid-123";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.findByIdOrder(invalidId);
        });

        assertTrue(exception.getMessage().contains("Pedido não encontrado"));
    }

    @Test
    @DisplayName("Deve buscar pedidos por ID do cliente")
    void findByCustomerId() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<OrderResponseDTO> result = orderService.findByCustomerId(validCustomerId, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 1);
        result.getContent().forEach(order ->
                assertEquals(validCustomerId, order.getCustomerId())
        );
    }

    @Test
    @DisplayName("Deve buscar pedidos por ID do fornecedor")
    void findBySupplierId() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        String supplierLongId = validSupplierId;

        // Act
        Page<OrderResponseDTO> result = orderService.findBySupplierId(supplierLongId, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 1);
    }

    @Test
    @DisplayName("Deve buscar pedidos por ID do fornecedor e status")
    void findBySupplierIdAndStatus() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        String supplierLongId = validSupplierId;

        // Act
        Page<OrderResponseDTO> result = orderService.findBySupplierIdAndStatus(
                supplierLongId,
                OrderStatusEnum.PENDING,
                pageable
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 1);
        result.getContent().forEach(order ->
                assertEquals(OrderStatusEnum.PENDING.getStatus(), order.getStatus())
        );
    }

    @Test
    @DisplayName("Deve buscar pedidos por status")
    void findByStatus() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<OrderResponseDTO> result = orderService.findByStatus(OrderStatusEnum.PENDING, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 1);
        result.getContent().forEach(order ->
                assertEquals(OrderStatusEnum.PENDING.getStatus(), order.getStatus())
        );
    }

    @Test
    @DisplayName("Deve atualizar o status do pedido com sucesso")
    void updateOrderStatus() {
        // Act
        String newStatus = orderService.updateOrderStatus(validOrderId, "CONFIRMED");

        // Assert
        assertEquals("Confirmado", newStatus);

        Order updatedOrder = orderRepository.findById(validOrderId).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatusEnum.CONFIRMED, updatedOrder.getStatus());
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar status de pedido inexistente")
    void updateOrderStatusNotFound() {
        // Arrange
        String invalidId = "invalid-uuid-456";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(invalidId, "CONFIRMED");
        });

        assertTrue(exception.getMessage().contains("Pedido com " + invalidId + " não encontrado"));
    }

    @Test
    @DisplayName("Deve buscar pedidos por fornecedor excluindo status DELIVERED e CANCELED")
    void findBySupplierIdAndStatusNotDeliveryAndCanceled() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        String supplierLongId = validSupplierId;
        List<OrderStatusEnum> excludeStatuses = List.of(
                OrderStatusEnum.DELIVERED,
                OrderStatusEnum.CANCELED
        );

        // Act
        Page<OrderResponseDTO> result = orderService.findBySupplierIdAndStatusNotDeliveryAndCanceled(
                supplierLongId,
                pageable,
                excludeStatuses
        );

        // Assert
        assertNotNull(result);
        result.getContent().forEach(order -> {
            assertNotEquals(OrderStatusEnum.DELIVERED, order.getStatus());
            assertNotEquals(OrderStatusEnum.CANCELED, order.getStatus());
        });
    }


    private Supplier createSupplier(
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

}