package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import de.hems.utils.whitelist.WhitelistStore;
import de.hems.utils.whitelist.WhitelistSync;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The rules page, where players put themselves on the whitelist, and the panel where the admins write the
 * rules and see who accepted them.
 * <p>
 * The page needs no login - that is the point of it - so what it accepts is narrow: a name mojang knows, a
 * tick in the box, and one try every few seconds per address.
 */
public class WhitelistModule implements WebModule {

    private static final String MINECRAFT_NAME = "[A-Za-z0-9_]{3,16}";
    private static final long COOLDOWN_MS = 10_000L;
    private static final int MAX_RULES = 20_000;

    /** When each address may try again. */
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "whitelist";
    }

    @Override
    public String getTitle() {
        return "Whitelist";
    }

    @Override
    public String getDescription() {
        return "Regeln, und wer sich durch Akzeptieren selbst auf die Whitelist gesetzt hat.";
    }

    @Override
    public void register(WebServer server) {
        server.publicGet("/regeln", ctx -> ctx.raw().redirect("/regeln.html"));
        server.publicGet("/api/public/rules", this::publicRules);
        server.publicPost("/api/public/whitelist", this::join);
        server.get("/api/whitelist", this::overview);
        server.post("/api/whitelist/settings", this::saveSettings);
        server.delete("/api/whitelist/{uuid}", this::remove);
    }

    /* ------------------------------------------------------------------ the public page */

    private void publicRules(ApiContext ctx) {
        WhitelistStore store = store();
        ctx.ok(new JSONObject()
                .put("brand", ctx.server().getConfiguration().getConfig().getString("web.brand", "MCServer"))
                .put("selfService", store.isSelfService())
                .put("rules", store.getRules()));
    }

    private void join(ApiContext ctx) {
        // a plain form on another site can not send json, so this keeps other pages from filling the list
        String type = ctx.header("Content-Type");
        if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            ctx.error(415, "Bitte das Formular auf der Seite benutzen.");
            return;
        }
        WhitelistStore store = store();
        if (!store.isSelfService()) {
            ctx.error(403, "Selbst eintragen ist gerade ausgeschaltet. Bitte wende dich an einen Admin.");
            return;
        }
        if (!ctx.body().optBoolean("accepted", false)) {
            ctx.error(400, "Du musst die Regeln akzeptieren.");
            return;
        }
        String name = ctx.string("name", "");
        if (!name.matches(MINECRAFT_NAME)) {
            ctx.error(400, "Ein Minecraft-Name hat 3 bis 16 Zeichen: Buchstaben, Ziffern und _.");
            return;
        }
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(ctx.clientKey());
        if (until != null && until > now) {
            ctx.error(429, "Bitte warte noch " + ((until - now + 999L) / 1000L) + " Sekunden.");
            return;
        }
        cooldowns.values().removeIf(time -> time <= now);
        cooldowns.put(ctx.clientKey(), now + COOLDOWN_MS);

        UUID uuid = UUIDFetcher.findUUIDByName(name, true);
        if (uuid == null) {
            ctx.error(404, "Mojang kennt keinen Account namens " + name + ". Tippfehler? "
                    + "(Oder Mojang ist gerade nicht erreichbar - dann bitte später noch einmal.)");
            return;
        }
        // mojang writes the name the way the player chose it, which is how it should show up everywhere
        String canonical = UUIDFetcher.findNameByUUID(uuid);
        if (canonical == null) canonical = name;

        boolean already = store.contains(uuid);
        store.add(uuid, canonical);
        if (!already) {
            WhitelistSync.add(uuid, canonical);
            System.out.println("Whitelist: " + canonical + " (" + uuid + ") accepted the rules and is on the whitelist.");
        }
        ctx.ok(new JSONObject()
                .put("ok", true)
                .put("name", canonical)
                .put("message", already
                        ? canonical + " steht schon auf der Whitelist. Die Regeln sind als akzeptiert vermerkt."
                        : "Willkommen, " + canonical + "! Du stehst jetzt auf der Whitelist und kannst sofort joinen."));
    }

    /* ------------------------------------------------------------------ the admin panel */

    private void overview(ApiContext ctx) {
        WhitelistStore store = store();
        String current = store.rulesHash();
        JSONArray players = new JSONArray();
        for (WhitelistStore.Entry entry : store.list()) {
            players.put(new JSONObject()
                    .put("uuid", entry.uuid().toString())
                    .put("name", entry.name())
                    .put("acceptedAt", entry.acceptedAt())
                    .put("currentRules", current.equals(entry.rules())));
        }
        ctx.ok(new JSONObject()
                .put("enforced", store.isEnforced())
                .put("selfService", store.isSelfService())
                .put("rules", store.getRules())
                .put("publicUrl", store.getPublicUrl() == null ? "" : store.getPublicUrl())
                .put("rulesUrl", WhitelistSync.rulesUrl())
                .put("adminNames", new JSONArray(ctx.server().getConfiguration().getConfig().getStringList("whitelist")))
                .put("players", players));
    }

    private void saveSettings(ApiContext ctx) {
        WhitelistStore store = store();
        JSONObject body = ctx.body();
        String rules = body.optString("rules", store.getRules()).strip();
        if (rules.isEmpty() || rules.length() > MAX_RULES) {
            ctx.error(400, "Die Regeln brauchen 1 bis " + MAX_RULES + " Zeichen.");
            return;
        }
        String url = body.optString("publicUrl", "").trim();
        if (!url.isEmpty() && !url.matches("https?://\\S+")) {
            ctx.error(400, "Der Link muss mit http:// oder https:// anfangen.");
            return;
        }
        boolean enforced = body.optBoolean("enforced", store.isEnforced());
        boolean changedEnforcement = enforced != store.isEnforced();

        store.setRules(rules);
        store.setPublicUrl(url);
        store.setSelfService(body.optBoolean("selfService", store.isSelfService()));
        store.setEnforced(enforced);
        if (changedEnforcement) {
            WhitelistSync.setEnforced(enforced);
            System.out.println("Whitelist: " + ctx.session().getUsername() + " switched enforcement "
                    + (enforced ? "on" : "off") + ".");
        }
        ctx.ok(changedEnforcement && enforced
                ? "Gespeichert. Die Whitelist gilt ab jetzt auf allen laufenden Servern."
                : "Gespeichert. Der Link in der Kick-Nachricht gilt ab dem nächsten Start eines Servers.");
    }

    private void remove(ApiContext ctx) {
        UUID uuid;
        try {
            uuid = UUID.fromString(ctx.pathParam("uuid"));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "Das ist keine UUID.");
            return;
        }
        WhitelistStore.Entry entry = store().remove(uuid);
        if (entry == null) {
            ctx.error(404, "Der Spieler steht nicht auf der Liste.");
            return;
        }
        WhitelistSync.remove(uuid, entry.name());
        System.out.println("Whitelist: " + ctx.session().getUsername() + " removed " + entry.name() + ".");
        ctx.ok(entry.name() + " steht nicht mehr auf der Whitelist.");
    }

    private static WhitelistStore store() {
        return Main.getInstance().getWhitelistStore();
    }
}
