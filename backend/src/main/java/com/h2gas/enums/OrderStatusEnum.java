package com.h2gas.enums;

public enum OrderStatusEnum {
    PENDING("pendente"),
    CONFIRMED("confirmado"),
    IN_PREPARATION("em-preparacao"),
    OUT_FOR_DELIVERY("saiu-para-entrega"),
    DELIVERED("entregue"),
    CANCELED("cancelado"),
    FAILED("falhou");

    private final String status;

    OrderStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
