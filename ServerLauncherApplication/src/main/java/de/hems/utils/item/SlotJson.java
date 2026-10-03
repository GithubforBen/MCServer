package de.hems.utils.item;

import de.hems.types.admin.ItemData;
import de.hems.types.item.ItemSpec;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The slots of a container as the browser sees them and sends them back - the same for a player's inventory
 * and the admin stash, so the item editor works on both alike.
 * <p>
 * An item travels with the bytes bukkit made of it and with its description. The browser marks an item it
 * edited; only then is the description put onto the item, and only then is it checked. An item it merely
 * moved goes back as the bytes it came as.
 */
public final class SlotJson {

    /** The most of one item a slot can hold - minecraft itself stops at 99. */
    public static final int MAX_STACK = 99;

    private SlotJson() {
    }

    /**
     * @param item one slot
     * @return it as json
     */
    public static JSONObject toJson(ItemData item) {
        return new JSONObject()
                .put("slot", item.getSlot())
                .put("material", item.getMaterial())
                .put("amount", item.getAmount())
                .put("displayName", item.getDisplayName() == null ? JSONObject.NULL : item.getDisplayName())
                .put("lore", item.getLore() == null ? new JSONArray() : new JSONArray(item.getLore()))
                .put("enchantments", item.getEnchantments() == null
                        ? new JSONArray() : new JSONArray(item.getEnchantments()))
                .put("damage", item.getDamage())
                .put("maxDurability", item.getMaxDurability())
                .put("raw", item.getRawBase64() == null ? JSONObject.NULL : item.getRawBase64())
                .put("spec", item.getSpec() == null ? JSONObject.NULL : item.getSpec().toJson())
                .put("modified", item.isModified());
    }

    /**
     * What reading the slots came to.
     *
     * @param items   the items, empty slots left out
     * @param problem what is wrong, {@code null} when nothing is
     */
    public record Result(List<ItemData> items, String problem) {
    }

    /**
     * @param array the slots as the browser sent them
     * @param size  how many slots the container has, slots outside are dropped
     * @return the items, or the first thing wrong with them
     */
    public static Result read(JSONArray array, int size) {
        List<ItemData> items = new ArrayList<>();
        if (array == null) return new Result(items, null);
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.optJSONObject(i);
            if (entry == null) continue;
            int slot = entry.optInt("slot", -1);
            if (slot < 0 || slot >= size) continue;
            ItemData item = new ItemData();
            item.setSlot(slot);

            JSONObject spec = entry.optJSONObject("spec");
            if (entry.optBoolean("modified", false) && spec != null) {
                ItemForm.Result read = ItemForm.read(spec, MAX_STACK, () -> ItemCatalogStore.get().catalog());
                if (!read.ok()) return new Result(items, "Slot " + slot + ": " + read.problem());
                item.setSpec(read.item());
                item.setModified(true);
                item.setMaterial(read.item().getMaterial());
                item.setAmount(read.item().getAmount());
            } else {
                String material = entry.optString("material", "");
                if (material.isBlank() || "AIR".equalsIgnoreCase(material)) continue;
                item.setMaterial(ItemSpec.normaliseMaterial(material));
                item.setAmount(Math.max(1, Math.min(MAX_STACK, entry.optInt("amount", 1))));
                // kept for showing it, never put onto the item - the bytes are what counts for this one
                if (spec != null) {
                    ItemSpec shown = ItemSpec.fromJson(spec);
                    shown.setAmount(item.getAmount());
                    item.setSpec(shown);
                }
            }
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
        return new Result(items, null);
    }
}
