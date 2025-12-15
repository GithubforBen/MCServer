package de.hems.types;

import java.security.SecureRandom;
import java.util.Base64;

public class SecureTokenGenerator {
    public static String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = random.generateSeed(1024);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
