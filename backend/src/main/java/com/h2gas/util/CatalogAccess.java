package com.h2gas.util;

import com.h2gas.exceptions.BusinessRuleException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// HU13: sem login, o catálogo só abre com a cidade escolhida pelo CEP (RN05)
public final class CatalogAccess {

    public static final String CITY_REQUIRED_MESSAGE = "Informe a cidade para ver o catálogo.";

    private CatalogAccess() {
    }

    public static void requireCityForVisitor(Long cityId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean visitor = auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken;
        if (visitor && cityId == null) {
            throw new BusinessRuleException(CITY_REQUIRED_MESSAGE);
        }
    }
}
