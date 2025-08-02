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

    @Autowired
    private UserRepository userRepository;

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

    private Long validIdSupplier;

    private Long validIdItemProduct;

    @Autowired
    private ProductItemRepository productItemRepository;

    @BeforeEach
    void setUp() {
        Supplier supplierFirst = createSupplierForTest(1);
        Supplier supplierSecond = createSupplierForTest(2);

        CategoryProduct categoryProduct = createCategoryForTest();

        ProductItem productItemFirst = new ProductItem(
                null,
                "Agua azul",
                new BigDecimal("150.00"),
                "urlImageTest//",
                5,
                null,
                null,
                categoryProduct,
                supplierFirst
        );

        productItemRepository.save(productItemFirst);
        validIdItemProduct = productItemFirst.getId();

        ProductItem productItemSecond = new ProductItem(
                null,
                "Agua vermelho",
                new BigDecimal("150.00"),
                "urlImageTest2//",
                5,
                null,
                null,
                categoryProduct,
                supplierSecond
        );

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



    CategoryProduct createCategoryForTest() {
        ProductType productTypeAgua = new ProductType(
                null,
                "Água Mineral",
                null
        );
        productTypeRepository.save(productTypeAgua);

        CategoryProduct categoryProduct = new CategoryProduct(
                null,
                "20L",
                productTypeAgua,
                null
        );

        categoryProductRepository.save(categoryProduct);

        return categoryProduct;
    }

    Supplier createSupplierForTest(Integer whatsReturnSupplier) {
        User user = new User(null,
                "Pablo Teste",
                "pablo@gmail.com",
                "1234545",
                "123435345",
                "234234234",
                "4534534",
                UserTypeEnum.SUPPLIER,
                null
        );

        userRepository.save(user);

        User user2 = new User(null,
                "Teste novo",
                "pablodddd@gmail.com",
                "12dd34545",
                "123435345dd",
                "234234234",
                "453453334",
                UserTypeEnum.CUSTOMER,
                null
        );

        userRepository.save(user2);

        City cityFirst = new City(
                null,
                "Curitiba",
                "PR"
        );
        cityRepository.save(cityFirst);

        Address address = new Address(
                null,
                "rua teste fornecedor",
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
                "1234123433333",
                "perto do lugar teste 33",
                "bairro do testado2",
                "924839847",
                cityFirst,
                null
        );

        addressRepository.save(address2);

        Supplier supplier = new Supplier(
                null,
                "Fornecedor Teste",
                "1234567890",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                Duration.ofMinutes(30),
                false,
                address,
                cityFirst,
                user,
                null,
                false
        );

        supplierRepository.save(supplier);

        validIdSupplier = supplier.getId();

        Supplier supplierSecond = new Supplier(
                null,
                "Fornecedor Teste 2",
                "1234567890433",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                Duration.ofMinutes(30),
                false,
                address2,
                cityFirst,
                user2,
                null,
                false
        );

        supplierRepository.save(supplierSecond);

        if (whatsReturnSupplier == 1) {
            return supplier;
        } else {
            return supplierSecond;
        }
    }
}