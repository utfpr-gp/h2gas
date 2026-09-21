package com.h2gas.services;

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