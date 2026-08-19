package de.hems.utils.webconsole.modules;

import de.hems.types.admin.ItemData;
import de.hems.types.admin.StashData;
import de.hems.utils.admin.StashStore;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The admin stash, seen from the browser.
 * <p>
 * This is where the item management drops things. Pulling an item out of a player's inventory has to put it
 * somewhere reachable, and this container is also the chest {@code /admin} opens in game - so an item
 * dragged out of a player in the browser can be picked up a moment later on the server.
 * <p>
 * The launcher stores the contents as the bytes bukkit produced and never looks inside them. The browser
 * gets the same readable description of each item that the player inventories use, which the game server
 * built when it read them - so the stash is described by whoever last put something in it.
 */
public class StashModule implements WebModule {

    private final StashStore stashes;

    public StashModule(StashStore stashes) {
        this.stashes = stashes;
    }

    @Override
    public String getId() {
        return "stash";
    }

    @Override
    public String getTitle() {
        return "Admin-Ablage";
    }

    @Override
    public String getDescription() {
        return "Die Kiste, in die Items gezogen werden. Im Spiel mit /admin zu öffnen.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/stash", this::read);
        server.post("/api/stash", this::write);
    }

    private void read(ApiContext ctx) {
        StashData stash = stashes.get(StashData.GLOBAL);
        ctx.ok(toJson(stash));
    }

    /**
     * @param stash the stash as it is stored
     * @return it as json, with the raw item bytes carried along so nothing is lost on the way back
     */
    private static JSONObject toJson(StashData stash) {
        JSONArray items = new JSONArray();
        for (ItemData item : stash.getItems()) {
            items.put(new JSONObject()
                    .put("slot", item.getSlot())
                    .put("material", item.getMaterial())
                    .put("amount", item.getAmount())
                    .put("raw", item.getRawBase64() == null ? JSONObject.NULL : item.getRawBase64()));
        }
        return new JSONObject()
                .put("id", stash.getId())
                .put("size", stash.getSize())
                .put("revision", stash.getRevision())
                .put("items", items);
    }

    private void write(ApiContext ctx) {
        JSONObject body = ctx.body();
        int size = body.optInt("size", 54);
        long revision = body.optLong("revision", -1L);
        if (revision < 0L) {
            ctx.error(400, "Es fehlt die Revision - bitte die Ablage neu laden.");
            return;
        }
        StashData stash = new StashData(StashData.GLOBAL, size,
                readItems(body.optJSONArray("items"), size), revision);
        StashStore.Result result = stashes.put(stash);
        if (!result.successful()) {
            ctx.json(new JSONObject()
                    .put("ok", false)
                    .put("error", result.message())
                    .put("revision", result.revision()), 409);
            return;
        }
        ctx.ok(new JSONObject()
                .put("ok", true)
                .put("message", result.message())
                .put("revision", result.revision()));
    }

    /**
     * Reads the slots the browser sent back.
     *
     * @param raw  the items as json
     * @param size how many slots the stash has
     * @return the items, skipping empty and out of range slots
     */
    private static List<ItemData> readItems(JSONArray raw, int size) {
        List<ItemData> items = new ArrayList<>();
        if (raw == null) return items;
        for (int i = 0; i < raw.length(); i++) {
            JSONObject entry = raw.optJSONObject(i);
            if (entry == null) continue;
            String material = entry.optString("material", "");
            int slot = entry.optInt("slot", -1);
            if (material.isBlank() || "AIR".equalsIgnoreCase(material) || slot < 0 || slot >= size) continue;
            ItemData item = new ItemData();
            item.setSlot(slot);
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
        return items;
    }
}
