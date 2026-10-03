package de.hems.utils.item;

import de.hems.types.item.ItemCatalog;
import de.hems.types.item.ItemSpec;
import org.json.JSONObject;

import java.util.function.Supplier;

/**
 * Reads an item the website's item editor sent and checks it, the same way wherever items are made: an
 * event prize, a slot in a player's inventory, the admin stash.
 * <p>
 * The form is checked first - names that cannot be names, levels out of range, too much lore - and then the
 * names against the catalog of a game server, when there is one. Without a catalog a name is only checked
 * for its form; a game server leaves out what it does not know when it builds the item.
 */
public final class ItemForm {

    private ItemForm() {
    }

    /**
     * What reading an item came to.
     *
     * @param item    the item, {@code null} when there was a problem or nothing was sent
     * @param problem what is wrong, in words for the admin, {@code null} when nothing is
     */
    public record Result(ItemSpec item, String problem) {

        public boolean ok() {
            return problem == null;
        }
    }

    /**
     * @param json      the item as the editor sent it
     * @param maxAmount the most of it there may be
     * @param catalog   where the names are checked against - only asked when it is needed, because asking
     *                  can take a moment
     * @return the item, or what is wrong with it
     */
    public static Result read(JSONObject json, int maxAmount, Supplier<ItemCatalog> catalog) {
        if (json == null) return new Result(null, "ein Item kann nicht gelesen werden.");
        ItemSpec item = ItemSpec.fromJson(json);
        String problem = item.problem(maxAmount);
        if (problem != null) return new Result(null, problem);
        ItemCatalog known = catalog == null ? null : catalog.get();
        if (known != null) {
            problem = known.unknownName(item);
            if (problem != null) return new Result(null, problem);
            ItemCatalog.Material material = known.material(item.getMaterial());
            // wear beyond what the item can take would break it the moment it is used
            if (material != null && item.getDamage() > 0 && item.getDamage() >= Math.max(1, material.maxDurability())) {
                return new Result(null, material.maxDurability() == 0
                        ? item.getMaterial() + " nutzt sich nicht ab."
                        : "der Verschleiß von " + item.getMaterial() + " muss unter " + material.maxDurability()
                        + " liegen.");
            }
        }
        return new Result(item, null);
    }
}
