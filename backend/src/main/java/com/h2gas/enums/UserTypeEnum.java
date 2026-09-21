package com.h2gas.enums;

public enum UserTypeEnum {
    ADMIN("admin"),
    CUSTOMER("customer"),
    SUPPLIER("supplier"),
    DELIVERER("deliverer");

    private String type;

    UserTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
