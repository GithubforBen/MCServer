package de.hems.utils.webconsole.auth;

/**
 * A login that is currently valid.
 */
public class Session {

    private final String token;
    private final String csrfToken;
    private final String username;
    private final long createdAt;
    private volatile long expiresAt;

    public Session(String token, String csrfToken, String username, long expiresAt) {
        this.token = token;
        this.csrfToken = csrfToken;
        this.username = username;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
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

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
