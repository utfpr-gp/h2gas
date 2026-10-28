package com.h2gas.services;

import com.h2gas.enums.CartUnavailableReasonEnum;
import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.model.OpeningHours;
import com.h2gas.model.dto.CartLineDTO;
import com.h2gas.model.dto.CartPreviewDTO;
import com.h2gas.model.dto.CartPreviewItemDTO;
import com.h2gas.model.dto.CartPreviewRequestDTO;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.repository.ProductItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// HU20: o navegador guarda só id + quantidade; aqui vêm os dados, o preço (RN01) e a disponibilidade atuais
@Service
public class CartPreviewService {

    private final ProductItemRepository productItemRepository;
    private final Clock clock;

    public CartPreviewService(ProductItemRepository productItemRepository, Clock clock) {
        this.productItemRepository = productItemRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CartPreviewDTO preview(CartPreviewRequestDTO request) {
        DeliveryModeEnum mode = "PICKUP".equals(request.getDeliveryMode()) ? DeliveryModeEnum.PICKUP : DeliveryModeEnum.DELIVERY;
        List<Long> ids = request.getItems().stream().map(CartLineDTO::getProductId).distinct().toList();
        Map<Long, ProductItem> products = productItemRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductItem::getId, Function.identity()));
        LocalTime now = LocalTime.now(clock);

        List<CartPreviewItemDTO> items = request.getItems().stream()
                .map(line -> toItem(line, products.get(line.getProductId()), mode, now))
                .toList();

        BigDecimal subtotal = items.stream()
                .filter(CartPreviewItemDTO::isAvailable)
                .map(CartPreviewItemDTO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Supplier supplier = request.getItems().stream()
                .map(line -> products.get(line.getProductId()))
                .filter(Objects::nonNull)
                .map(ProductItem::getSupplier)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        return new CartPreviewDTO(supplier == null ? null : supplier.getId(),
                supplier == null ? null : supplier.getName(), items, subtotal);
    }

    private CartPreviewItemDTO toItem(CartLineDTO line, ProductItem product, DeliveryModeEnum mode, LocalTime now) {
        int quantity = line.getQuantity();
        if (product == null) {
            return CartPreviewItemDTO.builder().productId(line.getProductId()).quantity(quantity)
                    .available(false).unavailableReason(CartUnavailableReasonEnum.NOT_FOUND).build();
        }
        CartUnavailableReasonEnum reason = unavailableReason(product, quantity, now);
        BigDecimal unitPrice = OrderItemService.unitPriceFor(product, mode);
        var category = product.getCategoryProduct();
        return CartPreviewItemDTO.builder()
                .productId(product.getId())
                .brand(product.getBrand())
                .category(category == null ? null : category.getName())
                .type(category == null || category.getProductType() == null ? null : category.getProductType().getName())
                .imageUrl(product.getImageUrl())
                .quantity(quantity)
                .stockQuantity(product.getStockQuantity())
                .unitPrice(unitPrice)
                .totalPrice(OrderItemService.calculateTotalPrice(quantity, unitPrice))
                .available(reason == null)
                .unavailableReason(reason)
                .build();
    }

    private CartUnavailableReasonEnum unavailableReason(ProductItem product, int quantity, LocalTime now) {
        Supplier supplier = product.getSupplier();
        if (supplier == null || !supplier.isActive()) {
            return CartUnavailableReasonEnum.STORE_INACTIVE;
        }
        if (!OpeningHours.isOpenAt(supplier.getOpeningTime(), supplier.getClosingTime(), now)) {
            return CartUnavailableReasonEnum.STORE_CLOSED;
        }
        Integer stock = product.getStockQuantity();
        if (stock == null || stock <= 0 || stock < quantity) {
            return CartUnavailableReasonEnum.OUT_OF_STOCK;
        }
        return null;
    }
}
