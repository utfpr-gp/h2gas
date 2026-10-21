package com.h2gas.enums;

import com.h2gas.exceptions.BusinessRuleException;

// F7 fatia 6: filtro de estoque da lista de produtos do fornecedor
public enum StockFilterEnum {
    ALL, AVAILABLE, LOW, OUT;

    public static StockFilterEnum fromParam(String value) {
        if (value == null || value.isBlank()) return ALL;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Filtro de estoque inválido: use all, available, low ou out.");
        }
    }
}
