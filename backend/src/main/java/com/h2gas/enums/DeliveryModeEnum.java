package com.h2gas.enums;

public enum DeliveryModeEnum {
    DELIVERY("Entrega no local"),
    PICKUP("Retirada na loja");

    private final String mode;

    DeliveryModeEnum(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return this.mode;
    }
}
