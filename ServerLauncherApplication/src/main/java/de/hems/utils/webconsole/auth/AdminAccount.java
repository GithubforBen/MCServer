package de.hems.utils.webconsole.auth;

/**
 * One account that may log into the admin website.
 */
public class AdminAccount {

    private final String username;
    private String passwordHash;
    private String totpSecret;

    public AdminAccount(String username, String passwordHash, String totpSecret) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.totpSecret = totpSecret;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    /**
     * @return whether the account can actually be logged into
     */
    public boolean isUsable() {
        return passwordHash != null && !passwordHash.isBlank()
                && totpSecret != null && !totpSecret.isBlank();
    }
}
