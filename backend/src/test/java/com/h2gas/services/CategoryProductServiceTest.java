package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ForbiddenResourceException;
import com.h2gas.model.dto.CategoryRequestDTO;
import com.h2gas.model.dto.SupplierCategoryDTO;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.repository.ProductItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.*;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.model.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@Transactional
@TestPropertySource(properties = "jwt.secret=segredoFake123")
class CategoryProductServiceTest {

    @Autowired
    private CategoryProductService categoryProductService;

    @Autowired
    private CategoryProductRepository categoryProductRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    private Long validCategoryId;

    private Long typeIdAux;

    private Long validCategotyIdSecond;

    private String supplierFirstId;

    private String supplierSecondId;

    private ProductType tipoAgua;

    @Autowired
    private ProductItemRepository productItemRepository;

    @BeforeEach
    public void setUp() {

        Supplier supplierFirst = createSupplierForTest(1);
        Supplier supplierSecond = createSupplierForTest(2);

        ProductType productTypeAgua = new ProductType(
                null,
                "Água Mineral",
                null
        );
        productTypeRepository.save(productTypeAgua);


        ProductType productTypeGas = new ProductType(
                null,
                "Gás",
                null
        );

        productTypeRepository.save(productTypeGas);


        CategoryProduct categoryProductFirst = new CategoryProduct(
                null,
                "20L",
                productTypeAgua,
                null
        );

        categoryProductRepository.save(categoryProductFirst);

        CategoryProduct categoryProductSecond = new CategoryProduct(
                null,
                "P13",
                productTypeGas,
                null
        );

        categoryProductRepository.save(categoryProductSecond);

        validCategoryId = categoryProductFirst.getId();

        typeIdAux = productTypeGas.getId();

        validCategotyIdSecond = categoryProductSecond.getId();

        supplierFirstId = supplierFirst.getId();
        supplierSecondId = supplierSecond.getId();
        tipoAgua = productTypeAgua;
    }

    @Test
    @DisplayName("Procura todas as categorias")
    void findAllCategoryProducts() {
        List<CategoryProductDTO> result = categoryProductService.findAllCategoryProducts();
        assertEquals(2, result.size());
        assertNotNull(result);
        assertEquals("20L", result.get(0).getName());
        assertEquals("P13", result.get(1).getName());
    }

    @Test
    @DisplayName("Procura categoria pelo ID")
    void findByIdCategory() {
        CategoryProductDTO result = categoryProductService.findByIdCategory(validCategoryId);
        assertNotNull(result);
        assertEquals("20L", result.getName());
    }

    @Test
    @DisplayName("Retorna erro quando passar id errado para o método")
    void findByIdError() {
        Long idInvalid = 47L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryProductService.findEntityById(idInvalid);
        });
        assertEquals("Categoria não encontrada com ID: " + idInvalid, exception.getMessage());
    }

    @Test
    @DisplayName("Cria uma nova categoria")
    void createCategory() {
        CategoryProductDTO categoriaCriada = categoryProductService.createCategory(
                new CategoryRequestDTO("Teste categoria criada", typeIdAux));

        assertNotNull(categoriaCriada);
        assertNotNull(categoriaCriada.getId());
        assertEquals("Teste categoria criada", categoriaCriada.getName());

        CategoryProduct entidade = categoryProductRepository.findById(categoriaCriada.getId()).orElse(null);
        assertNotNull(entidade);
        assertEquals("Teste categoria criada", entidade.getName());
    }

    @Test
    @DisplayName("Deleta uma categoria com sucesso")
    void deleteCategory() {
        assertTrue(categoryProductRepository.findById(validCategoryId).isPresent());
        categoryProductService.deleteCategory(validCategoryId);
        assertFalse(categoryProductRepository.findById(validCategoryId).isPresent());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar categoria inexistente")
    void deleteCategoryNotFound() {
        Long invalidId = 999L;

        Exception exception = assertThrows(RuntimeException.class, () -> {
            categoryProductService.deleteCategory(invalidId);
        });

        assertEquals("Categoria não encontrada com ID: " + invalidId, exception.getMessage());
    }

    @Test
    @DisplayName("Deve atualizar uma categoria")
    void update() {
        CategoryProductDTO result = categoryProductService.update(validCategotyIdSecond,
                new CategoryRequestDTO("Teste update", typeIdAux));
        assertEquals("Teste update", result.getName());
    }

    private Supplier createSupplierForTest(Integer whatsReturnSupplier) {
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

    private void salvarProduto(Long categoriaId, String supplierId) {
        ProductItem p = new ProductItem();
        p.setBrand("Minalba");
        p.setPriceDelivery(new java.math.BigDecimal("10.00"));
        p.setPricePickup(new java.math.BigDecimal("10.00"));
        p.setImageUrl("x");
        p.setStockQuantity(5);
        p.setCategoryProduct(categoryProductRepository.findById(categoriaId).orElseThrow());
        p.setSupplier(supplierRepository.findById(supplierId).orElseThrow());
        productItemRepository.save(p);
    }

    // ---- HU19: categorias globais, cadastradas só pelo admin ----

    @Test
    @DisplayName("HU19: a loja vê só as categorias que usa, com a contagem dos produtos dela")
    void deveListarAsCategoriasQueALojaUsa() {
        salvarProduto(validCategoryId, supplierFirstId);
        salvarProduto(validCategoryId, supplierFirstId);
        salvarProduto(validCategotyIdSecond, supplierSecondId);
        categoryProductService.createCategory(new CategoryRequestDTO("10L", tipoAgua.getId()));

        List<SupplierCategoryDTO> minhas = categoryProductService.findMine(supplierFirstId);

        assertEquals(List.of("20L"), minhas.stream().map(SupplierCategoryDTO::name).toList());
        SupplierCategoryDTO vinte = minhas.get(0);
        assertEquals("Água Mineral", vinte.type());
        assertEquals(tipoAgua.getId(), vinte.productTypeId());
        assertEquals("category", vinte.typeIcon());
        assertEquals(2, vinte.productCount());
    }

    @Test
    @DisplayName("HU19: a categoria é global — nasce sem loja, com o nome sem espaços nas pontas")
    void deveCriarCategoriaGlobal() {
        CategoryProductDTO criada = categoryProductService.createCategory(new CategoryRequestDTO("  P45 ", typeIdAux));

        CategoryProduct entidade = categoryProductRepository.findById(criada.getId()).orElseThrow();
        assertEquals("P45", entidade.getName());
        assertEquals(typeIdAux, entidade.getProductType().getId());
    }

    @Test
    @DisplayName("HU19: nome repetido no mesmo tipo é recusado; em outro tipo é permitido")
    void deveRecusarNomeRepetidoNoMesmoTipo() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> categoryProductService.createCategory(new CategoryRequestDTO("20l", tipoAgua.getId())));
        assertEquals("Categoria já cadastrada.", erro.getMessage());

        assertNotNull(categoryProductService.createCategory(new CategoryRequestDTO("20L", typeIdAux)));
        // editar mantendo o próprio nome não conflita
        assertNotNull(categoryProductService.update(validCategoryId, new CategoryRequestDTO("20L", tipoAgua.getId())));
    }

    @Test
    @DisplayName("HU19: renomear para um nome que já existe no tipo é recusado")
    void deveRecusarRenomearParaNomeQueJaExisteNoTipo() {
        categoryProductService.createCategory(new CategoryRequestDTO("10L", tipoAgua.getId()));

        assertThrows(BusinessRuleException.class,
                () -> categoryProductService.update(validCategoryId, new CategoryRequestDTO("10l", tipoAgua.getId())));
    }

    @Test
    @DisplayName("HU19: renomear muda o nome que os produtos mostram")
    void deveRenomearCategoria() {
        salvarProduto(validCategoryId, supplierFirstId);

        categoryProductService.update(validCategoryId, new CategoryRequestDTO("Galão 20L", tipoAgua.getId()));

        assertEquals("Galão 20L", productItemRepository.findAll().get(0).getCategoryProduct().getName());
    }

    @Test
    @DisplayName("HU19: categoria com produtos não é excluída (409)")
    void deveRecusarExcluirCategoriaComProdutos() {
        salvarProduto(validCategoryId, supplierFirstId);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> categoryProductService.deleteCategory(validCategoryId));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals("20L tem 1 produto. Mova ou exclua os produtos antes de excluir a categoria.", erro.getReason());
        assertTrue(categoryProductRepository.findById(validCategoryId).isPresent());
    }

    @Test
    @DisplayName("HU19: a listagem traz todas as categorias, ordenadas por tipo e nome")
    void deveListarTodasAsCategoriasOrdenadas() {
        categoryProductService.createCategory(new CategoryRequestDTO("10L", tipoAgua.getId()));
        categoryProductService.createCategory(new CategoryRequestDTO("P5", typeIdAux));

        assertEquals(List.of("10L", "20L", "P13", "P5"),
                categoryProductService.findAllCategoryProducts().stream().map(CategoryProductDTO::getName).toList());
    }
}
