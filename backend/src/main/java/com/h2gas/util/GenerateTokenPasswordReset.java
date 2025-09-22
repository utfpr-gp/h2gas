package com.h2gas.util;

import java.security.SecureRandom;
import java.util.Base64;

public class GenerateTokenPasswordReset {
    public static String generateTokenReset() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
