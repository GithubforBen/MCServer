package de.hems.utils.webconsole.modules;

import de.hems.utils.QrCode;
import de.hems.utils.bot.verification.DiscordOwner;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import de.hems.utils.webconsole.auth.AdminAccount;
import de.hems.utils.webconsole.auth.AuthService;
import de.hems.utils.webconsole.auth.Passwords;
import de.hems.utils.webconsole.auth.Totp;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The settings of the network and the accounts of this website - what {@code install.sh} asks for, so it can
 * be changed later without a shell on the machine.
 * <p>
 * Anything that could lock somebody out or let somebody in - a new password, a new authenticator secret, an
 * account more or less - needs the password of the admin doing it on top of the session. A session cookie
 * is easier to lose than a password.
 */
public class SettingsModule implements WebModule {

    private static final String NAME_PATTERN = "[A-Za-z0-9_.-]{1,32}";
    private static final String MINECRAFT_NAME = "[A-Za-z0-9_]{1,16}";
    private static final int MIN_PASSWORD = 12;

    /**
     * The authenticator secret a session is switching to. It only replaces the old one once a code from it
     * has been typed in, so a phone that did not scan it properly can not lock the account.
     */
    private final Map<String, String> pendingSecrets = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "settings";
    }

    @Override
    public String getTitle() {
        return "Einstellungen";
    }

    @Override
    public String getDescription() {
        return "Eigener Account, Admin-Accounts und die Einstellungen des Netzwerks.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/settings", this::settings);
        server.post("/api/settings", this::saveSettings);
        server.post("/api/account/password", this::changePassword);
        server.post("/api/account/totp", this::startTotp);
        server.post("/api/account/totp/confirm", this::confirmTotp);
        server.post("/api/accounts", this::createAccount);
        server.delete("/api/accounts/{name}", this::deleteAccount);
    }

    /* ------------------------------------------------------------------ network settings */

    private void settings(ApiContext ctx) {
        YamlConfiguration config = config(ctx);
        AuthService auth = ctx.server().getAuthService();
        ctx.ok(new JSONObject()
                .put("username", ctx.session().getUsername())
                .put("accounts", new JSONArray(auth.listAccounts()))
                .put("owner", DiscordOwner.id())
                .put("brand", config.getString("web.brand", "MCServer"))
                .put("ops", new JSONArray(config.getStringList("ops")))
                .put("whitelist", new JSONArray(config.getStringList("whitelist")))
                .put("autostart", new JSONArray(config.getStringList("autostart"))));
    }

    private void saveSettings(ApiContext ctx) {
        JSONObject body = ctx.body();
        String owner = ctx.string("owner", DiscordOwner.id());
        if (!owner.matches("\\d{17,20}")) {
            ctx.error(400, "Eine Discord-ID sind 17 bis 20 Ziffern.");
            return;
        }
        String brand = ctx.string("brand", "MCServer");
        if (brand.isEmpty() || brand.length() > 40) {
            ctx.error(400, "Der Name der Seite braucht 1 bis 40 Zeichen.");
            return;
        }
        List<String> ops = names(body.optJSONArray("ops"), MINECRAFT_NAME, false);
        List<String> whitelist = names(body.optJSONArray("whitelist"), MINECRAFT_NAME, false);
        List<String> autostart = names(body.optJSONArray("autostart"), "[A-Za-z0-9_-]{1,32}", true);
        if (ops == null || whitelist == null) {
            ctx.error(400, "Minecraft-Namen bestehen aus 1 bis 16 Buchstaben, Ziffern und _.");
            return;
        }
        if (autostart == null) {
            ctx.error(400, "Servernamen bestehen aus Buchstaben, Ziffern, _ und -.");
            return;
        }

        YamlConfiguration config = config(ctx);
        config.set(DiscordOwner.CONFIG_KEY, owner);
        config.set("web.brand", brand);
        config.set("ops", ops);
        config.set("whitelist", whitelist);
        config.set("autostart", autostart);
        ctx.server().getConfiguration().save();
        ctx.ok("Gespeichert. Ops und Whitelist gelten ab dem nächsten Start eines Servers, "
                + "Autostart ab dem nächsten Neustart des Netzwerks.");
    }

    /**
     * @param array    what the browser sent
     * @param pattern  what every entry has to look like
     * @param upper    whether to write the entries in capitals, as server names are
     * @return the entries without blanks and duplicates, or {@code null} if one is not usable
     */
    private static List<String> names(JSONArray array, String pattern, boolean upper) {
        List<String> result = new ArrayList<>();
        if (array == null) return result;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (value.isEmpty()) continue;
            if (!value.matches(pattern)) return null;
            if (upper) value = value.toUpperCase(Locale.ROOT);
            if (!result.contains(value)) result.add(value);
        }
        return result;
    }

    /* ------------------------------------------------------------------ own account */

    private void changePassword(ApiContext ctx) {
        AuthService auth = ctx.server().getAuthService();
        String username = ctx.session().getUsername();
        String next = ctx.body().optString("next", "");
        if (next.length() < MIN_PASSWORD) {
            ctx.error(400, "Das neue Passwort braucht mindestens " + MIN_PASSWORD + " Zeichen.");
            return;
        }
        if (!auth.confirmPassword(username, ctx.body().optString("current", ""))) {
            ctx.error(403, "Das aktuelle Passwort stimmt nicht.");
            return;
        }
        AdminAccount account = auth.findAccount(username);
        auth.saveAccount(account.getUsername(), next, account.getTotpSecret());
        // whoever else is logged in as this account did so with the old password
        auth.endSessions(username, ctx.session().getId());
        ctx.ok("Passwort geändert. Andere Sitzungen dieses Accounts wurden abgemeldet.");
    }

    /**
     * Makes a new authenticator secret and shows it, without switching to it yet.
     */
    private void startTotp(ApiContext ctx) {
        AuthService auth = ctx.server().getAuthService();
        String username = ctx.session().getUsername();
        if (!auth.confirmPassword(username, ctx.body().optString("current", ""))) {
            ctx.error(403, "Das Passwort stimmt nicht.");
            return;
        }
        String secret = Totp.generateSecret();
        pendingSecrets.put(ctx.session().getId(), secret);
        ctx.ok(authenticator(auth, username, secret));
    }

    /**
     * Switches to the new secret once a code from it proves the phone has it.
     */
    private void confirmTotp(ApiContext ctx) {
        AuthService auth = ctx.server().getAuthService();
        String secret = pendingSecrets.get(ctx.session().getId());
        if (secret == null) {
            ctx.error(409, "Es wird gerade kein neuer Schlüssel eingerichtet.");
            return;
        }
        if (auth.getTotp().verify(secret, ctx.string("code", "")) < 0) {
            ctx.error(400, "Der Code passt nicht zum neuen Schlüssel. Der alte gilt weiter.");
            return;
        }
        pendingSecrets.remove(ctx.session().getId());
        AdminAccount account = auth.findAccount(ctx.session().getUsername());
        config(ctx).set("web.admins." + account.getUsername() + ".totp-secret", secret);
        ctx.server().getConfiguration().save();
        ctx.ok("Der neue Schlüssel gilt ab jetzt, der alte nicht mehr.");
    }

    /* ------------------------------------------------------------------ other accounts */

    private void createAccount(ApiContext ctx) {
        AuthService auth = ctx.server().getAuthService();
        String name = ctx.string("name", "").toLowerCase(Locale.ROOT);
        if (!name.matches(NAME_PATTERN)) {
            ctx.error(400, "Benutzernamen bestehen aus 1 bis 32 Buchstaben, Ziffern, _ . und -.");
            return;
        }
        if (auth.findAccount(name) != null) {
            ctx.error(409, "Den Account " + name + " gibt es schon.");
            return;
        }
        if (!auth.confirmPassword(ctx.session().getUsername(), ctx.body().optString("current", ""))) {
            ctx.error(403, "Dein Passwort stimmt nicht.");
            return;
        }
        String password = Passwords.generate(20);
        String secret = Totp.generateSecret();
        auth.saveAccount(name, password, secret);
        System.out.println("Web: " + ctx.session().getUsername() + " created the admin account " + name + ".");
        ctx.ok(authenticator(auth, name, secret)
                .put("username", name)
                .put("password", password)
                .put("message", "Account " + name + " angelegt."));
    }

    private void deleteAccount(ApiContext ctx) {
        AuthService auth = ctx.server().getAuthService();
        String name = ctx.pathParam("name");
        AdminAccount account = auth.findAccount(name);
        if (account == null) {
            ctx.error(404, "Den Account gibt es nicht.");
            return;
        }
        if (account.getUsername().equalsIgnoreCase(ctx.session().getUsername())) {
            ctx.error(409, "Den eigenen Account kann man nicht löschen.");
            return;
        }
        if (!auth.confirmPassword(ctx.session().getUsername(), ctx.body().optString("current", ""))) {
            ctx.error(403, "Dein Passwort stimmt nicht.");
            return;
        }
        auth.deleteAccount(account.getUsername());
        System.out.println("Web: " + ctx.session().getUsername() + " deleted the admin account "
                + account.getUsername() + ".");
        ctx.ok("Account " + account.getUsername() + " gelöscht und abgemeldet.");
    }

    /* ------------------------------------------------------------------ helpers */

    private static JSONObject authenticator(AuthService auth, String username, String secret) {
        String uri = auth.toAuthenticatorUri(username, secret);
        return new JSONObject()
                .put("secret", secret)
                .put("uri", uri)
                .put("qr", QrCode.svgDataUrl(uri));
    }

    private static YamlConfiguration config(ApiContext ctx) {
        return ctx.server().getConfiguration().getConfig();
    }
}
