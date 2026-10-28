package com.h2gas.services;

import com.h2gas.configuration.ClockConfig;
import com.h2gas.enums.CartUnavailableReasonEnum;
import com.h2gas.model.dto.CartLineDTO;
import com.h2gas.model.dto.CartPreviewDTO;
import com.h2gas.model.dto.CartPreviewItemDTO;
import com.h2gas.model.dto.CartPreviewRequestDTO;
import com.h2gas.model.entity.CategoryProduct;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.ProductType;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.repository.ProductItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// HU20: o carrinho guarda só id + quantidade; o preview devolve dados, preço e disponibilidade atuais
class CartPreviewServiceTest {

    // 10:00 de Brasília — loja aberta = 08:00–18:00, fechada = 19:00–23:00
    private final Clock clock = Clock.fixed(LocalDateTime.of(2026, 10, 27, 10, 0)
            .atZone(ClockConfig.BRASILIA).toInstant(), ClockConfig.BRASILIA);

    private ProductItemRepository productItemRepository;
    private CartPreviewService service;

    @BeforeEach
    void setUp() {
        productItemRepository = mock(ProductItemRepository.class);
        service = new CartPreviewService(productItemRepository, clock);
    }

    private Supplier loja(boolean active, LocalTime opening, LocalTime closing) {
        Supplier supplier = new Supplier();
        supplier.setId("loja-1");
        supplier.setName("Gás Centro");
        supplier.setActive(active);
        supplier.setOpeningTime(opening);
        supplier.setClosingTime(closing);
        return supplier;
    }

    private Supplier lojaAberta() {
        return loja(true, LocalTime.of(8, 0), LocalTime.of(18, 0));
    }

    private ProductItem produto(Long id, int estoque, Supplier supplier) {
        ProductItem productItem = new ProductItem();
        productItem.setId(id);
        productItem.setBrand("Ultragaz");
        productItem.setImageUrl("https://img/p.png");
        productItem.setPriceDelivery(new BigDecimal("110.00"));
        productItem.setPricePickup(new BigDecimal("95.00"));
        productItem.setStockQuantity(estoque);
        productItem.setSupplier(supplier);
        productItem.setCategoryProduct(new CategoryProduct(1L, "Gás 13kg", new ProductType(1L, "Gás", null), null));
        return productItem;
    }

    private CartPreviewRequestDTO pedido(String modo, CartLineDTO... linhas) {
        return new CartPreviewRequestDTO(List.of(linhas), modo);
    }

    private CartPreviewItemDTO unicoItem(CartPreviewDTO preview) {
        assertEquals(1, preview.getItems().size());
        return preview.getItems().get(0);
    }

    @Test
    @DisplayName("HU20: calcula o preço pelo modo (entrega ou retirada) e traz os dados do produto")
    void deveCalcularOPrecoPeloModo() {
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of(produto(5L, 10, lojaAberta())));

        CartPreviewDTO entrega = service.preview(pedido("DELIVERY", new CartLineDTO(5L, 2)));
        CartPreviewDTO retirada = service.preview(pedido("PICKUP", new CartLineDTO(5L, 2)));

        CartPreviewItemDTO item = unicoItem(entrega);
        assertEquals(new BigDecimal("110.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("220.00"), item.getTotalPrice());
        assertEquals("Ultragaz", item.getBrand());
        assertEquals("Gás 13kg", item.getCategory());
        assertEquals("Gás", item.getType());
        assertTrue(item.isAvailable());
        assertNull(item.getUnavailableReason());
        assertEquals(new BigDecimal("220.00"), entrega.getSubtotal());
        assertEquals("loja-1", entrega.getSupplierId());
        assertEquals("Gás Centro", entrega.getSupplierName());
        assertEquals(new BigDecimal("190.00"), retirada.getSubtotal());
    }

    @Test
    @DisplayName("HU20: sem modo informado, vale a entrega")
    void deveUsarEntregaSemModo() {
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of(produto(5L, 10, lojaAberta())));

        assertEquals(new BigDecimal("110.00"), unicoItem(service.preview(pedido(null, new CartLineDTO(5L, 1)))).getUnitPrice());
    }

    @Test
    @DisplayName("HU20: devolve o estoque, para o + respeitar o limite")
    void deveDevolverOEstoque() {
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of(produto(5L, 7, lojaAberta())));

        assertEquals(7, unicoItem(service.preview(pedido("DELIVERY", new CartLineDTO(5L, 1)))).getStockQuantity());
    }

    @Test
    @DisplayName("HU20: produto que saiu do catálogo aparece como indisponível")
    void deveMarcarProdutoInexistente() {
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of());

        CartPreviewItemDTO item = unicoItem(service.preview(pedido("DELIVERY", new CartLineDTO(99L, 1))));

        assertFalse(item.isAvailable());
        assertEquals(CartUnavailableReasonEnum.NOT_FOUND, item.getUnavailableReason());
        assertEquals(99L, item.getProductId());
    }

    @Test
    @DisplayName("HU20: estoque zerado deixa o item indisponível (RN02)")
    void deveMarcarSemEstoque() {
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of(produto(5L, 0, lojaAberta())));

        assertEquals(CartUnavailableReasonEnum.OUT_OF_STOCK,
                unicoItem(service.preview(pedido("DELIVERY", new CartLineDTO(5L, 1)))).getUnavailableReason());
    }

    @Test
    @DisplayName("HU20: estoque menor que a quantidade deixa o item indisponível")
    void deveMarcarEstoqueMenorQueAQuantidade() {
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of(produto(5L, 2, lojaAberta())));

        assertEquals(CartUnavailableReasonEnum.OUT_OF_STOCK,
                unicoItem(service.preview(pedido("DELIVERY", new CartLineDTO(5L, 3)))).getUnavailableReason());
    }

    @Test
    @DisplayName("HU20: loja inativa deixa o item indisponível")
    void deveMarcarLojaInativa() {
        Supplier inativa = loja(false, LocalTime.of(8, 0), LocalTime.of(18, 0));
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of(produto(5L, 10, inativa)));

        assertEquals(CartUnavailableReasonEnum.STORE_INACTIVE,
                unicoItem(service.preview(pedido("DELIVERY", new CartLineDTO(5L, 1)))).getUnavailableReason());
    }

    @Test
    @DisplayName("HU20: loja fora do horário (HU18) deixa o item indisponível")
    void deveMarcarLojaFechadaPeloHorario() {
        Supplier fechada = loja(true, LocalTime.of(19, 0), LocalTime.of(23, 0));
        when(productItemRepository.findAllById(anyIterable())).thenReturn(List.of(produto(5L, 10, fechada)));

        assertEquals(CartUnavailableReasonEnum.STORE_CLOSED,
                unicoItem(service.preview(pedido("DELIVERY", new CartLineDTO(5L, 1)))).getUnavailableReason());
    }

    @Test
    @DisplayName("HU20: item indisponível não entra no subtotal")
    void naoDeveSomarItemIndisponivelNoSubtotal() {
        when(productItemRepository.findAllById(anyIterable()))
                .thenReturn(List.of(produto(5L, 10, lojaAberta()), produto(6L, 0, lojaAberta())));

        CartPreviewDTO preview = service.preview(pedido("DELIVERY", new CartLineDTO(5L, 1), new CartLineDTO(6L, 1)));

        assertEquals(2, preview.getItems().size());
        assertEquals(new BigDecimal("110.00"), preview.getSubtotal());
        assertEquals(6L, preview.getItems().get(1).getProductId());
        assertFalse(preview.getItems().get(1).isAvailable());
    }

    @Test
    @DisplayName("HU20: busca todos os produtos numa consulta só")
    void deveBuscarOsProdutosNumaConsultaSo() {
        when(productItemRepository.findAllById(anyIterable()))
                .thenReturn(List.of(produto(5L, 10, lojaAberta()), produto(6L, 10, lojaAberta())));

        service.preview(pedido("DELIVERY", new CartLineDTO(5L, 1), new CartLineDTO(6L, 1)));

        verify(productItemRepository, times(1)).findAllById(anyIterable());
    }
}
