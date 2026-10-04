package de.hems.types.item;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything an item can be made of on a running server: its materials, enchantments and attributes.
 * <p>
 * Built by a game server, because only there are bukkit's registries filled, and kept by the launcher so the
 * item editor on the website can make suggestions and the launcher can check what was typed - also while
 * no game server happens to be running.
 */
public class ItemCatalog implements Serializable {

    private static final long serialVersionUID = 5200L;

    /**
     * One material that can be an item.
     *
     * @param name          as bukkit names it
     * @param block         whether it can also be placed as a block
     * @param maxStack      how many fit into one slot
     * @param maxDurability how much wear it takes, zero if it takes none
     */
    public record Material(String name, boolean block, int maxStack, int maxDurability) implements Serializable {
    }

    /**
     * One enchantment.
     *
     * @param key       its namespaced key
     * @param maxLevel  the highest level an enchanting table gives
     * @param materials the materials it normally goes on - anything else is still allowed, it just is not
     *                  suggested first
     */
    public record Enchantment(String key, int maxLevel, List<String> materials) implements Serializable {
    }

    private final List<Material> materials;
    private final List<Enchantment> enchantments;
    private final List<String> attributes;
    /** When it was built, in milliseconds. */
    private final long builtAt;

    public ItemCatalog(List<Material> materials, List<Enchantment> enchantments, List<String> attributes,
                       long builtAt) {
        this.materials = materials == null ? List.of() : List.copyOf(materials);
        this.enchantments = enchantments == null ? List.of() : List.copyOf(enchantments);
        this.attributes = attributes == null ? List.of() : List.copyOf(attributes);
        this.builtAt = builtAt;
    }

    /**
     * @return a catalog that knows nothing - every name is then only checked for its form
     */
    public static ItemCatalog empty() {
        return new ItemCatalog(List.of(), List.of(), List.of(), 0L);
    }

    public List<Material> getMaterials() {
        return materials;
    }

    public List<Enchantment> getEnchantments() {
        return enchantments;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public long getBuiltAt() {
        return builtAt;
    }

    public boolean isEmpty() {
        return materials.isEmpty();
    }

    /**
     * @return the names of every material, in order
     */
    public List<String> materialNames() {
        List<String> names = new ArrayList<>(materials.size());
        for (Material material : materials) names.add(material.name());
        return names;
    }

    /**
     * @param name a material name as bukkit spells it
     * @return what is known about it, or {@code null}
     */
    public Material material(String name) {
        for (Material material : materials) {
            if (material.name().equals(name)) return material;
        }
        return null;
    }

    /**
     * @return the keys of every enchantment
     */
    public Set<String> enchantmentKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Enchantment enchantment : enchantments) keys.add(enchantment.key());
        return keys;
    }

    /**
     * Checks the names an item uses against this catalog. An empty catalog accepts every name, because then
     * there is nothing to check against - a game server skips what it does not know when it builds the item.
     *
     * @param spec the item
     * @return what is wrong, or {@code null}
     */
    public String unknownName(ItemSpec spec) {
        if (isEmpty()) return null;
        if (material(spec.getMaterial()) == null) return "'" + spec.getMaterial() + "' ist kein Minecraft-Item.";
        if (!enchantments.isEmpty()) {
            Set<String> known = enchantmentKeys();
            for (String key : spec.getEnchantments().keySet()) {
                if (!known.contains(key)) return "'" + key + "' ist keine Verzauberung.";
            }
        }
        if (!attributes.isEmpty()) {
            for (ItemSpec.Modifier modifier : spec.getAttributes()) {
                if (!attributes.contains(modifier.getAttribute())) {
                    return "'" + modifier.getAttribute() + "' ist kein Attribut.";
                }
            }
        }
        return null;
    }

    /* -------------------------------------------------------------------------------------- json */

    /**
     * @return the catalog as json, the form the website reads and the launcher keeps on disk
     */
    public JSONObject toJson() {
        JSONArray materialArray = new JSONArray();
        for (Material material : materials) {
            materialArray.put(new JSONObject()
                    .put("name", material.name())
                    .put("block", material.block())
                    .put("maxStack", material.maxStack())
                    .put("maxDurability", material.maxDurability()));
        }
        JSONArray enchantmentArray = new JSONArray();
        for (Enchantment enchantment : enchantments) {
            enchantmentArray.put(new JSONObject()
                    .put("key", enchantment.key())
                    .put("maxLevel", enchantment.maxLevel())
                    .put("materials", new JSONArray(enchantment.materials())));
        }
        return new JSONObject()
                .put("materials", materialArray)
                .put("enchantments", enchantmentArray)
                .put("attributes", new JSONArray(attributes))
                .put("builtAt", builtAt);
    }

    /**
     * @param json a catalog as {@link #toJson()} wrote it
     * @return the catalog, empty if the json is unusable
     */
    public static ItemCatalog fromJson(JSONObject json) {
        if (json == null) return empty();
        List<Material> materials = new ArrayList<>();
        JSONArray materialArray = json.optJSONArray("materials");
        if (materialArray != null) {
            for (int i = 0; i < materialArray.length(); i++) {
                JSONObject entry = materialArray.optJSONObject(i);
                if (entry == null || entry.optString("name", "").isBlank()) continue;
                materials.add(new Material(entry.getString("name"), entry.optBoolean("block", false),
                        entry.optInt("maxStack", 64), entry.optInt("maxDurability", 0)));
            }
        }
        List<Enchantment> enchantments = new ArrayList<>();
        JSONArray enchantmentArray = json.optJSONArray("enchantments");
        if (enchantmentArray != null) {
            for (int i = 0; i < enchantmentArray.length(); i++) {
                JSONObject entry = enchantmentArray.optJSONObject(i);
                if (entry == null || entry.optString("key", "").isBlank()) continue;
                List<String> on = new ArrayList<>();
                JSONArray onArray = entry.optJSONArray("materials");
                if (onArray != null) {
                    for (int j = 0; j < onArray.length(); j++) on.add(onArray.optString(j));
                }
                enchantments.add(new Enchantment(entry.getString("key"), entry.optInt("maxLevel", 1), on));
            }
        }
        List<String> attributes = new ArrayList<>();
        JSONArray attributeArray = json.optJSONArray("attributes");
        if (attributeArray != null) {
            for (int i = 0; i < attributeArray.length(); i++) attributes.add(attributeArray.optString(i));
        }
        return new ItemCatalog(materials, enchantments, attributes, json.optLong("builtAt", 0L));
    }
}
