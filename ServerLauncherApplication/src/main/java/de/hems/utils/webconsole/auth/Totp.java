package de.hems.utils.webconsole.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * The six digit numbers the Google Authenticator app shows, as described in RFC 6238.
 * <p>
 * Implemented here instead of pulled in as a dependency: it is a HMAC and a modulo, and the launcher
 * already ships everything that is needed for it.
 */
public final class Totp {

    /** The alphabet secrets are written in, so they can be typed into an authenticator app. */
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int digits;
    private final int periodSeconds;
    /** How many steps before and after the current one are still accepted, for clocks that drift. */
    private final int window;

    public Totp(int digits, int periodSeconds, int window) {
        this.digits = Math.max(6, Math.min(9, digits));
        this.periodSeconds = Math.max(1, periodSeconds);
        this.window = Math.max(0, window);
    }

    /**
     * @return a fresh secret, ready to be shown as a QR code or typed in by hand
     */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /**
     * Checks a code the user typed in.
     *
     * @param secret the base32 secret of the account
     * @param code   what the user typed
     * @return the time step the code belongs to, or {@code -1} if it is not valid
     */
    public long verify(String secret, String code) {
        if (secret == null || code == null) return -1L;
        String digitsOnly = code.replaceAll("\\s", "");
        if (digitsOnly.length() != digits || !digitsOnly.chars().allMatch(Character::isDigit)) return -1L;
        byte[] key;
        try {
            key = decodeBase32(secret);
        } catch (IllegalArgumentException e) {
            return -1L;
        }
        if (key.length == 0) return -1L;
        long step = System.currentTimeMillis() / 1000L / periodSeconds;
        for (long offset = -window; offset <= window; offset++) {
            if (constantTimeEquals(generate(key, step + offset), digitsOnly)) return step + offset;
        }
        return -1L;
    }

    /**
     * @param key  the raw secret
     * @param step the time step to generate for
     * @return the code of that step, zero padded
     */
    private String generate(byte[] key, long step) {
        byte[] counter = new byte[8];
        long value = step;
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        byte[] hash;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            hash = mac.doFinal(counter);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA1 is not available", e);
        }
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int modulo = (int) Math.pow(10, digits);
        return String.format("%0" + digits + "d", binary % modulo);
    }

    /**
     * The link an authenticator app scans to learn about the account.
     *
     * @param issuer  the name shown above the code in the app
     * @param account the account the code belongs to
     * @param secret  the base32 secret
     * @return the otpauth uri
     */
    public String toUri(String issuer, String account, String secret) {
        String label = encode(issuer) + ":" + encode(account);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1"
                + "&digits=" + digits
                + "&period=" + periodSeconds;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public int getPeriodSeconds() {
        return periodSeconds;
    }

    /**
     * Compares without giving away through timing how many characters matched.
     *
     * @param a the expected value
     * @param b the value that was supplied
     * @return whether both are equal
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int difference = 0;
        for (int i = 0; i < a.length(); i++) difference |= a.charAt(i) ^ b.charAt(i);
        return difference == 0;
    }

    /**
     * @param data the bytes to encode
     * @return those bytes in base32, without padding
     */
    static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) result.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        return result.toString();
    }

    /**
     * @param secret the secret as it is written in the config
     * @return the raw bytes behind it
     * @throws IllegalArgumentException if the secret contains characters that are not base32
     */
    static byte[] decodeBase32(String secret) {
        String cleaned = secret.trim().replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (cleaned.isEmpty()) return new byte[0];
        byte[] result = new byte[cleaned.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : cleaned.toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) throw new IllegalArgumentException("'" + c + "' is not a base32 character");
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
