package com.h2gas.enums;

import com.h2gas.exceptions.BusinessRuleException;

// HU16: ordenações aceitas na vitrine de lojas (parâmetro sort); fora da lista é 400
public enum StorefrontSortEnum {
    NAME("name"),
    RATING("rating"),
    // HU18: "abertas primeiro" deixou de ser opção — em toda ordenação as fechadas vêm depois das abertas
    DELIVERY_TIME("deliveryTime");

    private final String param;

    StorefrontSortEnum(String param) {
        this.param = param;
    }

    public String param() {
        return param;
    }

    public static StorefrontSortEnum fromParam(String value) {
        if (value == null || value.isBlank()) {
            return NAME;
        }
        for (StorefrontSortEnum sort : values()) {
            if (sort.param.equals(value.trim())) {
                return sort;
            }
        }
        throw new BusinessRuleException("Ordenação inválida.");
    }
}
