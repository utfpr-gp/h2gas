package com.h2gas.services;

import com.h2gas.model.dto.CreateProductItemDTO;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.ProductItemDTO;
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

        CategoryProduct categoryProductFirst = createCategoryForTest("20L", supplierFirst);
        CategoryProduct categoryProductSecond = createCategoryForTest("50L", supplierSecond);

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
        productItemService.deleteProductItem(validIdItemProduct);
        assertFalse(productItemRepository.findById(validIdItemProduct).isPresent());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar produto inexistente")
    void deleteProductNotFound() {
        Long invalidId = 999L;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            productItemService.deleteProductItem(invalidId);
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
        supplier.setOpen(true);
        supplier.setCity(city);
        supplierRepository.save(supplier);

        CategoryProduct categoryProduct = createCategoryForTest("HU02", supplier);

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
        supplier.setOpen(true);
        supplier.setCity(city);
        supplierRepository.save(supplier);

        CategoryProduct categoryProduct = createCategoryForTest("Água 20L", supplier);

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
    @DisplayName("F7: categoria de outro fornecedor no cadastro de produto é 403, não 500")
    void deveRetornar403AoUsarCategoriaDeOutroFornecedor() {
        Supplier logado = createSupplierForTest(1);
        logado.setName("Fornecedor 403 Teste");
        supplierRepository.save(logado);
        CategoryProduct deOutro = createCategoryForTest("13kg", createSupplierForTest(2));
        CreateProductItemDTO dto = new CreateProductItemDTO();
        dto.setBrand("Ultragaz");
        dto.setCategoryProductId(deOutro.getId());

        ForbiddenResourceException erro = assertThrows(ForbiddenResourceException.class,
                () -> productItemService.createProductItem(dto, null, "Fornecedor 403 Teste"));

        assertEquals("Categoria informada não pertence ao fornecedor logado.", erro.getMessage());
    }

    private CreateProductItemDTO dtoDeProduto(Long categoriaId, String entrega, String retirada) {
        CreateProductItemDTO dto = new CreateProductItemDTO();
        dto.setBrand("Ultragaz");
        dto.setCategoryProductId(categoriaId);
        dto.setPriceDelivery(new BigDecimal(entrega));
        dto.setPricePickup(new BigDecimal(retirada));
        dto.setStockQuantity(7);
        dto.setDescription("Botijão de teste");
        return dto;
    }

    @Test
    @DisplayName("HU10: cria produto com preço de entrega e de retirada diferentes")
    void deveCriarProdutoComPrecoDeEntregaEDeRetiradaDiferentes() {
        Supplier logado = createSupplierForTest(1);
        logado.setName("Fornecedor Preços Teste");
        supplierRepository.save(logado);
        CategoryProduct categoria = createCategoryForTest("13kg", logado);

        ProductItemDTO criado = productItemService.createProductItem(
                dtoDeProduto(categoria.getId(), "120.00", "100.00"), null, "Fornecedor Preços Teste");

        ProductItem salvo = productItemRepository.findById(criado.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("120.00").compareTo(salvo.getPriceDelivery()));
        assertEquals(0, new BigDecimal("100.00").compareTo(salvo.getPricePickup()));
        assertEquals(120.0, criado.getPriceDelivery());
        assertEquals(100.0, criado.getPricePickup());
        assertFalse(criado.isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: preços iguais ao criar sinalizam Entrega Grátis")
    void deveSinalizarEntregaGratisAoCriarComPrecosIguais() {
        Supplier logado = createSupplierForTest(1);
        logado.setName("Fornecedor Grátis Teste");
        supplierRepository.save(logado);
        CategoryProduct categoria = createCategoryForTest("13kg", logado);

        ProductItemDTO criado = productItemService.createProductItem(
                dtoDeProduto(categoria.getId(), "100.00", "100.00"), null, "Fornecedor Grátis Teste");

        assertTrue(criado.isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: editar grava os dois preços separados")
    void deveAtualizarAmbosPrecosAoEditarProduto() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        Supplier dono = produto.getSupplier();

        ProductItemDTO atualizado = productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(produto.getCategoryProduct().getId(), "130.00", "110.00"), dono);

        ProductItem salvo = productItemRepository.findById(validIdItemProduct).orElseThrow();
        assertEquals(0, new BigDecimal("130.00").compareTo(salvo.getPriceDelivery()));
        assertEquals(0, new BigDecimal("110.00").compareTo(salvo.getPricePickup()));
        assertEquals(130.0, atualizado.getPriceDelivery());
        assertEquals(110.0, atualizado.getPricePickup());
    }

    @Test
    @DisplayName("HU10: preços idênticos na edição passam a sinalizar Entrega Grátis")
    void deveSinalizarEntregaGratisAoEditarComPrecosIguais() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();

        ProductItemDTO atualizado = productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(produto.getCategoryProduct().getId(), "99.90", "99.90"), produto.getSupplier());

        assertTrue(atualizado.isFreeDelivery());
    }

    @Test
    @DisplayName("HU10: fornecedor não edita produto de outro fornecedor")
    void deveRejeitarEdicaoDeProdutoDeOutroFornecedor() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        Supplier intruso = createSupplierForTest(2);

        assertThrows(ForbiddenResourceException.class, () -> productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(produto.getCategoryProduct().getId(), "1.00", "1.00"), intruso));
        assertEquals(0, new BigDecimal("150.00").compareTo(
                productItemRepository.findById(validIdItemProduct).orElseThrow().getPriceDelivery()));
    }

    @Test
    @DisplayName("HU10: não move o produto para categoria de outro fornecedor")
    void deveRejeitarMoverProdutoParaCategoriaDeOutroFornecedor() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        CategoryProduct deOutro = createCategoryForTest("outra", createSupplierForTest(2));

        assertThrows(ForbiddenResourceException.class, () -> productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(deOutro.getId(), "150.00", "150.00"), produto.getSupplier()));
        assertEquals(produto.getCategoryProduct().getId(),
                productItemRepository.findById(validIdItemProduct).orElseThrow().getCategoryProduct().getId());
    }

    @Test
    @DisplayName("HU10: categoria legada sem fornecedor não é aceita como destino (403, não 500)")
    void deveRejeitarMoverProdutoParaCategoriaSemFornecedor() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        CategoryProduct semDono = createCategoryForTest("legada", null);

        assertThrows(ForbiddenResourceException.class, () -> productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(semDono.getId(), "150.00", "150.00"), produto.getSupplier()));
    }

    @Test
    @DisplayName("HU10: reenviar o categoryProductId devolvido pela API mantém a categoria")
    void deveManterCategoriaQuandoEdicaoReenviaOMesmoId() {
        ProductItem produto = productItemRepository.findById(validIdItemProduct).orElseThrow();
        Long categoriaAntes = produto.getCategoryProduct().getId();
        ProductItemDTO lido = productItemService.findById(validIdItemProduct);

        productItemService.updateProductItem(validIdItemProduct,
                dtoDeProduto(lido.getCategoryProductId(), "150.00", "140.00"), produto.getSupplier());

        assertEquals(categoriaAntes, productItemRepository.findById(validIdItemProduct).orElseThrow().getCategoryProduct().getId());
    }

    CategoryProduct createCategoryForTest(String name, Supplier supplier) {
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
                null,
                supplier
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
        supplierFirst.setOpen(false);

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
        supplierSecond.setOpen(false);

        supplierRepository.save(supplierSecond);

        if (whatsReturnSupplier == 1) {
            return supplierFirst;
        } else {
            return supplierSecond;
        }
    }
}