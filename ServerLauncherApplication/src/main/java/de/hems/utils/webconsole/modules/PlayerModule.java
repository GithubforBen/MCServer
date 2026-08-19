package de.hems.utils.webconsole.modules;

import de.hems.communication.events.admin.RequestPlayerActionEvent;
import de.hems.types.admin.InventoryData;
import de.hems.types.admin.ItemData;
import de.hems.types.admin.PlayerSnapshot;
import de.hems.utils.webconsole.AdminNetwork;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * The player manager: who is online, what they carry and what can be done to them.
 * <p>
 * Nothing here touches a game server directly - every question goes through {@link AdminNetwork} over the
 * event bus, because the website runs in the launcher and inventories only exist inside the paper servers.
 */
public class PlayerModule implements WebModule {

    /** The slots a player inventory has: 36 main, 4 armour, 1 off hand. */
    private static final int PLAYER_INVENTORY_SIZE = 41;

    @Override
    public String getId() {
        return "players";
    }

    @Override
    public String getTitle() {
        return "Spieler";
    }

    @Override
    public String getDescription() {
        return "Zeigt wer online ist, öffnet Inventar, Enderchest und Backpacks und greift ein.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/players", this::list);
        server.get("/api/players/{uuid}", this::detail);
        server.get("/api/players/{uuid}/inventory", this::readInventory);
        server.post("/api/players/{uuid}/inventory", this::writeInventory);
        server.post("/api/players/{uuid}/action", this::action);
        server.get("/api/materials", this::materials);
    }

    /* ------------------------------------------------------------------ list */

    private void list(ApiContext ctx) throws Exception {
        if (!AdminNetwork.isAvailable()) {
            ctx.error(503, "Das Netzwerk ist noch nicht verbunden.");
            return;
        }
        Set<UUID> paying = payingPlayers(ctx);
        AdminNetwork.Network network = AdminNetwork.network();
        JSONArray array = new JSONArray();
        for (PlayerSnapshot player : network.players()) {
            player.setPaying(player.getUuid() != null && paying.contains(player.getUuid()));
            array.put(toJson(player));
        }
        // the load of every server rides along, so the overview needs no second round trip
        JSONArray servers = new JSONArray();
        for (AdminNetwork.ServerLoad server : network.servers()) {
            servers.put(new JSONObject()
                    .put("name", server.name())
                    .put("tps", Math.round(server.tps() * 10.0d) / 10.0d)
                    .put("players", server.players()));
        }
        ctx.ok(new JSONObject().put("players", array).put("servers", servers));
    }

    private void detail(ApiContext ctx) throws Exception {
        UUID uuid = parseUuid(ctx);
        if (uuid == null) return;
        Set<UUID> paying = payingPlayers(ctx);
        for (PlayerSnapshot player : AdminNetwork.players()) {
            if (!uuid.equals(player.getUuid())) continue;
            player.setPaying(paying.contains(uuid));
            ctx.ok(toJson(player));
            return;
        }
        ctx.error(404, "Dieser Spieler ist gerade auf keinem Server online.");
    }

    /**
     * @param player the snapshot to describe
     * @return what the browser needs to draw a player row and the detail view
     */
    private static JSONObject toJson(PlayerSnapshot player) {
        JSONArray backpacks = new JSONArray();
        for (PlayerSnapshot.BackpackInfo backpack : player.getBackpacks()) {
            backpacks.put(new JSONObject()
                    .put("id", backpack.id())
                    .put("title", backpack.title())
                    .put("size", backpack.size()));
        }
        return new JSONObject()
                .put("uuid", String.valueOf(player.getUuid()))
                .put("name", player.getName())
                .put("server", player.getServer())
                .put("online", player.isOnline())
                .put("health", player.getHealth())
                .put("maxHealth", player.getMaxHealth())
                .put("foodLevel", player.getFoodLevel())
                .put("gameMode", player.getGameMode())
                .put("level", player.getLevel())
                .put("world", player.getWorld())
                .put("x", player.getX())
                .put("y", player.getY())
                .put("z", player.getZ())
                .put("firstPlayed", player.getFirstPlayed())
                .put("op", player.isOp())
                .put("banned", player.isBanned())
                .put("viewDistance", player.getViewDistance())
                .put("paying", player.isPaying())
                .put("backpacks", backpacks);
    }

    /* ------------------------------------------------------------------ inventories */

    private void readInventory(ApiContext ctx) throws Exception {
        UUID uuid = parseUuid(ctx);
        if (uuid == null) return;
        InventoryData.Kind kind = parseKind(ctx.queryParam("kind"));
        String container = ctx.queryParam("container");

        InventoryData inventory = AdminNetwork.inventory(uuid, kind, container);
        if (inventory == null) {
            ctx.error(404, kind == InventoryData.Kind.BACKPACK
                    ? "Dieses Backpack gibt es nicht, oder es ist kein Backpack-System installiert."
                    : "Der Spieler ist nicht online. Inventare gibt es nur von Spielern, die gerade spielen.");
            return;
        }
        ctx.ok(inventoryToJson(inventory));
    }

    /**
     * @param inventory the container that was read
     * @return it as json, with the raw bytes carried along so an untouched item survives the round trip
     */
    private static JSONObject inventoryToJson(InventoryData inventory) {
        JSONArray items = new JSONArray();
        for (ItemData item : inventory.getItems()) {
            items.put(new JSONObject()
                    .put("slot", item.getSlot())
                    .put("material", item.getMaterial())
                    .put("amount", item.getAmount())
                    .put("displayName", item.getDisplayName() == null ? JSONObject.NULL : item.getDisplayName())
                    .put("lore", item.getLore() == null ? new JSONArray() : new JSONArray(item.getLore()))
                    .put("enchantments", item.getEnchantments() == null
                            ? new JSONArray() : new JSONArray(item.getEnchantments()))
                    .put("damage", item.getDamage())
                    .put("maxDurability", item.getMaxDurability())
                    .put("raw", item.getRawBase64() == null ? JSONObject.NULL : item.getRawBase64()));
        }
        return new JSONObject()
                .put("playerId", String.valueOf(inventory.getPlayerId()))
                .put("playerName", inventory.getPlayerName())
                .put("kind", inventory.getKind().name())
                .put("containerId", inventory.getContainerId() == null
                        ? JSONObject.NULL : inventory.getContainerId())
                .put("title", inventory.getContainerTitle())
                .put("size", inventory.getSize())
                .put("items", items);
    }

    private void writeInventory(ApiContext ctx) throws Exception {
        UUID uuid = parseUuid(ctx);
        if (uuid == null) return;
        JSONObject body = ctx.body();
        InventoryData.Kind kind = parseKind(body.optString("kind", null));
        String container = body.optString("containerId", null);
        int size = body.optInt("size", kind == InventoryData.Kind.ENDER_CHEST ? 27 : PLAYER_INVENTORY_SIZE);

        List<ItemData> items = new ArrayList<>();
        JSONArray raw = body.optJSONArray("items");
        if (raw != null) {
            for (int i = 0; i < raw.length(); i++) {
                JSONObject entry = raw.optJSONObject(i);
                if (entry == null) continue;
                String material = entry.optString("material", "");
                if (material.isBlank() || "AIR".equalsIgnoreCase(material)) continue;
                ItemData item = new ItemData();
                item.setSlot(entry.optInt("slot", -1));
                item.setMaterial(material.toUpperCase(Locale.ROOT));
                item.setAmount(Math.max(1, entry.optInt("amount", 1)));
                String base64 = entry.optString("raw", null);
                if (base64 != null && !base64.isBlank() && !"null".equals(base64)) {
                    try {
                        item.setRawBase64(base64);
                    } catch (IllegalArgumentException e) {
                        // a mangled payload just means the item is rebuilt plain instead of restored
                    }
                }
                items.add(item);
            }
        }

        InventoryData inventory = new InventoryData(uuid, null, kind, container, null, size, items);
        String editor = ctx.session() == null ? "web" : ctx.session().getUsername();
        String message = AdminNetwork.applyInventory(inventory, editor);
        if (message == null) {
            ctx.error(409, "Konnte nicht gespeichert werden - ist der Spieler noch online?");
            return;
        }
        ctx.ok(message);
    }

    /* ------------------------------------------------------------------ actions */

    private void action(ApiContext ctx) throws Exception {
        UUID uuid = parseUuid(ctx);
        if (uuid == null) return;
        RequestPlayerActionEvent.Action action;
        try {
            action = RequestPlayerActionEvent.Action.valueOf(
                    ctx.string("action", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "Unbekannte Aktion.");
            return;
        }
        String editor = ctx.session() == null ? "web" : ctx.session().getUsername();
        String message = AdminNetwork.action(uuid, action, ctx.string("argument", null), editor);
        if (message == null) {
            ctx.error(409, "Die Aktion hat nicht funktioniert - ist der Spieler noch online?");
            return;
        }
        ctx.ok(message);
    }

    /**
     * The materials the item editor offers.
     * <p>
     * Fetched from a game server rather than read from this jvm's own paper api: bukkit's registry is only
     * filled inside a running server, so {@code Material.values()} here throws instead of answering.
     */
    private void materials(ApiContext ctx) throws Exception {
        String filter = ctx.queryParam("q");
        String needle = filter == null ? "" : filter.trim().toUpperCase(Locale.ROOT);
        JSONArray array = new JSONArray();
        for (String name : AdminNetwork.materials()) {
            if (!needle.isEmpty() && !name.contains(needle)) continue;
            array.put(new JSONObject().put("name", name));
        }
        ctx.ok("materials", array);
    }

    /* ------------------------------------------------------------------ helpers */

    /**
     * @param ctx the request to read the uuid from
     * @return the uuid, or {@code null} after an error was already answered
     */
    private static UUID parseUuid(ApiContext ctx) {
        try {
            return UUID.fromString(ctx.pathParam("uuid"));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "Das ist keine gültige UUID.");
            return null;
        }
    }

    private static InventoryData.Kind parseKind(String raw) {
        if (raw == null || raw.isBlank()) return InventoryData.Kind.INVENTORY;
        try {
            return InventoryData.Kind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return InventoryData.Kind.INVENTORY;
        }
    }

    /**
     * @param ctx the request, which carries the server and therefore the config
     * @return the players that pay, straight from the launcher config the paying player panel writes
     */
    private static Set<UUID> payingPlayers(ApiContext ctx) {
        Set<UUID> paying = new LinkedHashSet<>();
        for (String entry : ctx.server().getConfiguration().getConfig()
                .getStringList("paying-players")) {
            try {
                paying.add(UUID.fromString(entry.trim()));
            } catch (IllegalArgumentException ignored) {
                // the paying player panel drops these too
            }
        }
        return paying;
    }
}
