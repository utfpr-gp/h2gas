package com.h2gas.services;

import com.h2gas.model.dto.CategoryProductDTO;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.ProductType;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.model.repository.CategoryProductRepository;
import com.h2gas.model.repository.ProductTypeRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

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
    private ProductTypeMapper productTypeMapper;

    @Autowired
    private ProductTypeService productTypeService;

    private Long validCategoryId;

    private Long typeIdAux;

    private Long validCategotyIdSecond;

    @BeforeEach
    public void setUp() {
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
        CategoryProductDTO novaCategoria = new CategoryProductDTO(
            null,
            "Teste categoria criada",
            null,
            typeIdAux
        );

        CategoryProductDTO categoriaCriada = categoryProductService.createCategory(novaCategoria);

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
        CategoryProductDTO categoryProductDTO = new CategoryProductDTO(
                null,
                "Teste update",
                null,
                typeIdAux
        );
        CategoryProductDTO result = categoryProductService.update(validCategotyIdSecond, categoryProductDTO);
        assertEquals("Teste update", result.getName());
    }
}