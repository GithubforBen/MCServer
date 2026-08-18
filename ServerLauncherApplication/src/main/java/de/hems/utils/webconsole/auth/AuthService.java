package de.hems.utils.webconsole.auth;

import de.hems.utils.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The login of the admin website.
 * <p>
 * Two rules shape this class:
 * <ul>
 *   <li>A password is only ever compared once a valid authenticator code was supplied. A request without a
 *       code, or with a wrong one, never reaches the password check - so the website can not be used to
 *       find out whether a password is right by itself.</li>
 *   <li>Every attempt is answered only after the grace period has passed, so all outcomes take exactly the
 *       same time no matter where they failed, and the grace period then starts again from the moment that
 *       answer goes out. Between two attempts there are therefore always at least the configured seconds,
 *       whether they come one after another or at the same time.</li>
 * </ul>
 */
public class AuthService {

    /** Where the accounts live inside the launcher config. */
    private static final String ACCOUNTS_PATH = "web.admins";

    private final Configuration configuration;
    private final Totp totp;
    private final long graceMillis;
    private final long sessionTimeoutMillis;
    private final String issuer;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    /** When the grace period of a given key - account or address - runs out. */
    private final Map<String, Long> graceUntil = new ConcurrentHashMap<>();
    /** The last authenticator step that was accepted per account, so a code can not be replayed. */
    private final Map<String, Long> usedTotpStep = new ConcurrentHashMap<>();

    public AuthService(Configuration configuration) {
        this.configuration = configuration;
        YamlConfiguration config = configuration.getConfig();
        this.graceMillis = Math.max(0L, config.getLong("web.grace-period-seconds", 3L)) * 1000L;
        this.sessionTimeoutMillis = Math.max(1L, config.getLong("web.session-timeout-minutes", 60L)) * 60_000L;
        this.issuer = config.getString("web.totp.issuer", "MCServer");
        this.totp = new Totp(
                config.getInt("web.totp.digits", 6),
                config.getInt("web.totp.period-seconds", 30),
                config.getInt("web.totp.window", 1));
    }

    /**
     * Logs a user in.
     * <p>
     * The answer is never given before the grace period has passed, and the grace period starts over once
     * it is given - so an attempt costs the caller the grace period twice over, which is what makes trying
     * passwords in bulk pointless.
     *
     * @param username   the account
     * @param password   the password
     * @param token      the number from the authenticator app - without it nothing is checked
     * @param clientKey  something identifying the caller, used to keep grace periods apart
     * @return how the attempt ended
     */
    public LoginResult login(String username, String password, String token, String clientKey) {
        long startedAt = System.currentTimeMillis();
        String account = normalize(username);

        if (account.isEmpty() || password == null || password.isEmpty()) {
            return LoginResult.failure(LoginResult.Status.INCOMPLETE,
                    "Benutzername und Passwort werden benötigt.");
        }

        // the code has to be there before anything is checked - this is what starts a password check
        if (token == null || token.isBlank()) {
            return LoginResult.failure(LoginResult.Status.TOKEN_REQUIRED,
                    "Ohne Google Authenticator Code wird das Passwort nicht geprüft.");
        }

        long remaining = graceRemaining(account, clientKey);
        if (remaining > 0L) {
            return LoginResult.grace((remaining + 999L) / 1000L);
        }
        startGrace(account, clientKey);

        try {
            AdminAccount admin = findAccount(account);
            if (admin == null || !admin.isUsable()) {
                // spend the same time as a real check so missing accounts are indistinguishable
                Passwords.burn(password);
                return admin == null && listAccounts().isEmpty()
                        ? LoginResult.failure(LoginResult.Status.NO_ACCOUNT,
                        "Es ist kein Admin-Account eingerichtet. Siehe die Konsole des Launchers.")
                        : LoginResult.failure(LoginResult.Status.INVALID_CREDENTIALS,
                        "Code oder Passwort stimmen nicht.");
            }

            // step one: the authenticator code
            long step = totp.verify(admin.getTotpSecret(), token);
            if (step < 0L) {
                Passwords.burn(password);
                return LoginResult.failure(LoginResult.Status.INVALID_TOKEN,
                        "Der Google Authenticator Code stimmt nicht. Das Passwort wurde nicht geprüft.");
            }
            Long lastStep = usedTotpStep.get(account);
            if (lastStep != null && step <= lastStep) {
                Passwords.burn(password);
                return LoginResult.failure(LoginResult.Status.TOKEN_ALREADY_USED,
                        "Dieser Code wurde schon benutzt. Bitte auf den nächsten warten.");
            }

            // step two: only now the password is looked at
            if (!Passwords.matches(password, admin.getPasswordHash())) {
                return LoginResult.failure(LoginResult.Status.INVALID_CREDENTIALS,
                        "Code oder Passwort stimmen nicht.");
            }

            usedTotpStep.put(account, step);
            return LoginResult.success(createSession(admin.getUsername()));
        } finally {
            // every outcome leaves through here, so they all take the same time ...
            waitOutGracePeriod(startedAt);
            // ... and the next attempt is only allowed a full grace period after this answer
            startGrace(account, clientKey);
        }
    }

    /**
     * Holds the answer back until the grace period that started with this attempt is over.
     *
     * @param startedAt when the attempt came in
     */
    private void waitOutGracePeriod(long startedAt) {
        long remaining = graceMillis - (System.currentTimeMillis() - startedAt);
        while (remaining > 0L) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            remaining = graceMillis - (System.currentTimeMillis() - startedAt);
        }
    }

    /**
     * @param account   the account that is being logged into
     * @param clientKey who is trying
     * @return how many milliseconds are left of a running grace period, or 0 if there is none
     */
    // kept package private so the grace period can be inspected from tests
    private long graceRemaining(String account, String clientKey) {
        long now = System.currentTimeMillis();
        long remaining = 0L;
        for (String key : graceKeys(account, clientKey)) {
            Long until = graceUntil.get(key);
            if (until == null) continue;
            if (until <= now) {
                graceUntil.remove(key, until);
                continue;
            }
            remaining = Math.max(remaining, until - now);
        }
        return remaining;
    }

    private void startGrace(String account, String clientKey) {
        long until = System.currentTimeMillis() + graceMillis;
        for (String key : graceKeys(account, clientKey)) graceUntil.put(key, until);
    }

    /**
     * A grace period is kept both per account and per caller, so one account can not be hammered from many
     * addresses and one address can not walk through many accounts.
     */
    private List<String> graceKeys(String account, String clientKey) {
        List<String> keys = new ArrayList<>(2);
        keys.add("user:" + account);
        if (clientKey != null && !clientKey.isBlank()) keys.add("client:" + clientKey);
        return keys;
    }

    /**
     * @return how long a login is blocked after an attempt, in seconds
     */
    public long getGraceSeconds() {
        return graceMillis / 1000L;
    }

    private Session createSession(String username) {
        cleanUpSessions();
        Session session = new Session(Passwords.randomToken(), Passwords.randomToken(), username,
                System.currentTimeMillis() + sessionTimeoutMillis);
        sessions.put(session.getToken(), session);
        return session;
    }

    /**
     * Looks a session up and pushes its expiry back, so an admin that is working is not thrown out.
     *
     * @param token the token from the cookie
     * @return the session, or {@code null} if it is unknown or has run out
     */
    public Session getSession(String token) {
        if (token == null || token.isEmpty()) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (session.isExpired()) {
            sessions.remove(token);
            return null;
        }
        session.setExpiresAt(System.currentTimeMillis() + sessionTimeoutMillis);
        return session;
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    private void cleanUpSessions() {
        sessions.values().removeIf(Session::isExpired);
    }

    /**
     * @return how long a session stays valid without being used, in seconds
     */
    public long getSessionTimeoutSeconds() {
        return sessionTimeoutMillis / 1000L;
    }

    /**
     * @param username the account to look up
     * @return the account, or {@code null} if there is none with that name
     */
    public AdminAccount findAccount(String username) {
        ConfigurationSection section = config().getConfigurationSection(ACCOUNTS_PATH);
        if (section == null) return null;
        String normalized = normalize(username);
        for (String key : section.getKeys(false)) {
            if (!normalize(key).equals(normalized)) continue;
            return new AdminAccount(key,
                    section.getString(key + ".password"),
                    section.getString(key + ".totp-secret"));
        }
        return null;
    }

    /**
     * @return the names of every configured account
     */
    public List<String> listAccounts() {
        ConfigurationSection section = config().getConfigurationSection(ACCOUNTS_PATH);
        if (section == null) return List.of();
        return List.copyOf(section.getKeys(false));
    }

    /**
     * Creates an account, or replaces the password and secret of one that exists.
     *
     * @param username the account
     * @param password the password in clear text
     * @param secret   the base32 secret for the authenticator app
     */
    public void saveAccount(String username, String password, String secret) {
        String path = ACCOUNTS_PATH + "." + username;
        config().set(path + ".password", Passwords.hash(password));
        config().set(path + ".totp-secret", secret);
        configuration.save();
    }

    /**
     * Sets up a first account if there is none, and prints how to log in. Without this the website would be
     * unreachable after a fresh install.
     */
    public void ensureAccountExists() {
        if (!listAccounts().isEmpty()) return;
        String username = "admin";
        String password = Passwords.generate(20);
        String secret = Totp.generateSecret();
        saveAccount(username, password, secret);
        System.out.println("""
                
                ===============================================================
                 An admin account for the web interface was created.
                 user:     %s
                 password: %s
                 2FA:      %s
                 Add it to Google Authenticator with this link:
                 %s
                 This password is shown once - write it down now.
                ===============================================================
                """.formatted(username, password, secret, totp.toUri(issuer, username, secret)));
    }

    /**
     * @param username the account the code belongs to
     * @param secret   the base32 secret
     * @return the link an authenticator app can scan
     */
    public String toAuthenticatorUri(String username, String secret) {
        return totp.toUri(issuer, username, secret);
    }

    public Totp getTotp() {
        return totp;
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private YamlConfiguration config() {
        return configuration.getConfig();
    }
}
