package com.h2gas.model;

import java.time.LocalTime;

// HU18: regra única de "loja aberta agora" — vale para a vitrine, o catálogo e o cabeçalho da loja.
// A abertura conta e o fechamento não; fechamento antes da abertura = vira a meia-noite.
// As consultas repetem a mesma regra em JPQL (SupplierRepository.OPEN_NOW, ProductItemRepository.SUPPLIER_OPEN_NOW).
public final class OpeningHours {

    private OpeningHours() {
    }

    public static boolean isOpenAt(LocalTime opening, LocalTime closing, LocalTime now) {
        if (opening == null || closing == null || opening.equals(closing)) {
            return false;
        }
        if (opening.isBefore(closing)) {
            return !now.isBefore(opening) && now.isBefore(closing);
        }
        return !now.isBefore(opening) || now.isBefore(closing);
    }
}
