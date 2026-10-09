package com.h2gas.model.repository;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.City;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.ProductType;
import com.h2gas.model.entity.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProductItemRepositoryTest {

    @Autowired
    private ProductItemRepository productItemRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductTypeRepository productTypeRepository;

    @Autowired
    private CategoryProductRepository categoryProductRepository;

    private Supplier criarFornecedor(City city, String email) {
        Supplier supplier = new Supplier();
        supplier.setName("Fornecedor " + email);
        supplier.setEmail(email);
        supplier.setPassword("senha");
        supplier.setUserType(UserTypeEnum.SUPPLIER);
        supplier.setCnpj("12345678000199");
        supplier.setOpeningTime(LocalTime.of(8, 0));
        supplier.setClosingTime(LocalTime.of(18, 0));
        supplier.setAverageDeliveryTime(Duration.ofMinutes(30));
        supplier.setActive(true);
        supplier.setOpen(true);
        supplier.setCity(city);
        return supplierRepository.saveAndFlush(supplier);
    }

    private ProductItem criarProduto(Supplier supplier, CategoryProduct categoryProduct, String brand, BigDecimal preco) {
        ProductItem productItem = new ProductItem();
        productItem.setBrand(brand);
        productItem.setPriceDelivery(preco);
        productItem.setPricePickup(preco);
        productItem.setStockQuantity(10);
        productItem.setSupplier(supplier);
        productItem.setCategoryProduct(categoryProduct);
        return productItemRepository.saveAndFlush(productItem);
    }

    @Test
    @DisplayName("Deve filtrar produtos por cidade e categoria")
    void deveFiltrarProdutosPorCidadeECategoria() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        City guarapuava = cityRepository.save(new City(null, "Guarapuava", "PR"));

        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás", null));
        ProductType agua = productTypeRepository.save(new ProductType(null, "Água", null));

        Supplier fornecedorCuritiba = criarFornecedor(curitiba, "curitiba@h2gas.test");
        Supplier fornecedorGuarapuava = criarFornecedor(guarapuava, "guarapuava@h2gas.test");

        CategoryProduct categoriaGasCuritiba = new CategoryProduct();
        categoriaGasCuritiba.setName("Gás 13kg");
        categoriaGasCuritiba.setProductType(gas);
        categoriaGasCuritiba.setSupplier(fornecedorCuritiba);
        categoriaGasCuritiba = categoryProductRepository.saveAndFlush(categoriaGasCuritiba);

        CategoryProduct categoriaAguaCuritiba = new CategoryProduct();
        categoriaAguaCuritiba.setName("Água 20L");
        categoriaAguaCuritiba.setProductType(agua);
        categoriaAguaCuritiba.setSupplier(fornecedorCuritiba);
        categoriaAguaCuritiba = categoryProductRepository.saveAndFlush(categoriaAguaCuritiba);

        CategoryProduct categoriaGasGuarapuava = new CategoryProduct();
        categoriaGasGuarapuava.setName("Gás 13kg");
        categoriaGasGuarapuava.setProductType(gas);
        categoriaGasGuarapuava.setSupplier(fornecedorGuarapuava);
        categoriaGasGuarapuava = categoryProductRepository.saveAndFlush(categoriaGasGuarapuava);

        criarProduto(fornecedorCuritiba, categoriaGasCuritiba, "Botijão Curitiba", new BigDecimal("100.00"));
        criarProduto(fornecedorCuritiba, categoriaAguaCuritiba, "Galão Curitiba", new BigDecimal("20.00"));
        criarProduto(fornecedorGuarapuava, categoriaGasGuarapuava, "Botijão Guarapuava", new BigDecimal("90.00"));

        Page<ProductItem> result = productItemRepository.findAvailableProducts(
                curitiba.getId(), "Gás", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Botijão Curitiba", result.getContent().get(0).getBrand());
    }

    @Test
    @DisplayName("Deve retornar todos quando cidade e categoria são nulos")
    void deveRetornarTodosQuandoCidadeECategoriaNulos() {
        City city = cityRepository.save(new City(null, "Pato Branco", "PR"));
        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás", null));
        Supplier supplier = criarFornecedor(city, "patobranco@h2gas.test");

        CategoryProduct categoria = new CategoryProduct();
        categoria.setName("Gás 13kg");
        categoria.setProductType(gas);
        categoria.setSupplier(supplier);
        categoria = categoryProductRepository.saveAndFlush(categoria);

        criarProduto(supplier, categoria, "Botijão 1", new BigDecimal("100.00"));
        criarProduto(supplier, categoria, "Botijão 2", new BigDecimal("110.00"));

        Page<ProductItem> result = productItemRepository.findAvailableProducts(
                null, null, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Deve combinar filtro de categoria e busca por marca na listagem de um fornecedor")
    void deveCombinarFiltroDeCategoriaEBuscaPorMarca() {
        City city = cityRepository.save(new City(null, "Pato Branco", "PR"));
        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás", null));
        ProductType agua = productTypeRepository.save(new ProductType(null, "Água", null));
        Supplier supplier = criarFornecedor(city, "combinado@h2gas.test");

        CategoryProduct categoriaGas = new CategoryProduct();
        categoriaGas.setName("Gás 13kg");
        categoriaGas.setProductType(gas);
        categoriaGas.setSupplier(supplier);
        categoriaGas = categoryProductRepository.saveAndFlush(categoriaGas);

        CategoryProduct categoriaAgua = new CategoryProduct();
        categoriaAgua.setName("Água 20L");
        categoriaAgua.setProductType(agua);
        categoriaAgua.setSupplier(supplier);
        categoriaAgua = categoryProductRepository.saveAndFlush(categoriaAgua);

        criarProduto(supplier, categoriaGas, "UltragazFixture", new BigDecimal("120.00"));
        criarProduto(supplier, categoriaAgua, "CopapFixture", new BigDecimal("15.00"));
        criarProduto(supplier, categoriaAgua, "MinalbaFixture", new BigDecimal("25.00"));

        Page<ProductItem> apenasCategoria = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), "Água", null, PageRequest.of(0, 10));
        assertEquals(2, apenasCategoria.getTotalElements());

        Page<ProductItem> apenasBusca = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), null, "copap", PageRequest.of(0, 10));
        assertEquals(1, apenasBusca.getTotalElements());
        assertEquals("CopapFixture", apenasBusca.getContent().get(0).getBrand());

        Page<ProductItem> categoriaEBusca = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), "Água", "copap", PageRequest.of(0, 10));
        assertEquals(1, categoriaEBusca.getTotalElements());
        assertEquals("CopapFixture", categoriaEBusca.getContent().get(0).getBrand());
    }
}
