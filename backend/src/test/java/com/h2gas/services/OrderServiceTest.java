package com.h2gas.services;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.DelivererCreateDTO;
import com.h2gas.model.dto.OrderItemRequestDTO;
import com.h2gas.model.dto.OrderPreviewDTO;
import com.h2gas.model.dto.OrderPreviewRequestDTO;
import com.h2gas.model.dto.ReorderItemDTO;
import com.h2gas.model.dto.ReorderPreviewDTO;
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
    private DelivererService delivererService;

    @Autowired
    private DelivererRepository delivererRepository;

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
    private Long validCustomerAddressId;
    private City testCity;


    @BeforeEach
    public void setUp() {
        // Criar cidade
        City city = new City(null, "Curitiba", "PR");
        cityRepository.save(city);
        testCity = city;

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
        validCustomerAddressId = customerAddress.getId();

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
        productItem.setPriceDelivery(new BigDecimal("89.90"));
        productItem.setPricePickup(new BigDecimal("79.90"));
        productItem.setStockQuantity(100);
        productItem.setImageUrl("example.com/image.jpg");
        productItem.setCategoryProduct(category);
        productItem.setSupplier(supplier);
        productItemRepository.save(productItem);
//        validProductItemId = productItem.getId();
        productItem = productItemRepository.saveAndFlush(productItem);

        validProductItemId = productItem.getId();
        System.out.println(">>> ProductItem salvo com ID: " + validProductItemId);

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
        requestDTO.setAddressId(validCustomerAddressId);
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

    private OrderRequestDTO novoPedido(Long addressId) {
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO();
        itemDTO.setProductItemRef(validProductItemId);
        itemDTO.setQuantity(1);

        OrderRequestDTO requestDTO = new OrderRequestDTO();
        requestDTO.setCustomerUuid(validCustomerId);
        requestDTO.setSupplierRef(validSupplierId);
        requestDTO.setPaymentMethod("PIX");
        requestDTO.setAddressId(addressId);
        requestDTO.setItems(List.of(itemDTO));
        return requestDTO;
    }

    @Test
    @DisplayName("HU04: deve usar o endereço escolhido no pedido em vez de inferir do usuário")
    void deveUsarAddressIdInformadoNoPedidoEmVezDeInferirDoUsuario() {
        // endereço mais novo do cliente — o comportamento antigo escolheria este
        User customer = userRepository.findById(validCustomerId).orElseThrow();
        addressRepository.saveAndFlush(new Address(
                null, "Rua do Trabalho", "Trabalho", "50", null, "Batel", "80420-000", testCity, customer));

        OrderResponseDTO result = orderService.createOrder(novoPedido(validCustomerAddressId));

        Order salvo = orderRepository.findById(result.getId()).orElseThrow();
        assertEquals(validCustomerAddressId, salvo.getDeliveryAddress().getId());
    }

    @Test
    @DisplayName("HU04: deve rejeitar pedido com endereço de outro cliente")
    void deveRejeitarPedidoComAddressIdQueNaoPertenceAoCliente() {
        User outro = userRepository.save(new User(
                null, "Outro Cliente", "outro@teste.com", "41900000000",
                "11122233344", "senha", UserTypeEnum.CUSTOMER));
        Address enderecoDeOutro = addressRepository.saveAndFlush(new Address(
                null, "Rua de Outro", "Casa", "1", null, "Centro", "80000-000", testCity, outro));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.createOrder(novoPedido(enderecoDeOutro.getId())));
        assertEquals("Endereço não pertence ao cliente informado.", ex.getMessage());
    }

    @Test
    @DisplayName("HU04: deve rejeitar pedido sem endereço de entrega")
    void deveRejeitarPedidoSemEnderecoDeEntrega() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.createOrder(novoPedido(null)));
        assertEquals("Informe o endereço de entrega.", ex.getMessage());
    }

    @Test
    @DisplayName("HU03: deve persistir a modalidade e cobrar o preço de retirada")
    void devePersistirDeliveryModeNoPedido() {
        OrderRequestDTO request = novoPedido(validCustomerAddressId);
        request.setDeliveryMode("PICKUP");

        OrderResponseDTO result = orderService.createOrder(request);

        Order salvo = orderRepository.findById(result.getId()).orElseThrow();
        assertEquals(DeliveryModeEnum.PICKUP, salvo.getDeliveryMode());
        assertEquals(new BigDecimal("79.90"), salvo.getTotalPrice());
    }

    @Test
    @DisplayName("HU05: pedido de retirada é criado sem endereço de entrega")
    void deveCriarPedidoDeRetiradaSemEndereco() {
        OrderRequestDTO request = novoPedido(null);
        request.setDeliveryMode("PICKUP");

        OrderResponseDTO result = orderService.createOrder(request);

        Order salvo = orderRepository.findById(result.getId()).orElseThrow();
        assertNull(salvo.getDeliveryAddress());
        assertEquals(DeliveryModeEnum.PICKUP, salvo.getDeliveryMode());
    }

    @Test
    @DisplayName("HU05: pedido de retirada ignora o endereço recebido")
    void deveIgnorarEnderecoEmPedidoDeRetirada() {
        OrderRequestDTO request = novoPedido(validCustomerAddressId);
        request.setDeliveryMode("PICKUP");

        OrderResponseDTO result = orderService.createOrder(request);

        assertNull(orderRepository.findById(result.getId()).orElseThrow().getDeliveryAddress());
    }

    @Test
    @DisplayName("HU05: a resposta do pedido expõe a modalidade")
    void deveExporModalidadeNaRespostaDoPedido() {
        OrderRequestDTO request = novoPedido(null);
        request.setDeliveryMode("PICKUP");

        assertEquals("PICKUP", orderService.createOrder(request).getDeliveryMode());
    }

    @Test
    @DisplayName("HU03: deve assumir entrega quando a modalidade não for informada")
    void deveAssumirEntregaQuandoModalidadeAusente() {
        OrderResponseDTO result = orderService.createOrder(novoPedido(validCustomerAddressId));

        Order salvo = orderRepository.findById(result.getId()).orElseThrow();
        assertEquals(DeliveryModeEnum.DELIVERY, salvo.getDeliveryMode());
        assertEquals(new BigDecimal("89.90"), salvo.getTotalPrice());
    }

    @Test
    @DisplayName("HU03: deve rejeitar modalidade desconhecida")
    void deveRejeitarModalidadeDesconhecida() {
        OrderRequestDTO request = novoPedido(validCustomerAddressId);
        request.setDeliveryMode("DRONE");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.createOrder(request));
        assertEquals("Modalidade de entrega inválida: DRONE", ex.getMessage());
    }

    private OrderPreviewRequestDTO novoPreview(String deliveryMode, int quantity) {
        OrderItemRequestDTO itemDTO = new OrderItemRequestDTO();
        itemDTO.setProductItemRef(validProductItemId);
        itemDTO.setQuantity(quantity);
        return new OrderPreviewRequestDTO(List.of(itemDTO), deliveryMode);
    }

    @Test
    @DisplayName("HU03: preview calcula o subtotal sem persistir pedido nem mexer no estoque")
    void deveCalcularSubtotalSemPersistirNadaNoPreview() {
        long pedidosAntes = orderRepository.count();

        OrderPreviewDTO preview = orderService.previewOrder(novoPreview("DELIVERY", 2));

        assertEquals(new BigDecimal("179.80"), preview.getSubtotal());
        assertEquals(1, preview.getItems().size());
        assertEquals(new BigDecimal("89.90"), preview.getItems().get(0).getUnitPrice());
        assertEquals(new BigDecimal("179.80"), preview.getItems().get(0).getTotalPrice());
        assertEquals(pedidosAntes, orderRepository.count());
        assertEquals(100, productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("HU03: preview reflete a modalidade informada")
    void devePreviewRefletirModalidadeInformada() {
        OrderPreviewDTO preview = orderService.previewOrder(novoPreview("PICKUP", 2));

        assertEquals(new BigDecimal("159.80"), preview.getSubtotal());
        assertEquals(new BigDecimal("79.90"), preview.getItems().get(0).getUnitPrice());
    }

    private String pedidoRealizado(int quantity) {
        OrderRequestDTO request = novoPedido(validCustomerAddressId);
        request.getItems().get(0).setQuantity(quantity);
        return orderService.createOrder(request).getId();
    }

    private User clienteLogado() {
        return userRepository.findById(validCustomerId).orElseThrow();
    }

    private void alterarProduto(java.util.function.Consumer<ProductItem> alteracao) {
        ProductItem produto = productItemRepository.findById(validProductItemId).orElseThrow();
        alteracao.accept(produto);
        productItemRepository.saveAndFlush(produto);
    }

    @Test
    @DisplayName("HU05: deve montar a recompra com todos os itens disponíveis")
    void deveMontarPreviewComTodosItensDisponiveis() {
        String orderId = pedidoRealizado(2);

        ReorderPreviewDTO preview = orderService.buildReorderPreview(orderId, clienteLogado());

        assertEquals(1, preview.getAvailableItems().size());
        assertTrue(preview.getUnavailableItems().isEmpty());
        ReorderItemDTO item = preview.getAvailableItems().get(0);
        assertEquals(validProductItemId, item.getProductId());
        assertEquals(2, item.getQuantity());
        assertEquals("Marca Teste", item.getProductBrand());
        assertNull(item.getUnavailableReason());
    }

    @Test
    @DisplayName("HU05: deve trazer o fornecedor nos itens disponíveis (checkout agrupa por fornecedor)")
    void deveTrazerSupplierIdNosItensDisponiveis() {
        ReorderPreviewDTO preview = orderService.buildReorderPreview(pedidoRealizado(1), clienteLogado());

        assertEquals(validSupplierId, preview.getAvailableItems().get(0).getSupplierId());
    }

    @Test
    @DisplayName("HU05: deve marcar item como indisponível quando o produto foi removido")
    void deveMarcarItemComoIndisponivelQuandoProdutoForRemovido() {
        String orderId = pedidoRealizado(1);
        Order pedido = orderRepository.findById(orderId).orElseThrow();
        pedido.getItems().forEach(item -> item.setProduct(null));
        orderRepository.saveAndFlush(pedido);

        ReorderPreviewDTO preview = orderService.buildReorderPreview(orderId, clienteLogado());

        assertTrue(preview.getAvailableItems().isEmpty());
        assertEquals("PRODUTO_REMOVIDO", preview.getUnavailableItems().get(0).getUnavailableReason());
    }

    @Test
    @DisplayName("HU05: deve marcar item como indisponível quando o estoque zerou")
    void deveMarcarItemComoIndisponivelQuandoEstoqueZerado() {
        String orderId = pedidoRealizado(1);
        alterarProduto(p -> p.setStockQuantity(0));

        ReorderPreviewDTO preview = orderService.buildReorderPreview(orderId, clienteLogado());

        assertTrue(preview.getAvailableItems().isEmpty());
        ReorderItemDTO item = preview.getUnavailableItems().get(0);
        assertEquals("SEM_ESTOQUE", item.getUnavailableReason());
        assertEquals("Marca Teste", item.getProductBrand());
    }

    @Test
    @DisplayName("HU05: deve limitar a quantidade ao estoque atual")
    void deveLimitarQuantidadeAoEstoqueAtualQuandoInsuficiente() {
        String orderId = pedidoRealizado(3);
        alterarProduto(p -> p.setStockQuantity(2));

        ReorderPreviewDTO preview = orderService.buildReorderPreview(orderId, clienteLogado());

        assertEquals(2, preview.getAvailableItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("HU05: deve usar o preço atual do produto, não o do pedido antigo")
    void deveUsarPrecoAtualDoProdutoNaoOHistorico() {
        String orderId = pedidoRealizado(1);
        alterarProduto(p -> {
            p.setPriceDelivery(new BigDecimal("99.90"));
            p.setPricePickup(new BigDecimal("85.00"));
        });

        ReorderItemDTO item = orderService.buildReorderPreview(orderId, clienteLogado()).getAvailableItems().get(0);

        assertEquals(99.90, item.getPriceDelivery());
        assertEquals(85.00, item.getPricePickup());
    }

    @Test
    @DisplayName("HU05: deve rejeitar recompra de pedido de outro cliente")
    void deveRejeitarPreviewDePedidoQueNaoPertenceAoCliente() {
        String orderId = pedidoRealizado(1);
        User outro = userRepository.save(new User(
                null, "Outro Cliente", "outro@teste.com", "41900000000",
                "11122233344", "senha", UserTypeEnum.CUSTOMER));

        assertThrows(ForbiddenResourceException.class,
                () -> orderService.buildReorderPreview(orderId, outro));
    }

    @Test
    @DisplayName("HU05: a recompra não altera o estoque")
    void naoDeveAlterarEstoqueNoReorderPreview() {
        String orderId = pedidoRealizado(1);
        int estoqueAntes = productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity();

        orderService.buildReorderPreview(orderId, clienteLogado());

        assertEquals(estoqueAntes, productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("HU06: a resposta do pedido traz o telefone do cliente")
    void deveExporTelefoneDoClienteNoPedido() {
        OrderResponseDTO result = orderService.createOrder(novoPedido(validCustomerAddressId));

        assertEquals("41987654321", result.getCustomerPhone());
        assertNull(result.getCancellationReason());
    }

    private User fornecedorDoPedido() {
        return supplierRepository.findById(validSupplierId).orElseThrow();
    }

    private Supplier outroFornecedor() {
        return supplierRepository.save(createSupplier("Outro Fornecedor", "outro.fornecedor@teste.com",
                "41955555555", "55566677788", "senha", "99887766000155", "Outro", true, true, null, testCity));
    }

    @Test
    @DisplayName("HU06: cancelar exige justificativa")
    void deveRejeitarCancelamentoSemJustificativa() {
        String orderId = pedidoRealizado(1);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.cancelOrder(orderId, "   ", fornecedorDoPedido()));
        assertEquals("Justificativa é obrigatória para cancelar um pedido.", ex.getMessage());
        assertEquals(OrderStatusEnum.PENDING, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU06: cancelar com justificativa grava status e motivo")
    void deveCancelarPedidoComJustificativaValida() {
        String orderId = pedidoRealizado(1);

        orderService.cancelOrder(orderId, "Sem entregador disponível hoje", fornecedorDoPedido());

        Order cancelado = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatusEnum.CANCELED, cancelado.getStatus());
        assertEquals("Sem entregador disponível hoje", cancelado.getCancellationReason());
    }

    @Test
    @DisplayName("HU06: cancelar devolve o estoque dos itens")
    void deveDevolverEstoqueAoCancelar() {
        String orderId = pedidoRealizado(3);
        assertEquals(97, productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity());

        orderService.cancelOrder(orderId, "Cliente desistiu por telefone", fornecedorDoPedido());

        assertEquals(100, productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("HU06: fornecedor não cancela pedido de outro fornecedor")
    void deveRejeitarCancelamentoDePedidoDeOutroFornecedor() {
        String orderId = pedidoRealizado(1);
        Supplier outro = outroFornecedor();

        assertThrows(ForbiddenResourceException.class,
                () -> orderService.cancelOrder(orderId, "Não é meu", outro));
        assertEquals(OrderStatusEnum.PENDING, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU06: pedido entregue ou já cancelado não pode ser cancelado")
    void deveRejeitarCancelamentoDePedidoEntregueOuJaCancelado() {
        String entregue = pedidoRealizado(1);
        orderService.updateOrderStatus(entregue, "DELIVERED");
        assertThrows(BusinessRuleException.class,
                () -> orderService.cancelOrder(entregue, "Tarde demais", fornecedorDoPedido()));

        String cancelado = pedidoRealizado(1);
        orderService.cancelOrder(cancelado, "Primeiro cancelamento", fornecedorDoPedido());
        assertThrows(BusinessRuleException.class,
                () -> orderService.cancelOrder(cancelado, "De novo", fornecedorDoPedido()));
        // entregue tirou 1 (99), o segundo pedido tirou 1 (98), o primeiro cancelamento devolveu 1 (99)
        assertEquals(99, productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity(),
                "o segundo cancelamento não pode devolver estoque de novo");
    }

    private Long entregadorDe(String supplierId, String email) {
        return delivererService.createDeliverer(
                new DelivererCreateDTO("Carlos Silva", email, "41988887777", "senha123", "Moto - ABC-1234"), supplierId).getId();
    }

    private String pedidoEmPreparo(String deliveryMode) {
        OrderRequestDTO request = novoPedido(validCustomerAddressId);
        request.setDeliveryMode(deliveryMode);
        String orderId = orderService.createOrder(request).getId();
        orderService.updateOrderStatus(orderId, "CONFIRMED");
        orderService.updateOrderStatus(orderId, "IN_PREPARATION");
        return orderId;
    }

    @Test
    @DisplayName("HU07: despachar atribui o entregador, vai a OUT_FOR_DELIVERY e grava dispatchedAt")
    void deveDespacharEntregadorEAvancarParaOutForDelivery() {
        String orderId = pedidoEmPreparo("DELIVERY");
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");

        OrderResponseDTO resposta = orderService.assignDeliverer(orderId, entregadorId, fornecedorDoPedido());

        Order despachado = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, despachado.getStatus());
        assertEquals(entregadorId, despachado.getDeliverer().getId());
        assertNotNull(despachado.getDispatchedAt());
        assertEquals("Carlos Silva", resposta.getDelivererName());
    }

    @Test
    @DisplayName("HU07: só despacha pedido em preparação")
    void deveRejeitarDespachoForaDeEmPreparacao() {
        String orderId = pedidoRealizado(1);
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");

        assertThrows(BusinessRuleException.class,
                () -> orderService.assignDeliverer(orderId, entregadorId, fornecedorDoPedido()));
        assertNull(orderRepository.findById(orderId).orElseThrow().getDeliverer());
    }

    @Test
    @DisplayName("HU07: pedido de retirada não tem entregador")
    void deveRejeitarDespachoDePedidoDeRetirada() {
        String orderId = pedidoEmPreparo("PICKUP");
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");

        assertThrows(BusinessRuleException.class,
                () -> orderService.assignDeliverer(orderId, entregadorId, fornecedorDoPedido()));
    }

    @Test
    @DisplayName("HU07: fornecedor não despacha pedido de outro fornecedor")
    void deveRejeitarDespachoDePedidoDeOutroFornecedor() {
        String orderId = pedidoEmPreparo("DELIVERY");
        Supplier outro = outroFornecedor();
        Long entregadorId = entregadorDe(outro.getId(), "outro.entregador@teste.com");

        assertThrows(ForbiddenResourceException.class, () -> orderService.assignDeliverer(orderId, entregadorId, outro));
        assertEquals(OrderStatusEnum.IN_PREPARATION, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07: não usa entregador da frota de outro fornecedor")
    void deveRejeitarEntregadorDeOutroFornecedor() {
        String orderId = pedidoEmPreparo("DELIVERY");
        Long entregadorDeOutro = entregadorDe(outroFornecedor().getId(), "outro.entregador@teste.com");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.assignDeliverer(orderId, entregadorDeOutro, fornecedorDoPedido()));
        assertEquals("Entregador não pertence ao fornecedor do pedido.", ex.getMessage());
    }

    @Test
    @DisplayName("HU07: entregador já em rota pode receber outro pedido (o badge só avisa)")
    void devePermitirDespacharEntregadorJaEmRota() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String primeiro = pedidoEmPreparo("DELIVERY");
        String segundo = pedidoEmPreparo("DELIVERY");

        orderService.assignDeliverer(primeiro, entregadorId, fornecedorDoPedido());
        orderService.assignDeliverer(segundo, entregadorId, fornecedorDoPedido());

        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(segundo).orElseThrow().getStatus());
        assertTrue(delivererService.findMine(validSupplierId).get(0).isBusy());
    }

    @Test
    @DisplayName("HU07: o atalho 'enviar para entrega' é recusado em pedido de entrega, mas vale na retirada")
    void deveRecusarAtalhoDeEnvioParaPedidoDeEntrega() {
        String entrega = pedidoEmPreparo("DELIVERY");
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.updateOrderStatus(entrega, "OUT_FOR_DELIVERY"));
        assertEquals("Use o despacho com entregador.", ex.getMessage());

        String retirada = pedidoEmPreparo("PICKUP");
        assertEquals("Saiu para entrega", orderService.updateOrderStatus(retirada, "OUT_FOR_DELIVERY"));
    }

    private String pedidoEmRota(Long entregadorId) {
        String orderId = pedidoEmPreparo("DELIVERY");
        orderService.assignDeliverer(orderId, entregadorId, fornecedorDoPedido());
        return orderId;
    }

    @Test
    @DisplayName("HU07: o entregador confirma a entrega e o pedido vai a DELIVERED com deliveredAt")
    void deveConcluirEntregaEGravarDeliveredAt() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);

        OrderResponseDTO resposta = orderService.completeDelivery(orderId, entregadorId);

        Order entregue = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatusEnum.DELIVERED, entregue.getStatus());
        assertNotNull(entregue.getDeliveredAt());
        assertEquals("Entregue", resposta.getStatus());
    }

    @Test
    @DisplayName("HU07: entregador não conclui pedido atribuído a outro entregador")
    void deveRejeitarConclusaoDePedidoDeOutroEntregador() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        String orderId = pedidoEmRota(carlos);

        assertThrows(ForbiddenResourceException.class, () -> orderService.completeDelivery(orderId, ana));
        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07: pedido sem entregador também é recusado")
    void deveRejeitarConclusaoDePedidoSemEntregador() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmPreparo("DELIVERY");

        assertThrows(ForbiddenResourceException.class, () -> orderService.completeDelivery(orderId, carlos));
    }

    @Test
    @DisplayName("HU07: só conclui pedido em rota (não reconclui entrega feita)")
    void deveRejeitarConclusaoForaDeOutForDelivery() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);
        orderService.completeDelivery(orderId, entregadorId);

        assertThrows(BusinessRuleException.class, () -> orderService.completeDelivery(orderId, entregadorId));
    }

    @Test
    @DisplayName("HU07: a rota do entregador traz só os pedidos em rota dele, do mais antigo ao mais novo")
    void deveListarApenasRotasDoEntregador() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        String primeiro = pedidoEmRota(carlos);
        String segundo = pedidoEmRota(carlos);
        pedidoEmRota(ana);
        String jaEntregue = pedidoEmRota(carlos);
        orderService.completeDelivery(jaEntregue, carlos);

        List<OrderResponseDTO> rotas = orderService.findActiveByDeliverer(carlos);

        assertEquals(List.of(primeiro, segundo), rotas.stream().map(OrderResponseDTO::getId).toList());
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
        Page<OrderResponseDTO> result = orderService.findByCustomerId(validCustomerId, null, pageable);

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