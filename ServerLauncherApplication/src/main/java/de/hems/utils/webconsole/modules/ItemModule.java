package de.hems.utils.webconsole.modules;

import de.hems.types.item.ItemCatalog;
import de.hems.utils.item.ItemCatalogStore;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * What the item editor on the website builds items from: the materials, enchantments and attributes a game
 * server knows. Not a panel of its own - every panel that makes items asks here.
 */
public class ItemModule implements WebModule {

    @Override
    public String getId() {
        return "items";
    }

    @Override
    public String getTitle() {
        return "Items";
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/items/catalog", this::catalog);
        server.get("/api/materials", this::materials);
    }

    /**
     * The whole catalog, plus where it came from - the editor says so when it is only the copy from the last
     * time a server ran, or when there is none at all.
     */
    private void catalog(ApiContext ctx) {
        ItemCatalogStore store = ItemCatalogStore.get();
        ItemCatalog catalog = store.catalog();
        String source = catalog.isEmpty() ? "none" : store.isLive() ? "live" : "stored";
        ctx.ok(catalog.toJson().put("ok", true).put("source", source));
    }

    /**
     * Only the material names, optionally filtered - what the item editor used before the catalog.
     */
    private void materials(ApiContext ctx) {
        String filter = ctx.queryParam("q");
        String needle = filter == null ? "" : filter.trim().toUpperCase(Locale.ROOT);
        JSONArray array = new JSONArray();
        for (String name : ItemCatalogStore.get().catalog().materialNames()) {
            if (!needle.isEmpty() && !name.contains(needle)) continue;
            array.put(new JSONObject().put("name", name));
        }
        ctx.ok("materials", array);
    }
}
