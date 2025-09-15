package com.h2gas.enums;

public enum OrderStatusEnum {
    PENDING("Pendente"),
    CONFIRMED("Confirmado"),
    IN_PREPARATION("Em preparacao"),
    OUT_FOR_DELIVERY("Saiu para entrega"),
    DELIVERED("Entregue"),
    CANCELED("Cancelado");

    private final String status;

    OrderStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
