package com.h2gas.enums;

public enum UserTypeEnum {
    ADMIN("admin"),
    CUSTOMER("customer"),
    SUPPLIER("supplier");

    private String type;

    UserTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
