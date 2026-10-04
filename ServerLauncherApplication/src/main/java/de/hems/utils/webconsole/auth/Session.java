package de.hems.utils.webconsole.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A login that is currently valid.
 * <p>
 * The launcher only ever keeps the hash of the token, never the token itself - sessions are written to disk
 * so a login survives a restart, and a copy of that file must not be enough to log in. The token is only
 * known right after the login, long enough to put it into the cookie.
 */
public class Session {

    /** Only set on a session that was just created, so the cookie can be written. */
    private final transient String token;
    private final String id;
    private final String csrfToken;
    private final String username;
    private final long createdAt;
    private final boolean remember;
    private volatile long expiresAt;
    /** What the account's password looked like at login - when it changes, the session is over. */
    private volatile String accountStamp;

    /**
     * A fresh login.
     */
    public Session(String token, String csrfToken, String username, long expiresAt, boolean remember,
                   String accountStamp) {
        this(token, hash(token), csrfToken, username, System.currentTimeMillis(), expiresAt, remember, accountStamp);
    }

    /**
     * A login read back from disk, whose token is no longer known.
     */
    public Session(String id, String csrfToken, String username, long createdAt, long expiresAt, boolean remember,
                   String accountStamp) {
        this(null, id, csrfToken, username, createdAt, expiresAt, remember, accountStamp);
    }

    private Session(String token, String id, String csrfToken, String username, long createdAt, long expiresAt,
                    boolean remember, String accountStamp) {
        this.token = token;
        this.id = id;
        this.csrfToken = csrfToken;
        this.username = username;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.remember = remember;
        this.accountStamp = accountStamp;
    }

    /**
     * @param token a session token
     * @return the id a session with that token is kept under
     */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from this jvm", e);
        }
    }

    /**
     * @return the token for the cookie - only on a session that was just created, {@code null} otherwise
     */
    public String getToken() {
        return token;
    }

    /**
     * @return what identifies the session without giving its token away
     */
    public String getId() {
        return id;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public String getUsername() {
        return username;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * @return whether "Angemeldet bleiben" was ticked - the session then runs for a fixed time from the login
     *         instead of ending after a while without use
     */
    public boolean isRemember() {
        return remember;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getAccountStamp() {
        return accountStamp;
    }

    public void setAccountStamp(String accountStamp) {
        this.accountStamp = accountStamp;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
