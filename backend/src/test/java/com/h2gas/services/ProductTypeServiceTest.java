package com.h2gas.services;

import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.exceptions.ResourceNotFoundException;
import com.h2gas.model.dto.CityDTO;
import com.h2gas.model.dto.ProductTypeDTO;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.ProductType;
import com.h2gas.model.mapper.ProductTypeMapper;
import com.h2gas.model.repository.ProductTypeRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
class ProductTypeServiceTest {
    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private ProductTypeMapper productTypeMapper;

    @Autowired
    private ProductTypeService productTypeService;

    private Long validTypeId;


    @BeforeAll
    public void setUp() {
        ProductType productTypeFirst = new ProductType(
            null,
            "Água Mineral",
        null
        );
        productTypeRepository.save(productTypeFirst);


        ProductType productTypeSecond = new ProductType(
                null,
                "Gás",
                null
        );

        productTypeRepository.save(productTypeSecond);

        validTypeId = productTypeFirst.getId();
    }

    @Test
    @DisplayName("Procura todos os tipos de produtos")
    void findAllProductTypes() {
        List<ProductTypeDTO> result = productTypeService.findAllProductTypes();
        assertEquals(2, result.size());
        assertNotNull(result);
        assertEquals("Água Mineral", result.get(0).getName());
        assertEquals("Gás", result.get(1).getName());
    }

    @Test
    @DisplayName("Retorna erro ao passar o id incorreto")
    void findByIdTypesProductsError() {
        Long idInvalid = 4L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productTypeService.findByIdTypesProducts(idInvalid);
        });
        assertEquals("Tipo não encontrada com esse id: " + idInvalid, exception.getMessage());
    }

    @Test
    @DisplayName("Procura tipos de produto utilizando o id")
    void findByIdTypesProductsSuccess() {
        ProductTypeDTO result = productTypeService.findByIdTypesProducts(validTypeId);
        assertNotNull(result);
        assertEquals("Água Mineral", result.getName());
    }

    @Test
    @DisplayName("Deve retornar a entidade com base no id")
    void findEntityById() {
        ProductType result = productTypeService.findEntityById(validTypeId);
        assertNotNull(result);
        assertEquals("Água Mineral", result.getName());
    }

    @Test
    void createProductType() {
        ProductTypeDTO novoTipo = new ProductTypeDTO(
                null,
                "Gás teste"
        );

        ProductTypeDTO tipoCriado = productTypeService.createProductType(novoTipo);

        assertNotNull(tipoCriado);
        assertNotNull(tipoCriado.getId());
        assertEquals("Gás teste", tipoCriado.getName());

        ProductType entidade = productTypeRepository.findById(tipoCriado.getId()).orElse(null);
        assertNotNull(entidade);
        assertEquals("Gás teste", entidade.getName());
    }

    @Test
    @DisplayName("HU12: admin cria tipo, gravado sem espaços nas pontas")
    void deveCriarTipoSemEspacosNasPontas() {
        ProductTypeDTO criado = productTypeService.createProductType(new ProductTypeDTO(null, "  Carvão  "));

        assertNotNull(criado.getId());
        assertEquals("Carvão", productTypeRepository.findById(criado.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("HU12: tipo repetido (sem diferenciar maiúsculas) é recusado")
    void deveRecusarTipoRepetidoSemDiferenciarMaiusculas() {
        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> productTypeService.createProductType(new ProductTypeDTO(null, " gÁS ")));
        assertEquals("Tipo já cadastrado.", erro.getMessage());
    }

    @Test
    @DisplayName("HU12: admin renomeia tipo; o próprio nome não conta como repetido")
    void deveRenomearTipo() {
        ProductTypeDTO renomeado = productTypeService.updateProductType(validTypeId, new ProductTypeDTO(null, "Água mineral 20L"));
        assertEquals("Água mineral 20L", renomeado.getName());

        assertDoesNotThrow(() -> productTypeService.updateProductType(validTypeId, new ProductTypeDTO(null, "ÁGUA MINERAL 20L")));
        assertThrows(BusinessRuleException.class,
                () -> productTypeService.updateProductType(validTypeId, new ProductTypeDTO(null, "Gás")));
    }

    @Test
    @DisplayName("HU12: renomear tipo inexistente é 404")
    void deveRetornar404AoRenomearTipoInexistente() {
        assertThrows(ResourceNotFoundException.class,
                () -> productTypeService.updateProductType(999_999L, new ProductTypeDTO(null, "Qualquer")));
    }
}
