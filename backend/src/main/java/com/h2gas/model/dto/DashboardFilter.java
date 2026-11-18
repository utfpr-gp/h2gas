package com.h2gas.model.dto;

import com.h2gas.enums.DeliveryModeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.entity.Order;
import com.h2gas.model.entity.OrderItem;
import com.h2gas.model.entity.ProductItem;
import com.h2gas.util.OrderFilterRules;

// F7 fatia 6: filtros do dashboard. Pagamento e modo valem para o pedido; tipo, categoria e produto
// selecionam itens (o pedido entra se tiver algum item que bate, e só esses itens somam)
public record DashboardFilter(String payment, DeliveryModeEnum mode, Long typeId, Long categoryId, Long productId) {

    public static final DashboardFilter NONE = new DashboardFilter(null, null, null, null, null);

    public static DashboardFilter parse(String payment, String mode, String typeId, String categoryId, String productId) {
        return new DashboardFilter(OrderFilterRules.parsePayment(payment), OrderFilterRules.parseMode(mode),
                id(typeId, "tipo"), id(categoryId, "categoria"), id(productId, "produto"));
    }

    public boolean hasItemFilter() {
        return typeId != null || categoryId != null || productId != null;
    }

    public boolean matchesOrder(Order order) {
        return (payment == null || payment.equals(order.getPaymentMethod()))
                && (mode == null || mode == order.getDeliveryMode());
    }

    public boolean matchesItem(OrderItem item) {
        ProductItem product = item.getProduct();
        if (product == null) return !hasItemFilter();
        return (productId == null || productId.equals(product.getId()))
                && (categoryId == null || (product.getCategoryProduct() != null && categoryId.equals(product.getCategoryProduct().getId())))
                && (typeId == null || (product.getCategoryProduct() != null && product.getCategoryProduct().getProductType() != null
                && typeId.equals(product.getCategoryProduct().getProductType().getId())));
    }

    private static Long id(String value, String label) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Filtro de " + label + " inválido.");
        }
    }
}
