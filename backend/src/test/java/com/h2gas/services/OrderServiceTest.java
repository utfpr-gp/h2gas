package com.h2gas.services;

import com.h2gas.model.dto.OrderHistorySummaryDTO;
import com.h2gas.model.dto.OrderHistoryFilter;
import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.DelivererCreateDTO;
import com.h2gas.model.dto.DeliveryHistoryDTO;
import com.h2gas.model.dto.DeliveryHistoryItemDTO;
import com.h2gas.events.OrderStatusChangedEvent;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@TestPropertySource(properties = "jwt.secret=segredoFake123")
@Transactional
@RecordApplicationEvents
class OrderServiceTest {

    // HU08: o serviço publica o evento de domínio; o envio SSE (após o commit) é testado no listener
    @Autowired
    private ApplicationEvents applicationEvents;


    @Autowired
    private OrderService orderService;

    @Autowired
    private DelivererService delivererService;

    @Autowired
    private DelivererRepository delivererRepository;

    @Autowired
    private DeliveryRefusalRepository deliveryRefusalRepository;

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
                null
        );
        categoryProductRepository.save(category);

        // Criar produto
        ProductItem productItem = new ProductItem();
        productItem.setBrand("Marca Teste");
        productItem.setPrice(new BigDecimal("79.90"));
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
        assertEquals(new BigDecimal("159.80"), result.getTotalPrice());
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
        assertEquals(new BigDecimal("79.90"), salvo.getTotalPrice());
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

        assertEquals(new BigDecimal("159.80"), preview.getSubtotal());
        assertEquals(1, preview.getItems().size());
        assertEquals(new BigDecimal("79.90"), preview.getItems().get(0).getUnitPrice());
        assertEquals(new BigDecimal("159.80"), preview.getItems().get(0).getTotalPrice());
        assertEquals(pedidosAntes, orderRepository.count());
        assertEquals(100, productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity());
    }

    @Test
    @DisplayName("HU21: o preview usa o mesmo preço na entrega e na retirada")
    void devePreviewUsarOMesmoPrecoNasDuasModalidades() {
        OrderPreviewDTO retirada = orderService.previewOrder(novoPreview("PICKUP", 2));
        OrderPreviewDTO entrega = orderService.previewOrder(novoPreview("DELIVERY", 2));

        assertEquals(new BigDecimal("159.80"), retirada.getSubtotal());
        assertEquals(retirada.getSubtotal(), entrega.getSubtotal());
        assertEquals(new BigDecimal("79.90"), retirada.getItems().get(0).getUnitPrice());
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
            p.setPrice(new BigDecimal("85.00"));
        });

        ReorderItemDTO item = orderService.buildReorderPreview(orderId, clienteLogado()).getAvailableItems().get(0);

        assertEquals(85.00, item.getPrice());
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

    private List<OrderStatusChangedEvent> eventosDeStatus() {
        return applicationEvents.stream(OrderStatusChangedEvent.class).toList();
    }

    private String clienteDoPedido(String orderId) {
        return orderRepository.findById(orderId).orElseThrow().getCustomer().getId();
    }

    @Test
    @DisplayName("HU08: atualizar o status publica o evento para o cliente do pedido")
    void devePublicarEventoAoAtualizarStatus() {
        String orderId = orderService.createOrder(novoPedido(validCustomerAddressId)).getId();
        applicationEvents.clear();

        orderService.updateOrderStatus(orderId, "CONFIRMED");

        assertEquals(List.of(new OrderStatusChangedEvent(orderId, clienteDoPedido(orderId), "Confirmado")), eventosDeStatus());
    }

    @Test
    @DisplayName("HU08: cancelar o pedido publica o evento com status Cancelado")
    void devePublicarEventoAoCancelarPedido() {
        String orderId = pedidoEmPreparo("DELIVERY");
        applicationEvents.clear();

        orderService.cancelOrder(orderId, "Sem gás hoje", fornecedorDoPedido());

        assertEquals(List.of(new OrderStatusChangedEvent(orderId, clienteDoPedido(orderId), "Cancelado")), eventosDeStatus());
    }

    @Test
    @DisplayName("HU08: o entregador iniciar publica Saiu para entrega")
    void devePublicarEventoAoIniciarEntrega() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoAtribuido(entregadorId);
        applicationEvents.clear();

        orderService.startDelivery(orderId, entregadorId);

        assertEquals(List.of(new OrderStatusChangedEvent(orderId, clienteDoPedido(orderId), "Saiu para entrega")), eventosDeStatus());
    }

    @Test
    @DisplayName("HU08: o entregador concluir publica Entregue")
    void devePublicarEventoAoConcluirEntrega() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);
        applicationEvents.clear();

        orderService.completeDelivery(orderId, entregadorId);

        assertEquals(List.of(new OrderStatusChangedEvent(orderId, clienteDoPedido(orderId), "Entregue")), eventosDeStatus());
    }

    @Test
    @DisplayName("HU08 (decisão a): a recusa do entregador não é publicada ao cliente")
    void naoDevePublicarEventoAoRecusarEntrega() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);
        applicationEvents.clear();

        orderService.cancelDelivery(orderId, entregadorId, "Moto quebrou");

        assertTrue(eventosDeStatus().isEmpty());
    }

    @Test
    @DisplayName("HU08: despachar só atribui, sem mudar o status, e não publica")
    void naoDevePublicarEventoAoAtribuirEntregador() {
        String orderId = pedidoEmPreparo("DELIVERY");
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        applicationEvents.clear();

        orderService.assignDeliverer(orderId, entregadorId, fornecedorDoPedido());

        assertTrue(eventosDeStatus().isEmpty());
    }

    @Test
    @DisplayName("HU07: despachar só atribui o entregador; o pedido segue em preparo, sem dispatchedAt")
    void deveAtribuirEntregadorSemAvancarStatus() {
        String orderId = pedidoEmPreparo("DELIVERY");
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");

        OrderResponseDTO resposta = orderService.assignDeliverer(orderId, entregadorId, fornecedorDoPedido());

        Order atribuido = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatusEnum.IN_PREPARATION, atribuido.getStatus());
        assertEquals(entregadorId, atribuido.getDeliverer().getId());
        assertNull(atribuido.getDispatchedAt());
        assertEquals("Carlos Silva", resposta.getDelivererName());
    }

    @Test
    @DisplayName("HU07: o fornecedor pode trocar o entregador enquanto ele não iniciou a entrega")
    void devePermitirTrocarEntregadorEnquantoEmPreparacao() {
        String orderId = pedidoEmPreparo("DELIVERY");
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        orderService.assignDeliverer(orderId, carlos, fornecedorDoPedido());

        orderService.assignDeliverer(orderId, ana, fornecedorDoPedido());

        assertEquals(ana, orderRepository.findById(orderId).orElseThrow().getDeliverer().getId());
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
        String primeiro = pedidoEmRota(entregadorId);
        String segundo = pedidoEmPreparo("DELIVERY");

        orderService.assignDeliverer(segundo, entregadorId, fornecedorDoPedido());

        assertEquals(entregadorId, orderRepository.findById(segundo).orElseThrow().getDeliverer().getId());
        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(primeiro).orElseThrow().getStatus());
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

    private String pedidoAtribuido(Long entregadorId) {
        String orderId = pedidoEmPreparo("DELIVERY");
        orderService.assignDeliverer(orderId, entregadorId, fornecedorDoPedido());
        return orderId;
    }

    private String pedidoEmRota(Long entregadorId) {
        String orderId = pedidoAtribuido(entregadorId);
        orderService.startDelivery(orderId, entregadorId);
        return orderId;
    }

    @Test
    @DisplayName("HU07: o entregador inicia a entrega, o pedido vai a OUT_FOR_DELIVERY e grava dispatchedAt")
    void deveIniciarEntregaEGravarDispatchedAt() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoAtribuido(entregadorId);

        OrderResponseDTO resposta = orderService.startDelivery(orderId, entregadorId);

        Order emRota = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, emRota.getStatus());
        assertNotNull(emRota.getDispatchedAt());
        assertEquals("Saiu para entrega", resposta.getStatus());
    }

    @Test
    @DisplayName("HU07: entregador não inicia pedido atribuído a outro entregador")
    void deveRejeitarInicioDePedidoDeOutroEntregador() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        String orderId = pedidoAtribuido(carlos);

        assertThrows(ForbiddenResourceException.class, () -> orderService.startDelivery(orderId, ana));
        assertEquals(OrderStatusEnum.IN_PREPARATION, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07: só inicia pedido em preparo (não reinicia entrega em rota)")
    void deveRejeitarInicioForaDeEmPreparacao() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);

        assertThrows(BusinessRuleException.class, () -> orderService.startDelivery(orderId, entregadorId));
    }

    @Test
    @DisplayName("HU07 (3ª revisão): entregador com uma entrega em rota não inicia outra")
    void deveRejeitarInicioComOutraEntregaEmRota() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        pedidoEmRota(entregadorId);
        String segundo = pedidoAtribuido(entregadorId);

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> orderService.startDelivery(segundo, entregadorId));

        assertEquals("Finalize a entrega em rota antes de iniciar outra.", erro.getMessage());
        assertEquals(OrderStatusEnum.IN_PREPARATION, orderRepository.findById(segundo).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07 (3ª revisão): depois de confirmar a entrega em rota, o entregador inicia a próxima")
    void devePermitirIniciarNovaEntregaDepoisDeConcluirAAnterior() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String primeiro = pedidoEmRota(entregadorId);
        String proximo = pedidoAtribuido(entregadorId);
        orderService.completeDelivery(primeiro, entregadorId);

        orderService.startDelivery(proximo, entregadorId);

        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(proximo).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07 (3ª revisão): depois de cancelar a entrega em rota, o entregador inicia a próxima")
    void devePermitirIniciarNovaEntregaDepoisDeCancelarAAnterior() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String primeiro = pedidoEmRota(entregadorId);
        String proximo = pedidoAtribuido(entregadorId);
        orderService.cancelDelivery(primeiro, entregadorId, "Moto quebrou");

        orderService.startDelivery(proximo, entregadorId);

        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(proximo).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07 (3ª revisão): a entrega em rota de um entregador não impede outro entregador de iniciar")
    void devePermitirOutroEntregadorIniciarComColegaEmRota() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        pedidoEmRota(carlos);
        String deAna = pedidoAtribuido(ana);

        orderService.startDelivery(deAna, ana);

        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(deAna).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07 (3ª revisão): a regra vale só para iniciar — o fornecedor ainda atribui a quem está em rota")
    void deveAtribuirPedidoAEntregadorEmRotaSemIniciar() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        pedidoEmRota(entregadorId);

        String segundo = pedidoAtribuido(entregadorId);

        Order atribuido = orderRepository.findById(segundo).orElseThrow();
        assertEquals(OrderStatusEnum.IN_PREPARATION, atribuido.getStatus());
        assertEquals(entregadorId, atribuido.getDeliverer().getId());
    }

    @Test
    @DisplayName("HU07: recusa devolve o pedido ao fornecedor, sem cancelar nem mexer no estoque")
    void deveCancelarEntregaDevolvendoPedidoAoFornecedor() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);
        int estoque = productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity();

        orderService.cancelDelivery(orderId, entregadorId, "  Moto quebrou  ");

        Order devolvido = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatusEnum.IN_PREPARATION, devolvido.getStatus());
        assertNull(devolvido.getDeliverer());
        assertNull(devolvido.getDispatchedAt());
        assertNull(devolvido.getCancellationReason());
        assertEquals(estoque, productItemRepository.findById(validProductItemId).orElseThrow().getStockQuantity());
        List<DeliveryRefusal> recusas = deliveryRefusalRepository.findByDelivererIdOrderByCreatedAtDesc(entregadorId);
        assertEquals(1, recusas.size());
        assertEquals("Moto quebrou", recusas.get(0).getReason());
        assertEquals(orderId, recusas.get(0).getOrder().getId());
    }

    @Test
    @DisplayName("HU07: também recusa pedido atribuído que ainda não iniciou")
    void deveCancelarEntregaAindaNaoIniciada() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoAtribuido(entregadorId);

        orderService.cancelDelivery(orderId, entregadorId, "Fora da minha região");

        assertNull(orderRepository.findById(orderId).orElseThrow().getDeliverer());
        assertEquals(OrderStatusEnum.IN_PREPARATION, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07: recusa exige motivo")
    void deveRejeitarCancelamentoDeEntregaSemMotivo() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);

        assertThrows(BusinessRuleException.class, () -> orderService.cancelDelivery(orderId, entregadorId, "   "));
        assertEquals(entregadorId, orderRepository.findById(orderId).orElseThrow().getDeliverer().getId());
    }

    @Test
    @DisplayName("HU07: entregador não recusa pedido atribuído a outro entregador")
    void deveRejeitarCancelamentoDeEntregaDeOutroEntregador() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        String orderId = pedidoEmRota(carlos);

        assertThrows(ForbiddenResourceException.class, () -> orderService.cancelDelivery(orderId, ana, "Não é meu"));
        assertEquals(OrderStatusEnum.OUT_FOR_DELIVERY, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07: não recusa pedido já entregue")
    void deveRejeitarCancelamentoDePedidoJaEntregue() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);
        orderService.completeDelivery(orderId, entregadorId);

        assertThrows(BusinessRuleException.class, () -> orderService.cancelDelivery(orderId, entregadorId, "Tarde demais"));
        assertEquals(OrderStatusEnum.DELIVERED, orderRepository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("HU07: o pedido expõe a cidade de entrega e, após recusa, o motivo e quem recusou")
    void deveExporCidadeEUltimaRecusaNoPedido() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        String orderId = pedidoEmRota(carlos);

        assertEquals("Curitiba", orderService.findByIdOrder(orderId).getCityName());
        assertNull(orderService.findByIdOrder(orderId).getLastRefusalReason());

        orderService.cancelDelivery(orderId, carlos, "Moto quebrou");

        OrderResponseDTO devolvido = orderService.findByIdOrder(orderId);
        assertEquals("Moto quebrou", devolvido.getLastRefusalReason());
        assertEquals("Carlos Silva", devolvido.getLastRefusalBy());
        assertNull(devolvido.getDelivererName());

        orderService.assignDeliverer(orderId, ana, fornecedorDoPedido());
        assertNull(orderService.findByIdOrder(orderId).getLastRefusalReason(),
                "com novo entregador atribuído a recusa antiga não aparece mais");
    }

    @Test
    @DisplayName("HU07: o pedido entregue expõe deliveredAt")
    void deveExporDeliveredAtNoPedidoEntregue() {
        Long entregadorId = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoEmRota(entregadorId);

        OrderResponseDTO entregue = orderService.completeDelivery(orderId, entregadorId);

        assertNotNull(entregue.getDeliveredAt());
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
    @DisplayName("HU07: não conclui pedido atribuído que ainda não iniciou a entrega")
    void deveRejeitarConclusaoDePedidoAindaNaoIniciado() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        String orderId = pedidoAtribuido(carlos);

        assertThrows(BusinessRuleException.class, () -> orderService.completeDelivery(orderId, carlos));
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
    @DisplayName("HU07: a rota traz só pedidos dele: em rota (despacho mais antigo primeiro) e depois os pendentes")
    void deveListarRotasPendentesEEmRotaApenasDoEntregador() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        String pendente = pedidoAtribuido(carlos);
        String jaEntregue = pedidoEmRota(carlos);
        orderService.completeDelivery(jaEntregue, carlos);
        String recusado = pedidoEmRota(carlos);
        orderService.cancelDelivery(recusado, carlos, "Moto quebrou");
        String primeiro = pedidoEmRota(carlos);
        // 3ª revisão: só uma em rota por vez; duas em rota só existem em dados de antes da regra
        String segundo = pedidoAtribuido(carlos);
        Order legado = orderRepository.findById(segundo).orElseThrow();
        legado.setStatus(OrderStatusEnum.OUT_FOR_DELIVERY);
        legado.setDispatchedAt(LocalDateTime.now().plusMinutes(1));
        orderRepository.save(legado);
        pedidoEmRota(ana);

        List<OrderResponseDTO> rotas = orderService.findActiveByDeliverer(carlos);

        assertEquals(List.of(primeiro, segundo, pendente), rotas.stream().map(OrderResponseDTO::getId).toList());
    }

    @Test
    @DisplayName("HU07: o histórico mistura entregues e recusadas dele, da mais recente para a mais antiga")
    void deveMontarHistoricoComEntreguesERecusadas() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        Long ana = entregadorDe(validSupplierId, "ana@teste.com");
        String entregue = pedidoEmRota(carlos);
        orderService.completeDelivery(entregue, carlos);
        String recusado = pedidoEmRota(carlos);
        orderService.cancelDelivery(recusado, carlos, "Moto quebrou");
        String deAna = pedidoEmRota(ana);
        orderService.completeDelivery(deAna, ana);
        pedidoEmRota(carlos);

        DeliveryHistoryDTO historico = orderService.findDeliveryHistory(carlos);

        assertEquals(List.of(recusado, entregue), historico.getItems().stream().map(DeliveryHistoryItemDTO::getOrderId).toList());
        DeliveryHistoryItemDTO itemRecusado = historico.getItems().get(0);
        assertEquals("REFUSED", itemRecusado.getKind());
        assertNull(itemRecusado.getTotalPrice());
        DeliveryHistoryItemDTO itemEntregue = historico.getItems().get(1);
        assertEquals("DELIVERED", itemEntregue.getKind());
        assertNotNull(itemEntregue.getTotalPrice());
        assertEquals("Curitiba", itemEntregue.getCityName());
        assertFalse(itemEntregue.getItemsSummary().isBlank());
        assertNotNull(itemEntregue.getOccurredAt());
    }

    @Test
    @DisplayName("HU07: 'Entregas hoje' conta só as entregues no dia corrente")
    void deveContarEntregasDeHoje() {
        Long carlos = entregadorDe(validSupplierId, "carlos@teste.com");
        String hoje = pedidoEmRota(carlos);
        orderService.completeDelivery(hoje, carlos);
        String antigo = pedidoEmRota(carlos);
        orderService.completeDelivery(antigo, carlos);
        Order pedidoAntigo = orderRepository.findById(antigo).orElseThrow();
        pedidoAntigo.setDeliveredAt(java.time.LocalDateTime.now().minusDays(2));
        orderRepository.save(pedidoAntigo);

        DeliveryHistoryDTO historico = orderService.findDeliveryHistory(carlos);

        assertEquals(1, historico.getDeliveredToday());
        assertEquals(2, historico.getItems().size());
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
        // HU18: aberta/fechada pelo horário (o relógio dos testes marca 10:00)
        supplier.setOpeningTime(java.time.LocalTime.of(open ? 8 : 19, 0));
        supplier.setClosingTime(java.time.LocalTime.of(open ? 18 : 23, 0));

        return supplier;
    }


    // ---- F7 fatia 6: histórico do fornecedor ----
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private Order pedidoFinalizado(Supplier supplier, User customer, OrderStatusEnum status, String payment,
                                   DeliveryModeEnum mode, String total, String createdAt) {
        Order o = new Order();
        o.setCustomer(customer);
        o.setSupplier(supplier);
        o.setStatus(status);
        o.setPaymentMethod(payment);
        o.setDeliveryMode(mode);
        o.setTotalPrice(new BigDecimal(total));
        o = orderRepository.saveAndFlush(o);
        entityManager.createNativeQuery("UPDATE orders SET created_at = :d WHERE id = :id")
                .setParameter("d", java.sql.Timestamp.valueOf(createdAt + " 10:00:00")).setParameter("id", o.getId()).executeUpdate();
        return o;
    }

    private void criarHistorico() {
        Supplier supplier = supplierRepository.findById(validSupplierId).orElseThrow();
        User cliente = userRepository.findById(validCustomerId).orElseThrow();
        User maria = userRepository.save(new User(null, "Maria Busca", "maria@teste.com", "41900000000", "11122233344", "senha", UserTypeEnum.CUSTOMER));
        pedidoFinalizado(supplier, cliente, OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, "100.00", "2026-10-05");
        pedidoFinalizado(supplier, maria, OrderStatusEnum.DELIVERED, "DINHEIRO", DeliveryModeEnum.PICKUP, "50.00", "2026-09-20");
        pedidoFinalizado(supplier, cliente, OrderStatusEnum.CANCELED, "PIX", DeliveryModeEnum.DELIVERY, "80.00", "2026-10-10");
        Supplier outro = supplierRepository.save(createSupplier("Outra Loja", "outra@teste.com", "41911112222", "55566677788",
                "senha", "11222333000155", "outra", true, true, null, testCity));
        pedidoFinalizado(outro, cliente, OrderStatusEnum.DELIVERED, "PIX", DeliveryModeEnum.DELIVERY, "999.00", "2026-10-05");
        entityManager.clear();
    }

    private OrderHistoryFilter filtro(String status, String from, String to, String payment, String mode, String q) {
        return OrderHistoryFilter.parse(status, from, to, payment, mode, q);
    }

    @Test
    @DisplayName("F7 fatia 6: histórico traz só entregues e cancelados do fornecedor, mais recentes primeiro")
    void deveListarSoFinalizadosDoFornecedor() {
        criarHistorico();

        Page<OrderResponseDTO> todos = orderService.findMyHistory(validSupplierId, filtro(null, null, null, null, null, null), PageRequest.of(0, 10));

        assertEquals(3, todos.getTotalElements()); // o PENDING do setUp e o pedido da outra loja ficam de fora
        assertEquals(List.of(new BigDecimal("80.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
                todos.getContent().stream().map(OrderResponseDTO::getTotalPrice).toList());
    }

    @Test
    @DisplayName("F7 fatia 6: filtros de status, período, pagamento, modo e busca")
    void deveFiltrarPorPeriodoPagamentoModoEBusca() {
        criarHistorico();
        java.util.function.Function<OrderHistoryFilter, Long> total =
                f -> orderService.findMyHistory(validSupplierId, f, PageRequest.of(0, 10)).getTotalElements();

        assertEquals(2, total.apply(filtro("delivered", null, null, null, null, null)));
        assertEquals(1, total.apply(filtro("canceled", null, null, null, null, null)));
        assertEquals(2, total.apply(filtro(null, "2026-10-01", "2026-10-31", null, null, null)));
        assertEquals(2, total.apply(filtro(null, null, null, "PIX", null, null)));
        assertEquals(1, total.apply(filtro(null, null, null, null, "PICKUP", null)));
        assertEquals(1, total.apply(filtro(null, null, null, null, null, "maria")));
        assertEquals(1, total.apply(filtro("delivered", "2026-10-01", "2026-10-31", "PIX", "DELIVERY", "cliente")));
    }

    @Test
    @DisplayName("F7 fatia 6: resumo do histórico conta e soma no banco com os mesmos filtros")
    void deveResumirReceitaEContagensComOsFiltros() {
        criarHistorico();

        OrderHistorySummaryDTO tudo = orderService.summarizeMyHistory(validSupplierId, filtro(null, null, null, null, null, null));
        assertEquals(2, tudo.delivered());
        assertEquals(1, tudo.canceled());
        assertEquals(0, new BigDecimal("150.00").compareTo(tudo.revenue()));
        assertEquals(0, new BigDecimal("75.00").compareTo(tudo.averageTicket()));

        OrderHistorySummaryDTO pix = orderService.summarizeMyHistory(validSupplierId, filtro(null, null, null, "PIX", null, null));
        assertEquals(1, pix.delivered());
        assertEquals(0, new BigDecimal("100.00").compareTo(pix.revenue()));

        OrderHistorySummaryDTO vazio = orderService.summarizeMyHistory(validSupplierId, filtro(null, "2020-01-01", "2020-01-31", null, null, null));
        assertEquals(0, vazio.delivered());
        assertEquals(0, BigDecimal.ZERO.compareTo(vazio.revenue()));
        assertNull(vazio.averageTicket());
    }

    @Test
    @DisplayName("F7 fatia 6: filtro inválido no histórico é 400")
    void deveRecusarFiltroInvalidoNoHistorico() {
        assertThrows(BusinessRuleException.class, () -> filtro("pendente", null, null, null, null, null));
        assertThrows(BusinessRuleException.class, () -> filtro(null, "ontem", null, null, null, null));
        assertThrows(BusinessRuleException.class, () -> filtro(null, "2026-10-31", "2026-10-01", null, null, null));
        assertThrows(BusinessRuleException.class, () -> filtro(null, null, null, "BOLETO", null, null));
        assertThrows(BusinessRuleException.class, () -> filtro(null, null, null, null, "DRONE", null));
    }

    // ---- HU17: frete por km somado e gravado no pedido ----
    // loja no centro de Curitiba e cliente no Batel: ~2,4 km em linha reta
    private void comFrete(String fixa, String porKm, String raio) {
        Supplier supplier = supplierRepository.findById(validSupplierId).orElseThrow();
        supplier.setDeliveryBaseFee(new BigDecimal(fixa));
        supplier.setDeliveryFeePerKm(new BigDecimal(porKm));
        supplier.setDeliveryRadiusKm(raio == null ? null : new BigDecimal(raio));
        supplier.getAddress().applyCoordinates(new com.h2gas.model.Coordinates(-25.4296, -49.2713));
        addressRepository.save(supplier.getAddress());
        supplierRepository.save(supplier);
        Address cliente = addressRepository.findById(validCustomerAddressId).orElseThrow();
        cliente.applyCoordinates(new com.h2gas.model.Coordinates(-25.4459, -49.2867));
        addressRepository.save(cliente);
    }

    private BigDecimal freteEsperado(String fixa, String porKm) {
        BigDecimal km = com.h2gas.model.DeliveryFeeCalculator.distanceKm(
                new com.h2gas.model.Coordinates(-25.4296, -49.2713), new com.h2gas.model.Coordinates(-25.4459, -49.2867));
        return new BigDecimal(fixa).add(new BigDecimal(porKm).multiply(km)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Test
    @DisplayName("HU17: o total do pedido em entrega soma o frete por km")
    void deveSomarOFreteAoTotalDoPedido() {
        comFrete("5.00", "2.00", null);

        OrderResponseDTO result = orderService.createOrder(novoPedido(validCustomerAddressId));

        BigDecimal frete = freteEsperado("5.00", "2.00");
        assertEquals(0, frete.compareTo(result.getDeliveryFee()));
        assertEquals(0, new BigDecimal("79.90").compareTo(result.getSubtotal()));
        assertEquals(0, new BigDecimal("79.90").add(frete).compareTo(result.getTotalPrice()));
    }

    @Test
    @DisplayName("HU17: o frete e a distância ficam gravados no pedido")
    void deveGravarOFreteEADistanciaNoPedido() {
        comFrete("5.00", "2.00", null);

        OrderResponseDTO result = orderService.createOrder(novoPedido(validCustomerAddressId));

        Order salvo = orderRepository.findById(result.getId()).orElseThrow();
        assertEquals(0, freteEsperado("5.00", "2.00").compareTo(salvo.getDeliveryFee()));
        assertNotNull(salvo.getDeliveryDistanceKm());
        assertEquals(2.4, salvo.getDeliveryDistanceKm().doubleValue(), 0.2);
    }

    @Test
    @DisplayName("HU17: retirada não cobra frete")
    void naoDeveCobrarFreteNaRetirada() {
        comFrete("5.00", "2.00", null);
        OrderRequestDTO request = novoPedido(null);
        request.setDeliveryMode("PICKUP");

        OrderResponseDTO result = orderService.createOrder(request);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getDeliveryFee()));
        assertEquals(0, new BigDecimal("79.90").compareTo(result.getTotalPrice()));
        assertNull(orderRepository.findById(result.getId()).orElseThrow().getDeliveryDistanceKm());
    }

    @Test
    @DisplayName("HU17: endereço fora do raio é recusado")
    void deveRecusarEnderecoForaDoRaio() {
        comFrete("5.00", "2.00", "1.0");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.createOrder(novoPedido(validCustomerAddressId)));
        assertEquals("Esta loja não entrega no endereço escolhido.", ex.getMessage());
    }

    @Test
    @DisplayName("HU17: mudar a tarifa da loja não altera pedido já feito")
    void naoDeveRecalcularOFreteDePedidoJaFeito() {
        comFrete("5.00", "2.00", null);
        OrderResponseDTO result = orderService.createOrder(novoPedido(validCustomerAddressId));

        comFrete("50.00", "20.00", null);

        Order salvo = orderRepository.findById(result.getId()).orElseThrow();
        assertEquals(0, freteEsperado("5.00", "2.00").compareTo(salvo.getDeliveryFee()));
    }

    @Test
    @DisplayName("HU17: sem coordenada, cobra só a taxa fixa (decisão do autor)")
    void deveCobrarSoATaxaFixaSemCoordenada() {
        Supplier supplier = supplierRepository.findById(validSupplierId).orElseThrow();
        supplier.setDeliveryBaseFee(new BigDecimal("6.00"));
        supplier.setDeliveryFeePerKm(new BigDecimal("2.00"));
        supplierRepository.save(supplier);

        OrderResponseDTO result = orderService.createOrder(novoPedido(validCustomerAddressId));

        assertEquals(0, new BigDecimal("6.00").compareTo(result.getDeliveryFee()));
        assertNull(result.getDeliveryDistanceKm());
    }

    @Test
    @DisplayName("HU17: loja que só faz retirada recusa pedido de entrega")
    void deveRecusarEntregaDeLojaQueSoFazRetirada() {
        Supplier supplier = supplierRepository.findById(validSupplierId).orElseThrow();
        supplier.setOffersDelivery(false);
        supplierRepository.save(supplier);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.createOrder(novoPedido(validCustomerAddressId)));
        assertEquals("Esta loja não entrega no endereço escolhido.", ex.getMessage());
    }
}
