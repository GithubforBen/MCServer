package de.hems.utils.webconsole.modules;

import de.hems.types.admin.ItemData;
import de.hems.types.admin.StashData;
import de.hems.utils.admin.StashStore;
import de.hems.utils.item.SlotJson;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;


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
        for (ItemData item : stash.getItems()) items.put(SlotJson.toJson(item));
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
        SlotJson.Result read = SlotJson.read(body.optJSONArray("items"), size);
        if (read.problem() != null) {
            ctx.error(400, read.problem());
            return;
        }
        StashData stash = new StashData(StashData.GLOBAL, size, read.items(), revision);
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
}
