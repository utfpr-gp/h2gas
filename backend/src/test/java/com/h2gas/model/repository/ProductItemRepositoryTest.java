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

    // HU18: a consulta recebe a hora atual; os fornecedores de teste abrem das 08:00 às 18:00
    private static final java.time.LocalTime DEZ_HORAS = java.time.LocalTime.of(10, 0);

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
        // HU18: aberta pelo horário (o relógio dos testes marca 10:00)
        supplier.setOpeningTime(java.time.LocalTime.of(8, 0));
        supplier.setClosingTime(java.time.LocalTime.of(18, 0));
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
        categoriaGasCuritiba = categoryProductRepository.saveAndFlush(categoriaGasCuritiba);

        CategoryProduct categoriaAguaCuritiba = new CategoryProduct();
        categoriaAguaCuritiba.setName("Água 20L");
        categoriaAguaCuritiba.setProductType(agua);
        categoriaAguaCuritiba = categoryProductRepository.saveAndFlush(categoriaAguaCuritiba);

        CategoryProduct categoriaGasGuarapuava = new CategoryProduct();
        categoriaGasGuarapuava.setName("Gás 13kg");
        categoriaGasGuarapuava.setProductType(gas);
        categoriaGasGuarapuava = categoryProductRepository.saveAndFlush(categoriaGasGuarapuava);

        criarProduto(fornecedorCuritiba, categoriaGasCuritiba, "Botijão Curitiba", new BigDecimal("100.00"));
        criarProduto(fornecedorCuritiba, categoriaAguaCuritiba, "Galão Curitiba", new BigDecimal("20.00"));
        criarProduto(fornecedorGuarapuava, categoriaGasGuarapuava, "Botijão Guarapuava", new BigDecimal("90.00"));

        Page<ProductItem> result = productItemRepository.findAvailableProducts(curitiba.getId(), "Gás", DEZ_HORAS, PageRequest.of(0, 10));

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
        categoria = categoryProductRepository.saveAndFlush(categoria);

        criarProduto(supplier, categoria, "Botijão 1", new BigDecimal("100.00"));
        criarProduto(supplier, categoria, "Botijão 2", new BigDecimal("110.00"));

        Page<ProductItem> result = productItemRepository.findAvailableProducts(null, null, DEZ_HORAS, PageRequest.of(0, 10));

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
        categoriaGas = categoryProductRepository.saveAndFlush(categoriaGas);

        CategoryProduct categoriaAgua = new CategoryProduct();
        categoriaAgua.setName("Água 20L");
        categoriaAgua.setProductType(agua);
        categoriaAgua = categoryProductRepository.saveAndFlush(categoriaAgua);

        criarProduto(supplier, categoriaGas, "UltragazFixture", new BigDecimal("120.00"));
        criarProduto(supplier, categoriaAgua, "CopapFixture", new BigDecimal("15.00"));
        criarProduto(supplier, categoriaAgua, "MinalbaFixture", new BigDecimal("25.00"));

        Page<ProductItem> apenasCategoria = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), "Água", null, false, PageRequest.of(0, 10));
        assertEquals(2, apenasCategoria.getTotalElements());

        Page<ProductItem> apenasBusca = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), null, "copap", false, PageRequest.of(0, 10));
        assertEquals(1, apenasBusca.getTotalElements());
        assertEquals("CopapFixture", apenasBusca.getContent().get(0).getBrand());

        Page<ProductItem> categoriaEBusca = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), "Água", "copap", false, PageRequest.of(0, 10));
        assertEquals(1, categoriaEBusca.getTotalElements());
        assertEquals("CopapFixture", categoriaEBusca.getContent().get(0).getBrand());
    }

    private CategoryProduct categoriaDe(Supplier supplier, ProductType tipo) {
        CategoryProduct categoria = new CategoryProduct();
        categoria.setName("Gás 13kg");
        categoria.setProductType(tipo);
        return categoryProductRepository.saveAndFlush(categoria);
    }

    private ProductItem criarProdutoComEstoque(Supplier supplier, CategoryProduct categoria, String brand, Integer estoque) {
        ProductItem produto = criarProduto(supplier, categoria, brand, new BigDecimal("100.00"));
        produto.setStockQuantity(estoque);
        return productItemRepository.saveAndFlush(produto);
    }

    @Test
    @DisplayName("HU09/RN02: onlyAvailable=true oculta produtos com estoque zero ou nulo")
    void deveOcultarProdutosZeradosQuandoOnlyAvailableTrue() {
        City city = cityRepository.save(new City(null, "Pato Branco", "PR"));
        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás", null));
        Supplier supplier = criarFornecedor(city, "estoque.true@h2gas.test");
        CategoryProduct categoria = categoriaDe(supplier, gas);
        criarProdutoComEstoque(supplier, categoria, "Com estoque", 3);
        criarProdutoComEstoque(supplier, categoria, "Zerado", 0);
        criarProdutoComEstoque(supplier, categoria, "Sem controle", null);

        Page<ProductItem> result = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), null, null, true, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Com estoque", result.getContent().get(0).getBrand());
    }

    @Test
    @DisplayName("HU09/RN02: onlyAvailable=false mantém a visão do fornecedor com os itens zerados para repor")
    void deveExibirProdutosZeradosQuandoOnlyAvailableFalse() {
        City city = cityRepository.save(new City(null, "Pato Branco", "PR"));
        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás", null));
        Supplier supplier = criarFornecedor(city, "estoque.false@h2gas.test");
        CategoryProduct categoria = categoriaDe(supplier, gas);
        criarProdutoComEstoque(supplier, categoria, "Com estoque", 3);
        criarProdutoComEstoque(supplier, categoria, "Zerado", 0);

        Page<ProductItem> result = productItemRepository.findBySupplierIdFiltered(
                supplier.getId(), null, null, false, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("HU09/RN02: a vitrine aberta nunca traz produto zerado")
    void deveOcultarProdutosZeradosNaVitrineAberta() {
        City city = cityRepository.save(new City(null, "Pato Branco", "PR"));
        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás", null));
        Supplier supplier = criarFornecedor(city, "vitrine.estoque@h2gas.test");
        CategoryProduct categoria = categoriaDe(supplier, gas);
        criarProdutoComEstoque(supplier, categoria, "Com estoque", 2);
        criarProdutoComEstoque(supplier, categoria, "Zerado", 0);

        Page<ProductItem> result = productItemRepository.findAvailableProducts(city.getId(), null, DEZ_HORAS, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Com estoque", result.getContent().get(0).getBrand());
    }

    // ---- HU16: ordenação dos produtos ----

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    private ProductItem produtoDe(Supplier loja, String marca, String preco) {
        ProductType gas = productTypeRepository.save(new ProductType(null, "Gás " + marca, null));
        CategoryProduct categoria = new CategoryProduct();
        categoria.setName("P13");
        categoria.setProductType(gas);
        return criarProduto(loja, categoryProductRepository.saveAndFlush(categoria), marca, new BigDecimal(preco));
    }

    private void avaliar(Supplier loja, com.h2gas.model.entity.User cliente, int nota) {
        com.h2gas.model.entity.Order pedido = new com.h2gas.model.entity.Order();
        pedido.setCustomer(cliente);
        pedido.setSupplier(loja);
        pedido.setStatus(com.h2gas.enums.OrderStatusEnum.DELIVERED);
        pedido = orderRepository.save(pedido);
        com.h2gas.model.entity.Review avaliacao = new com.h2gas.model.entity.Review();
        avaliacao.setOrder(pedido);
        avaliacao.setSupplier(loja);
        avaliacao.setRating(nota);
        reviewRepository.saveAndFlush(avaliacao);
    }

    private com.h2gas.model.entity.User cliente() {
        return userRepository.save(new com.h2gas.model.entity.User(null, "Cliente", "cliente-hu16@h2gas.test",
                "41999990000", "99988877766", "x", UserTypeEnum.CUSTOMER));
    }

    private java.util.List<String> marcas(City cidade, com.h2gas.enums.ProductSortEnum ordem) {
        return productItemRepository.findAvailableProducts(cidade.getId(), null, null, ordem.byShopRating(), DEZ_HORAS, PageRequest.of(0, 10, ordem.sort()))
                .getContent().stream().map(ProductItem::getBrand).toList();
    }

    @Test
    @DisplayName("HU16: melhor avaliado ordena pela nota da loja que vende o produto")
    void deveOrdenarProdutosPelaNotaDaLoja() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        com.h2gas.model.entity.User cliente = cliente();
        Supplier media = criarFornecedor(curitiba, "media@h2gas.test");
        Supplier otima = criarFornecedor(curitiba, "otima@h2gas.test");
        produtoDe(media, "Da Loja Média", "100.00");
        produtoDe(otima, "Da Loja Ótima", "120.00");
        avaliar(media, cliente, 3);
        avaliar(otima, cliente, 5);

        assertEquals(java.util.List.of("Da Loja Ótima", "Da Loja Média"),
                marcas(curitiba, com.h2gas.enums.ProductSortEnum.SHOP_RATING));
    }

    @Test
    @DisplayName("HU16: produto de loja sem avaliação fica no fim")
    void deveColocarProdutoDeLojaSemNotaNoFim() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        com.h2gas.model.entity.User cliente = cliente();
        produtoDe(criarFornecedor(curitiba, "semnota@h2gas.test"), "Sem Nota", "50.00");
        Supplier avaliada = criarFornecedor(curitiba, "avaliada@h2gas.test");
        produtoDe(avaliada, "Com Nota", "60.00");
        avaliar(avaliada, cliente, 1);

        assertEquals(java.util.List.of("Com Nota", "Sem Nota"),
                marcas(curitiba, com.h2gas.enums.ProductSortEnum.SHOP_RATING));
    }

    @Test
    @DisplayName("HU16: maior preço primeiro")
    void deveOrdenarProdutosPorMaiorPreco() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        Supplier loja = criarFornecedor(curitiba, "precos@h2gas.test");
        produtoDe(loja, "Barato", "50.00");
        produtoDe(loja, "Caro", "150.00");
        produtoDe(loja, "Médio", "100.00");

        assertEquals(java.util.List.of("Caro", "Médio", "Barato"),
                marcas(curitiba, com.h2gas.enums.ProductSortEnum.PRICE_DESC));
        assertEquals(java.util.List.of("Barato", "Médio", "Caro"),
                marcas(curitiba, com.h2gas.enums.ProductSortEnum.PRICE_ASC));
    }

    @Test
    @DisplayName("HU16: nome (A-Z) ordena pela marca")
    void deveOrdenarProdutosPorNome() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        Supplier loja = criarFornecedor(curitiba, "nomes@h2gas.test");
        produtoDe(loja, "Ultragaz", "100.00");
        produtoDe(loja, "Copagaz", "100.00");
        produtoDe(loja, "Liquigás", "100.00");

        assertEquals(java.util.List.of("Copagaz", "Liquigás", "Ultragaz"),
                marcas(curitiba, com.h2gas.enums.ProductSortEnum.NAME));
    }

    @Test
    @DisplayName("HU16: mais recentes primeiro, pela data de cadastro")
    void deveOrdenarProdutosPelosMaisRecentes() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        Supplier loja = criarFornecedor(curitiba, "recentes@h2gas.test");
        ProductItem antigo = produtoDe(loja, "Antigo", "100.00");
        ProductItem novo = produtoDe(loja, "Novo", "100.00");
        // created_at vem do @CreationTimestamp; gravamos datas distintas para a ordem não depender do relógio
        entityManager.getEntityManager().createNativeQuery("UPDATE product_items SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, java.time.LocalDateTime.of(2026, 1, 1, 8, 0)).setParameter(2, antigo.getId()).executeUpdate();
        entityManager.getEntityManager().createNativeQuery("UPDATE product_items SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, java.time.LocalDateTime.of(2026, 6, 1, 8, 0)).setParameter(2, novo.getId()).executeUpdate();
        entityManager.clear();

        assertEquals(java.util.List.of("Novo", "Antigo"), marcas(curitiba, com.h2gas.enums.ProductSortEnum.NEWEST));
    }

    @Test
    @DisplayName("HU18: produto de loja fora do horário não aparece no catálogo")
    void naoDeveListarProdutoDeLojaForaDoHorario() {
        City curitiba = cityRepository.save(new City(null, "Curitiba", "PR"));
        produtoDe(criarFornecedor(curitiba, "aberta@h2gas.test"), "Da Aberta", "100.00");
        Supplier fechada = criarFornecedor(curitiba, "fechada@h2gas.test");
        fechada.setOpeningTime(java.time.LocalTime.of(19, 0));
        fechada.setClosingTime(java.time.LocalTime.of(23, 0));
        supplierRepository.saveAndFlush(fechada);
        produtoDe(fechada, "Da Fechada", "100.00");

        assertEquals(java.util.List.of("Da Aberta"), marcas(curitiba, com.h2gas.enums.ProductSortEnum.NAME));
    }
}
