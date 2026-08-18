package de.hems.utils.webconsole.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hashing and checking of admin passwords.
 * <p>
 * Passwords are stored as PBKDF2 hashes, never in clear text, so the config file of the launcher does not
 * hand out access to whoever gets a copy of it.
 */
public final class Passwords {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {
    }

    /**
     * @param password the password in clear text
     * @return the string that is written into the config
     */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password.toCharArray(), salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    /**
     * Checks a password against a stored hash. Always does the full amount of work, also for a hash that
     * can not be parsed, so a caller can not tell the two cases apart by how long the answer took.
     *
     * @param password the password that was typed in
     * @param stored   the hash from the config
     * @return whether they match
     */
    public static boolean matches(String password, String stored) {
        if (password == null) return false;
        String[] parts = stored == null ? new String[0] : stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            // burn the same amount of time as a real check would take
            derive(password.toCharArray(), new byte[SALT_BYTES], ITERATIONS);
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            derive(password.toCharArray(), new byte[SALT_BYTES], ITERATIONS);
            return false;
        }
    }

    /**
     * Spends the same time as a real check without comparing anything. Used for accounts that do not exist,
     * so an attacker can not find out which user names are real by measuring the response.
     *
     * @param password the password that was typed in
     */
    public static void burn(String password) {
        derive((password == null ? "" : password).toCharArray(), new byte[SALT_BYTES], ITERATIONS);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        }
    }

    /**
     * @param length how many characters the password should have
     * @return a password that is safe to use until the admin picks their own
     */
    public static String generate(int length) {
        String alphabet = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) result.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        return result.toString();
    }

    /**
     * @return a random token that is unguessable, used for sessions and csrf protection
     */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Compares two tokens without leaking through timing how far they matched.
     *
     * @param a the expected token
     * @param b the token that was supplied
     * @return whether they are equal
     */
    public static boolean tokensEqual(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
