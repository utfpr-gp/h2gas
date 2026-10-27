package com.h2gas.services;

import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.ProductItemDTO;
import com.h2gas.model.dto.ProductStockSummaryDTO;
import com.h2gas.enums.OrderStatusEnum;
import com.h2gas.enums.StockFilterEnum;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Transactional
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class ProductItemServiceTest {

//    @Autowired
//    private SupplierService supplierService;

    @Autowired
    private AddressRepository addressRepository;

//    @Autowired
//    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

//    @Autowired
//    private UserRepository userRepository;

    @Autowired
    private CategoryProductService categoryProductService;

    @Autowired
    private CategoryProductRepository categoryProductRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductItemService productItemService;

    private String validIdSupplier;

    private Long validIdItemProduct;

    @Autowired
    private ProductItemRepository productItemRepository;

    @BeforeEach
    void setUp() {
        Supplier supplierFirst = createSupplierForTest(1);
        Supplier supplierSecond = createSupplierForTest(2);

        CategoryProduct categoryProductFirst = createCategoryForTest("20L");
        CategoryProduct categoryProductSecond = createCategoryForTest("50L");

        ProductItem productItemFirst = new ProductItem();
        productItemFirst.setBrand("Agua azul");
        productItemFirst.setPriceDelivery(new BigDecimal("150.00"));
        productItemFirst.setPricePickup(new BigDecimal("150.00"));
        productItemFirst.setDescription("teste descrição");
        productItemFirst.setImageUrl("urlImageTest//");
        productItemFirst.setStockQuantity(5);
        productItemFirst.setCategoryProduct(categoryProductFirst);
        productItemFirst.setSupplier(supplierFirst);
        productItemFirst.setVersion(null);

        productItemRepository.save(productItemFirst);
        validIdItemProduct = productItemFirst.getId();

        ProductItem productItemSecond = new ProductItem();
        productItemSecond.setBrand("Agua vermelho");
        productItemSecond.setPriceDelivery(new BigDecimal("150.00"));
        productItemSecond.setPricePickup(new BigDecimal("150.00"));
        productItemSecond.setDescription("teste descricao 2");
        productItemSecond.setImageUrl("urlImageTest2//");
        productItemSecond.setStockQuantity(5);
        productItemSecond.setCategoryProduct(categoryProductSecond);
        productItemSecond.setSupplier(supplierSecond);
        productItemSecond.setVersion(null);

        productItemRepository.save(productItemSecond);
    }

    @Test
    @DisplayName("Procura todos os items")
    void findAllProductItems() {
        List<ProductItemDTO> result = productItemService.findAllProductItems();
        assertEquals(2, result.size());
        assertNotNull(result);
        assertEquals("Agua azul", result.get(0).getBrand());
        assertEquals("Agua vermelho", result.get(1).getBrand());
    }

    @Test
    @DisplayName("Procura os items passando ID")
    void findById() {
        ProductItemDTO result = productItemService.findById(validIdItemProduct);
        assertNotNull(result);
        assertEquals("Agua azul", result.getBrand());
    }

    @Test
    @DisplayName("Retorna erro ao passar o id incorreto")
    void findByIdItemError() {
        Long idInvalid = 44L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productItemService.findById(idInvalid);
        });
        assertEquals("Produto não encontrado com esse id: " + idInvalid, exception.getMessage());
    }

//    @Test
//    void createProductItem() {
//    }

    @Test
    @DisplayName("Deve deletar o item")
    void deleteProductItem() {
        assertTrue(productItemRepository.findById(validIdItemProduct).isPresent());
        productItemService.deleteProductItem(validIdItemProduct, productItemRepository.findById(validIdItemProduct).orElseThrow().getSupplier());
        assertFalse(productItemRepository.findById(validIdItemProduct).isPresent());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar produto inexistente")
    void deleteProductNotFound() {
        Long invalidId = 999L;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            productItemService.deleteProductItem(invalidId, supplierRepository.findById(validIdSupplier).orElseThrow());
        });

        assertEquals("Produto com " + invalidId + " não encontrado", exception.getMessage());
    }

//    @Test
//    void updateProductItem() {
//    }

    @Test
    @DisplayName("HU02: deve ordenar por preço de entrega crescente")
    void deveOrdenarPorPrecoDeEntregaCrescente() {
        City city = new City(null, "Pato Branco", "PR");
        cityRepository.save(city);

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor HU02");
        supplier.setEmail("fornecedor.hu02@h2gas.test");
        supplier.setPassword("senha");
        supplier.setUserType(UserTypeEnum.SUPPLIER);
        supplier.setCnpj("12345678000199");
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplier.setActive(true);
        // HU18: aberta pelo horário (o relógio dos testes marca 10:00)
        supplier.setOpeningTime(java.time.LocalTime.of(8, 0));
        supplier.setClosingTime(java.time.LocalTime.of(18, 0));
        supplier.setCity(city);
        supplierRepository.save(supplier);

        CategoryProduct categoryProduct = createCategoryForTest("HU02");

        ProductItem caro = new ProductItem();
        caro.setBrand("Produto Caro");
        caro.setPriceDelivery(new BigDecimal("200.00"));
        caro.setPricePickup(new BigDecimal("200.00"));
        caro.setStockQuantity(5);
        caro.setCategoryProduct(categoryProduct);
        caro.setSupplier(supplier);
        productItemRepository.save(caro);

        ProductItem barato = new ProductItem();
        barato.setBrand("Produto Barato");
        barato.setPriceDelivery(new BigDecimal("50.00"));
        barato.setPricePickup(new BigDecimal("50.00"));
        barato.setStockQuantity(5);
        barato.setCategoryProduct(categoryProduct);
        barato.setSupplier(supplier);
        productItemRepository.save(barato);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "priceDelivery"));
        Page<ProductItemDTO> result = productItemService.findBySupplierActiveAndSupplierOpen(
                city.getId(), null, pageable);

        List<ProductItemDTO> content = result.getContent();
        assertTrue(content.size() >= 2);
        assertEquals("Produto Barato", content.get(0).getBrand());
    }

    @Test
    @DisplayName("Deve combinar filtro de categoria e busca por marca na listagem de um fornecedor")
    void deveCombinarFiltroDeCategoriaEBuscaSimultaneamente() {
        City city = new City(null, "Pato Branco", "PR");
        cityRepository.save(city);

        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor Filtro Combinado");
        supplier.setEmail("filtro.combinado@h2gas.test");
        supplier.setPassword("senha");
        supplier.setUserType(UserTypeEnum.SUPPLIER);
        supplier.setCnpj("12345678000199");
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplier.setActive(true);
        // HU18: aberta pelo horário (o relógio dos testes marca 10:00)
        supplier.setOpeningTime(java.time.LocalTime.of(8, 0));
        supplier.setClosingTime(java.time.LocalTime.of(18, 0));
        supplier.setCity(city);
        supplierRepository.save(supplier);

        CategoryProduct categoryProduct = createCategoryForTest("Água 20L");

        ProductItem copap = new ProductItem();
        copap.setBrand("CopapFixture");
        copap.setPriceDelivery(new BigDecimal("15.00"));
        copap.setPricePickup(new BigDecimal("15.00"));
        copap.setStockQuantity(5);
        copap.setCategoryProduct(categoryProduct);
        copap.setSupplier(supplier);
        productItemRepository.save(copap);

        ProductItem minalba = new ProductItem();
        minalba.setBrand("MinalbaFixture");
        minalba.setPriceDelivery(new BigDecimal("25.00"));
        minalba.setPricePickup(new BigDecimal("25.00"));
        minalba.setStockQuantity(5);
        minalba.setCategoryProduct(categoryProduct);
        minalba.setSupplier(supplier);
        productItemRepository.save(minalba);

        String categoria = categoryProduct.getProductType().getName();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductItemDTO> result = productItemService.findProductsFiltered(
                supplier.getId(), "copap", categoria, false, pageable);

        List<ProductItemDTO> content = result.getContent();
        assertEquals(1, content.size());
        assertEquals("CopapFixture", content.get(0).getBrand());
    }



    @Test
    @DisplayName("HU19: o fornecedor cadastra produto com qualquer categoria (são globais)")
    void deveCadastrarProdutoComQualquerCategoria() {
        Supplier logado = createSupplierForTest(1);
        logado.setName("Fornecedor Categoria Global");
        supplierRepository.save(logado);
        CategoryProduct global = createCategoryForTest("13kg");
        CreateProductItemDTO dto = new CreateProductItemDTO();
        dto.setBrand("Ultragaz");
        dto.setCategoryProductId(global.getId());
        dto.setPrice(new BigDecimal("100.00"));
        dto.setShippingFee(new BigDecimal("10.00"));

        ProductItemDTO criado = productItemService.createProductItem(dto, null, "Fornecedor Categoria Global");

        assertEquals(global.getId(), criado.getCategoryProductId());
    }

    @Test
    @DisplayName("HU19: categoria inexistente no cadastro de produto é 404")
    void deveRetornar404ParaCategoriaInexistenteNoCadastro() {
        Supplier logado = createSupplierForTest(1);
        logado.setName("Fornecedor 404 Categoria");
        supplierRepository.save(logado);
        CreateProductItemDTO dto = new CreateProductItemDTO();
        dto.setBrand("Ultragaz");
        dto.setCategoryProductId(987_654L);
        dto.setPrice(new BigDecimal("100.00"));
        dto.setShippingFee(new BigDecimal("10.00"));

        assertThrows(com.h2gas.exceptions.ResourceNotFoundException.class,
                () -> productItemService.createProductItem(dto, null, "Fornecedor 404 Categoria"));
    }

    private CreateProductItemDTO dtoDeProduto(Long categoriaId, String preco, String frete) {
        CreateProductItemDTO dto = new CreateProductItemDTO();
        dto.setBrand("Ultragaz");
        dto.setCategoryProductId(categoriaId);
        dto.setPrice(new BigDecimal(preco));
        dto.setShippingFee(new BigDecimal(frete));
        dto.setStockQuantity(7);
        dto.setDescription("Botijão de teste");
        return dto;
    }

    @Test
    @DisplayName("HU10: cria produto derivando retirada = preço e entrega = preço + frete")
    void deveDerivarPrecoDeEntregaSomandoOFrete() {
        Supplier logado = createSupplierForTest(1);
        logado.setName("Fornecedor Preços Teste");
        supplierRepository.save(logado);
        CategoryProduct categoria = createCategoryForTest("13kg");

        ProductItemDTO criado = productItemService.createProductItem(
                dtoDeProduto(categoria.getId(), "100.00", "20.00"), null, "Fornecedor Preços Teste");

        ProductItem salvo = productItemRepository.findById(criado.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("120.00").compareTo(salvo.getPriceDelivery()));
        assertEquals(0, new BigDecimal("100.00").compareTo(salvo.getPricePickup()));
        assertEquals(120.0, criado.getPriceDelivery());
        assertEquals(100.0, criado.getPricePickup());
        assertFalse(criado.isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: frete zero ao criar sinaliza Entrega Grátis")
    void deveSinalizarEntregaGratisQuandoOFreteForZero() {
        Supplier logado = createSupplierForTest(1);
        logado.setName("Fornecedor Grátis Teste");
        supplierRepository.save(logado);
        CategoryProduct categoria = createCategoryForTest("13kg");

        ProductItemDTO criado = productItemService.createProductItem(
                dtoDeProduto(categoria.getId(), "100.00", "0.00"), null, "Fornecedor Grátis Teste");

        assertTrue(criado.isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: editar grava preço e frete, derivando as duas colunas")
    void deveAtualizarPrecoEFreteAoEditarProduto() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        Supplier dono = produto.getSupplier();

        ProductItemDTO atualizado = productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(produto.getCategoryProduct().getId(), "110.00", "20.00"), dono);

        ProductItem salvo = productItemRepository.findById(validIdItemProduct).orElseThrow();
        assertEquals(0, new BigDecimal("130.00").compareTo(salvo.getPriceDelivery()));
        assertEquals(0, new BigDecimal("110.00").compareTo(salvo.getPricePickup()));
        assertEquals(130.0, atualizado.getPriceDelivery());
        assertEquals(110.0, atualizado.getPricePickup());
    }

    @Test
    @DisplayName("HU10: frete zero na edição passa a sinalizar Entrega Grátis")
    void deveSinalizarEntregaGratisAoEditarComFreteZero() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();

        ProductItemDTO atualizado = productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(produto.getCategoryProduct().getId(), "99.90", "0.00"), produto.getSupplier());

        assertTrue(atualizado.isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: fornecedor não edita produto de outro fornecedor")
    void deveRejeitarEdicaoDeProdutoDeOutroFornecedor() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        Supplier intruso = createSupplierForTest(2);

        assertThrows(ForbiddenResourceException.class, () -> productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(produto.getCategoryProduct().getId(), "1.00", "0.00"), intruso));
        assertEquals(0, new BigDecimal("150.00").compareTo(
                productItemRepository.findById(validIdItemProduct).orElseThrow().getPriceDelivery()));
    }

    @Test
    @DisplayName("HU19: o dono move o produto para qualquer categoria")
    void deveMoverProdutoParaQualquerCategoria() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        CategoryProduct outra = createCategoryForTest("outra");

        productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(outra.getId(), "150.00", "0.00"), produto.getSupplier());

        assertEquals(outra.getId(),
                productItemRepository.findById(validIdItemProduct).orElseThrow().getCategoryProduct().getId());
    }

    @Test
    @DisplayName("HU10: reenviar o categoryProductId devolvido pela API mantém a categoria")
    void deveManterCategoriaQuandoEdicaoReenviaOMesmoId() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        Long categoriaAntes = produto.getCategoryProduct().getId();
        ProductItemDTO lido = productItemService.findById(validIdItemProduct);

        productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(lido.getCategoryProductId(), "140.00", "10.00"), produto.getSupplier());

        assertEquals(categoriaAntes, productItemRepository.findById(validIdItemProduct).orElseThrow().getCategoryProduct().getId());
    }

    CategoryProduct createCategoryForTest(String name) {
        ProductType productTypeAgua = new ProductType(
                null,
                "Água Mineral",
                null
        );
        productTypeRepository.save(productTypeAgua);

        CategoryProduct categoryProduct = new CategoryProduct(
                null,
                name,
                productTypeAgua,
                null
        );

        categoryProductRepository.save(categoryProduct);

        return categoryProduct;
    }

    Supplier createSupplierForTest(Integer whatsReturnSupplier) {
        City cityFirst = new City(
                null,
                "Curitiba",
                "PR"
        );
        cityRepository.save(cityFirst);

        Address address = new Address(
                null,
                "rua teste fornecedor",
                "testeee",
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
                "casa teste",
                "1234123433333",
                "perto do lugar teste 33",
                "bairro do testado2",
                "924839847",
                cityFirst,
                null
        );

        addressRepository.save(address2);

        Supplier supplierFirst = new Supplier();

        // Campos da superclasse User
        supplierFirst.setId(null); // geralmente deixa null para autogeração
        supplierFirst.setName("Fornecedor Teste");
        supplierFirst.setEmail("fornecedor@gmail");
        supplierFirst.setPhoneNumber("1234567890");
        supplierFirst.setCpf("12343445");
        supplierFirst.setPassword("senhateste");
        supplierFirst.setUserType(UserTypeEnum.SUPPLIER);

        // Campos específicos do Supplier
        supplierFirst.setCnpj("12345678901234");
        supplierFirst.setDescription("Testando descricao");
        supplierFirst.setOpeningTime(LocalTime.of(8, 0));
        supplierFirst.setClosingTime(LocalTime.of(18, 0));
        supplierFirst.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplierFirst.setActive(true);
        supplierFirst.setAddress(address);
        supplierFirst.setCity(cityFirst);
        supplierFirst.setProductItems(null); // ou new ArrayList<>()
        // HU18: fechada pelo horário (o relógio dos testes marca 10:00)
        supplierFirst.setOpeningTime(java.time.LocalTime.of(19, 0));
        supplierFirst.setClosingTime(java.time.LocalTime.of(23, 0));

        supplierRepository.save(supplierFirst);

        validIdSupplier = supplierFirst.getId();

        Supplier supplierSecond = new Supplier();
        // Campos da superclasse User (preencha conforme necessário ou deixe null)
        supplierSecond.setId(null);
        supplierSecond.setName("Fornecedor Teste 2");
        supplierSecond.setEmail("1234567890433");
        supplierSecond.setPhoneNumber("teste teste"); // parecia que você misturou dados, verifique se é telefone
        supplierSecond.setCpf(null); // se não tiver CPF
        supplierSecond.setPassword(null); // se não tiver senha
        supplierSecond.setUserType(UserTypeEnum.SUPPLIER); // assumindo que é fornecedor

        // Campos específicos do Supplier
        supplierSecond.setOpeningTime(LocalTime.of(8, 0));
        supplierSecond.setClosingTime(LocalTime.of(18, 0));
        supplierSecond.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplierSecond.setActive(false);
        supplierSecond.setAddress(address2);
        supplierSecond.setCity(cityFirst);
        supplierSecond.setProductItems(null); // ou new ArrayList<>()
        // HU18: fechada pelo horário (o relógio dos testes marca 10:00)
        supplierSecond.setOpeningTime(java.time.LocalTime.of(19, 0));
        supplierSecond.setClosingTime(java.time.LocalTime.of(23, 0));

        supplierRepository.save(supplierSecond);

        if (whatsReturnSupplier == 1) {
            return supplierFirst;
        } else {
            return supplierSecond;
        }
    }

    @Autowired
    private OrderRepository orderRepository;

    private ProductItem salvarProduto(Supplier supplier, CategoryProduct categoria, String marca, int estoque) {
        ProductItem p = new ProductItem();
        p.setBrand(marca);
        p.setPriceDelivery(new BigDecimal("10.00"));
        p.setPricePickup(new BigDecimal("10.00"));
        p.setImageUrl("x");
        p.setStockQuantity(estoque);
        p.setCategoryProduct(categoria);
        p.setSupplier(supplier);
        return productItemRepository.save(p);
    }

    private Supplier donoDoProdutoValido() {
        return productItemRepository.findById(validIdItemProduct).orElseThrow().getSupplier();
    }

    @Test
    @DisplayName("F7 fatia 6: lista só os produtos do fornecedor, filtrando por estoque")
    void deveListarProdutosDoFornecedorPorEstoque() {
        Supplier dono = donoDoProdutoValido();
        CategoryProduct categoria = productItemRepository.findById(validIdItemProduct).orElseThrow().getCategoryProduct();
        salvarProduto(dono, categoria, "Bento Esgotado", 0);
        salvarProduto(dono, categoria, "Cheio Total", 50);

        List<String> todos = productItemService.findMine(dono.getId(), null, StockFilterEnum.ALL, PageRequest.of(0, 10))
                .map(ProductItemDTO::getBrand).getContent();
        assertEquals(List.of("Agua azul", "Bento Esgotado", "Cheio Total"), todos);
        assertEquals(List.of("Agua azul", "Cheio Total"), marcas(dono, StockFilterEnum.AVAILABLE));
        assertEquals(List.of("Agua azul"), marcas(dono, StockFilterEnum.LOW));
        assertEquals(List.of("Bento Esgotado"), marcas(dono, StockFilterEnum.OUT));
    }

    private List<String> marcas(Supplier dono, StockFilterEnum filtro) {
        return productItemService.findMine(dono.getId(), null, filtro, PageRequest.of(0, 10)).map(ProductItemDTO::getBrand).getContent();
    }

    @Test
    @DisplayName("F7 fatia 6: busca por marca, categoria ou tipo")
    void deveBuscarPorMarcaCategoriaOuTipo() {
        String dono = donoDoProdutoValido().getId();

        assertEquals(1, productItemService.findMine(dono, "AZUL", StockFilterEnum.ALL, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, productItemService.findMine(dono, "20l", StockFilterEnum.ALL, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, productItemService.findMine(dono, "água mineral", StockFilterEnum.ALL, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, productItemService.findMine(dono, "vermelho", StockFilterEnum.ALL, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    @DisplayName("F7 fatia 6: resumo do estoque usa o estoque mínimo da configuração")
    void deveResumirEstoqueComOLimiteDaConfiguracao() {
        Supplier dono = donoDoProdutoValido();
        CategoryProduct categoria = productItemRepository.findById(validIdItemProduct).orElseThrow().getCategoryProduct();
        salvarProduto(dono, categoria, "Zerado", 0);
        salvarProduto(dono, categoria, "Dez", 10);

        ProductStockSummaryDTO padrao = productItemService.summarizeMine(dono.getId());
        assertEquals(new ProductStockSummaryDTO(3, 2, 1, 1, 8), padrao);

        dono.setLowStockThreshold(10);
        supplierRepository.save(dono);
        assertEquals(new ProductStockSummaryDTO(3, 2, 2, 1, 10), productItemService.summarizeMine(dono.getId()));
    }

    @Test
    @DisplayName("F7 fatia 6: fornecedor não exclui produto de outro")
    void deveRecusarExcluirProdutoDeOutroFornecedor() {
        Supplier intruso = createSupplierForTest(2);

        assertThrows(ForbiddenResourceException.class, () -> productItemService.deleteProductItem(validIdItemProduct, intruso));
        assertTrue(productItemRepository.findById(validIdItemProduct).isPresent());
    }

    @Test
    @DisplayName("F7 fatia 6: produto já vendido não é excluído (409)")
    void deveRecusarExcluirProdutoJaVendido() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        Order pedido = new Order();
        pedido.setCustomer(produto.getSupplier());
        pedido.setSupplier(produto.getSupplier());
        pedido.setStatus(OrderStatusEnum.DELIVERED);
        pedido.setTotalPrice(new BigDecimal("10.00"));
        pedido.setItems(new java.util.ArrayList<>(List.of(new OrderItem(null, 1, new BigDecimal("10.00"), new BigDecimal("10.00"), produto, pedido))));
        orderRepository.save(pedido);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> productItemService.deleteProductItem(validIdItemProduct, produto.getSupplier()));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals("Este produto já está em pedidos e não pode ser excluído. Zere o estoque para tirá-lo do catálogo.", erro.getReason());
    }

    @Test
    @DisplayName("F7 §5.21: busca de produtos disponíveis por marca, categoria ou tipo, só na cidade (RN05)")
    void deveBuscarProdutosDisponiveisDaCidade() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        City londrina = cityRepository.save(new City(null, "Londrina", "PR"));
        Supplier lojaCuritiba = lojaAberta("cwb", curitiba);
        Supplier lojaLondrina = lojaAberta("ldn", londrina);
        CategoryProduct vinteCwb = createCategoryForTest("20L");
        CategoryProduct vinteLdn = createCategoryForTest("20L");
        salvarProduto(lojaCuritiba, vinteCwb, "Minalba", 10);
        salvarProduto(lojaCuritiba, vinteCwb, "Ouro Fino", 10);
        salvarProduto(lojaCuritiba, vinteCwb, "Minalba Zerada", 0);
        salvarProduto(lojaLondrina, vinteLdn, "Minalba", 10);

        java.util.function.Function<String, List<String>> busca = q -> productItemService
                .findBySupplierActiveAndSupplierOpen(curitiba.getId(), null, q, PageRequest.of(0, 10))
                .map(ProductItemDTO::getBrand).getContent();

        assertEquals(List.of("Minalba"), busca.apply("MINALBA"));
        assertEquals(2, busca.apply("20l").size());
        assertEquals(2, busca.apply("água mineral").size());
        assertEquals(2, busca.apply(null).size());
    }

    private Supplier lojaAberta(String sufixo, City city) {
        Supplier s = new Supplier();
        s.setName("Loja " + sufixo);
        s.setEmail("loja." + sufixo + "@h2gas.test");
        s.setPassword("senha");
        s.setUserType(UserTypeEnum.SUPPLIER);
        s.setCnpj("12345678000199");
        s.setOpeningTime(LocalTime.of(8, 0));
        s.setClosingTime(LocalTime.of(18, 0));
        s.setAverageDeliveryTime(Duration.ofMinutes(30));
        s.setActive(true);
        // HU18: aberta pelo horário (o relógio dos testes marca 10:00)
        s.setOpeningTime(java.time.LocalTime.of(8, 0));
        s.setClosingTime(java.time.LocalTime.of(18, 0));
        s.setCity(city);
        return supplierRepository.save(s);
    }
}