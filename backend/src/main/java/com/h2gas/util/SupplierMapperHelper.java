package com.h2gas.util;

import com.h2gas.model.entity.Supplier;

import java.time.Duration;
import java.time.LocalTime;

public class SupplierMapperHelper {
    public static void applyTimeFields(Supplier supplier, String openingTime, String closingTime, Long averageDeliveryTime) {
        if (openingTime != null) {
            supplier.setOpeningTime(LocalTime.parse(openingTime));
        }

        if (closingTime != null) {
            supplier.setClosingTime(LocalTime.parse(closingTime));
        }

        if (averageDeliveryTime != null) {
            supplier.setAverageDeliveryTime(Duration.ofMinutes(averageDeliveryTime));
        }
    }
}
