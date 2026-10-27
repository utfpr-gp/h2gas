package com.h2gas.enums;

import com.h2gas.exceptions.BusinessRuleException;
import org.springframework.data.domain.Sort;

// HU16: ordenações aceitas no catálogo de produtos (parâmetro sort); fora da lista é 400.
// SHOP_RATING não vira Sort: produto não tem nota, então a consulta ordena pela média da loja.
public enum ProductSortEnum {
    NONE(null, Sort.unsorted()),
    PRICE_ASC("priceDelivery,asc", Sort.by(Sort.Direction.ASC, "priceDelivery")),
    PRICE_DESC("priceDelivery,desc", Sort.by(Sort.Direction.DESC, "priceDelivery")),
    NAME("brand,asc", Sort.by(Sort.Direction.ASC, "brand")),
    NEWEST("createdAt,desc", Sort.by(Sort.Direction.DESC, "createdAt")),
    SHOP_RATING("rating", Sort.unsorted());

    private final String param;
    private final Sort sort;

    ProductSortEnum(String param, Sort sort) {
        this.param = param;
        this.sort = sort;
    }

    public Sort sort() {
        return sort;
    }

    public boolean byShopRating() {
        return this == SHOP_RATING;
    }

    public static ProductSortEnum fromParam(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        for (ProductSortEnum sort : values()) {
            if (value.trim().equals(sort.param)) {
                return sort;
            }
        }
        throw new BusinessRuleException("Ordenação inválida.");
    }
}
