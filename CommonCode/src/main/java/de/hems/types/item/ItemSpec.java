package de.hems.types.item;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * An item as somebody describes it: what it is, how many, and everything that makes it more than the plain
 * material - a name, lore, enchantments, attribute modifiers, unbreakable and wear.
 * <p>
 * This is the one shape items take wherever they are made by hand: an event prize, a slot edited on the
 * website, the admin stash. It knows nothing about bukkit on purpose, because the launcher stores and checks
 * these without a server to resolve them with - a game server turns them into real stacks when it needs them.
 * <p>
 * Names stay as namespaced keys ({@code minecraft:sharpness}, {@code minecraft:attack_damage}) rather than
 * enum constants, so an enchantment or attribute added by a newer minecraft needs no change here.
 */
public class ItemSpec implements Serializable {

    private static final long serialVersionUID = 5100L;

    /** Longest custom name, in characters including colour codes. */
    public static final int MAX_NAME = 120;
    /** Most lore lines. */
    public static final int MAX_LORE_LINES = 16;
    /** Longest lore line. */
    public static final int MAX_LORE_LINE = 120;
    /** Highest enchantment level minecraft can store. */
    public static final int MAX_ENCHANT_LEVEL = 255;
    /** Most enchantments on one item - far more than there are, but a bound nonetheless. */
    public static final int MAX_ENCHANTMENTS = 48;
    /** Most attribute modifiers on one item. */
    public static final int MAX_ATTRIBUTES = 16;
    /** The largest attribute amount that is accepted, either sign. */
    public static final double MAX_ATTRIBUTE_AMOUNT = 2048d;

    /** The equipment slot groups a modifier can apply in, as bukkit spells them. */
    public static final Set<String> SLOTS = Set.of("any", "mainhand", "offhand", "hand",
            "feet", "legs", "chest", "head", "armor", "body", "saddle");

    private static final Pattern MATERIAL = Pattern.compile("[A-Z0-9_]+");
    private static final Pattern KEY = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /** How an attribute modifier is applied, the three operations minecraft knows. */
    public enum Operation {
        /** Adds the amount. */
        ADD_NUMBER,
        /** Adds amount times the base value. */
        ADD_SCALAR,
        /** Multiplies everything by one plus the amount. */
        MULTIPLY_SCALAR_1;

        /**
         * @param name what was stored or sent, any case
         * @return the operation, {@link #ADD_NUMBER} for anything unknown
         */
        public static Operation byName(String name) {
            if (name == null) return ADD_NUMBER;
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ADD_NUMBER;
            }
        }
    }

    /**
     * One attribute modifier.
     */
    public static class Modifier implements Serializable {

        private static final long serialVersionUID = 5101L;

        private String attribute;
        private double amount;
        private Operation operation = Operation.ADD_NUMBER;
        private String slot = "any";

        public Modifier() {
        }

        /**
         * @param attribute the attribute, as a key - with or without the namespace
         * @param amount    by how much
         * @param operation how it is applied
         * @param slot      where the item has to be for it to count, see {@link #SLOTS}
         */
        public Modifier(String attribute, double amount, Operation operation, String slot) {
            setAttribute(attribute);
            this.amount = amount;
            this.operation = operation == null ? Operation.ADD_NUMBER : operation;
            setSlot(slot);
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = normaliseKey(attribute);
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public Operation getOperation() {
            return operation == null ? Operation.ADD_NUMBER : operation;
        }

        public void setOperation(Operation operation) {
            this.operation = operation == null ? Operation.ADD_NUMBER : operation;
        }

        public String getSlot() {
            return slot == null ? "any" : slot;
        }

        public void setSlot(String slot) {
            String value = slot == null ? "" : slot.trim().toLowerCase(Locale.ROOT);
            this.slot = value.isEmpty() ? "any" : value;
        }

        JSONObject toJson() {
            return new JSONObject()
                    .put("attribute", attribute)
                    .put("amount", amount)
                    .put("operation", getOperation().name())
                    .put("slot", getSlot());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Modifier modifier)) return false;
            return Double.compare(amount, modifier.amount) == 0
                    && Objects.equals(attribute, modifier.attribute)
                    && getOperation() == modifier.getOperation()
                    && getSlot().equals(modifier.getSlot());
        }

        @Override
        public int hashCode() {
            return Objects.hash(attribute, amount, getOperation(), getSlot());
        }
    }

    private String material;
    private int amount = 1;
    /** The custom name with {@code &} colour codes, {@code null} for the default name. */
    private String name;
    private List<String> lore = new ArrayList<>();
    /** Enchantment key to level, in the order they were added. */
    private Map<String, Integer> enchantments = new LinkedHashMap<>();
    private List<Modifier> attributes = new ArrayList<>();
    private boolean unbreakable;
    /** How worn the item is, zero for brand new. */
    private int damage;

    public ItemSpec() {
    }

    /**
     * @param material the material, as bukkit names it - the namespace and the case do not matter
     * @param amount   how many
     */
    public ItemSpec(String material, int amount) {
        setMaterial(material);
        this.amount = amount;
    }

    /* ------------------------------------------------------------------------------------- fields */

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = normaliseMaterial(material);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null || name.isBlank() ? null : name;
    }

    public List<String> getLore() {
        if (lore == null) lore = new ArrayList<>();
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
    }

    public Map<String, Integer> getEnchantments() {
        if (enchantments == null) enchantments = new LinkedHashMap<>();
        return enchantments;
    }

    /**
     * @param key   the enchantment, with or without the namespace
     * @param level its level
     * @return this item, so calls can be chained
     */
    public ItemSpec withEnchantment(String key, int level) {
        String normalised = normaliseKey(key);
        if (normalised.isEmpty()) return this;
        getEnchantments().put(normalised, level);
        return this;
    }

    public List<Modifier> getAttributes() {
        if (attributes == null) attributes = new ArrayList<>();
        return attributes;
    }

    /**
     * @param modifier the modifier to add
     * @return this item, so calls can be chained
     */
    public ItemSpec withAttribute(Modifier modifier) {
        if (modifier != null) getAttributes().add(modifier);
        return this;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = Math.max(0, damage);
    }

    /* ---------------------------------------------------------------------------------- questions */

    /**
     * @return whether this is nothing but a material and an amount - such an item is written the short way
     */
    public boolean isPlain() {
        return name == null && getLore().isEmpty() && getEnchantments().isEmpty() && getAttributes().isEmpty()
                && !unbreakable && damage == 0;
    }

    /**
     * @param other another item
     * @return whether the two are the same thing apart from how many there are, so they can be added up
     */
    public boolean isSimilar(ItemSpec other) {
        if (other == null) return false;
        return Objects.equals(material, other.material)
                && Objects.equals(name, other.name)
                && getLore().equals(other.getLore())
                && getEnchantments().equals(other.getEnchantments())
                && getAttributes().equals(other.getAttributes())
                && unbreakable == other.unbreakable
                && damage == other.damage;
    }

    /**
     * Checks everything that can be checked without knowing which names a server has.
     *
     * @param maxAmount the most of it there may be
     * @return what is wrong, in words for the admin, or {@code null} when it is fine
     */
    public String problem(int maxAmount) {
        if (material == null || material.isEmpty()) return "es fehlt das Material.";
        if (!MATERIAL.matcher(material).matches()) return "'" + material + "' ist kein gültiger Materialname.";
        if (amount < 1 || amount > maxAmount) {
            return "die Anzahl von " + material + " muss zwischen 1 und " + maxAmount + " liegen.";
        }
        if (name != null && name.length() > MAX_NAME) return "der Name ist länger als " + MAX_NAME + " Zeichen.";
        if (getLore().size() > MAX_LORE_LINES) return "höchstens " + MAX_LORE_LINES + " Zeilen Beschreibung.";
        for (String line : getLore()) {
            if (line != null && line.length() > MAX_LORE_LINE) {
                return "eine Beschreibungszeile ist länger als " + MAX_LORE_LINE + " Zeichen.";
            }
        }
        if (getEnchantments().size() > MAX_ENCHANTMENTS) return "zu viele Verzauberungen.";
        for (Map.Entry<String, Integer> enchantment : getEnchantments().entrySet()) {
            if (!KEY.matcher(enchantment.getKey()).matches()) {
                return "'" + enchantment.getKey() + "' ist keine gültige Verzauberung.";
            }
            int level = enchantment.getValue() == null ? 0 : enchantment.getValue();
            if (level < 1 || level > MAX_ENCHANT_LEVEL) {
                return "die Stufe von " + enchantment.getKey() + " muss zwischen 1 und " + MAX_ENCHANT_LEVEL
                        + " liegen.";
            }
        }
        if (getAttributes().size() > MAX_ATTRIBUTES) return "höchstens " + MAX_ATTRIBUTES + " Attribute.";
        for (Modifier modifier : getAttributes()) {
            if (modifier.getAttribute() == null || !KEY.matcher(modifier.getAttribute()).matches()) {
                return "'" + modifier.getAttribute() + "' ist kein gültiges Attribut.";
            }
            if (!Double.isFinite(modifier.getAmount()) || Math.abs(modifier.getAmount()) > MAX_ATTRIBUTE_AMOUNT) {
                return "der Wert von " + modifier.getAttribute() + " muss zwischen -" + (int) MAX_ATTRIBUTE_AMOUNT
                        + " und " + (int) MAX_ATTRIBUTE_AMOUNT + " liegen.";
            }
            if (!SLOTS.contains(modifier.getSlot())) {
                return "'" + modifier.getSlot() + "' ist kein Ausrüstungsplatz.";
            }
        }
        if (damage < 0) return "der Verschleiß kann nicht negativ sein.";
        return null;
    }

    /**
     * @return a short, readable account of the item, e.g. {@code 1x DIAMOND_SWORD "Excalibur" (sharpness 5)}
     */
    public String describe() {
        StringBuilder text = new StringBuilder().append(amount).append("x ").append(material);
        if (name != null) text.append(" \"").append(stripCodes(name)).append('"');
        List<String> extras = new ArrayList<>();
        for (Map.Entry<String, Integer> enchantment : getEnchantments().entrySet()) {
            extras.add(shortKey(enchantment.getKey()) + " " + enchantment.getValue());
        }
        for (Modifier modifier : getAttributes()) {
            extras.add(shortKey(modifier.getAttribute()) + " " + formatAmount(modifier));
        }
        if (unbreakable) extras.add("unzerstörbar");
        if (!extras.isEmpty()) text.append(" (").append(String.join(", ", extras)).append(')');
        return text.toString();
    }

    private static String formatAmount(Modifier modifier) {
        double value = modifier.getAmount();
        String number = value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
        String signed = value >= 0 ? "+" + number : number;
        return modifier.getOperation() == Operation.ADD_NUMBER ? signed : signed + "x";
    }

    private static String shortKey(String key) {
        return key != null && key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }

    /**
     * @param text a text with {@code &} or {@code §} colour codes
     * @return it without them
     */
    public static String stripCodes(String text) {
        return text == null ? null : text.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
    }

    /* ------------------------------------------------------------------------------------ copying */

    /**
     * @return a copy that can be changed without touching this one
     */
    public ItemSpec copy() {
        return fromJson(toJson());
    }

    /**
     * @param amount how many the copy holds
     * @return a copy of this item with another amount
     */
    public ItemSpec withAmount(int amount) {
        ItemSpec copy = copy();
        copy.amount = amount;
        return copy;
    }

    /* -------------------------------------------------------------------------------------- json */

    /**
     * @return the item as json, the form the website reads and writes
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject()
                .put("material", material == null ? "" : material)
                .put("amount", amount);
        if (name != null) json.put("name", name);
        if (!getLore().isEmpty()) json.put("lore", new JSONArray(getLore()));
        if (!getEnchantments().isEmpty()) {
            JSONArray enchants = new JSONArray();
            for (Map.Entry<String, Integer> enchantment : getEnchantments().entrySet()) {
                enchants.put(new JSONObject().put("key", enchantment.getKey()).put("level", enchantment.getValue()));
            }
            json.put("enchantments", enchants);
        }
        if (!getAttributes().isEmpty()) {
            JSONArray modifiers = new JSONArray();
            for (Modifier modifier : getAttributes()) modifiers.put(modifier.toJson());
            json.put("attributes", modifiers);
        }
        if (unbreakable) json.put("unbreakable", true);
        if (damage > 0) json.put("damage", damage);
        return json;
    }

    /**
     * Reads an item the way {@link #toJson()} wrote it, or the way the website sent it. Nothing is checked
     * here beyond the shape - {@link #problem(int)} says whether it makes sense.
     *
     * @param json the item
     * @return the item, never {@code null}
     */
    public static ItemSpec fromJson(JSONObject json) {
        ItemSpec spec = new ItemSpec();
        if (json == null) return spec;
        spec.setMaterial(json.optString("material", ""));
        spec.amount = json.optInt("amount", 1);
        spec.setName(json.optString("name", null));
        JSONArray lore = json.optJSONArray("lore");
        if (lore != null) {
            for (int i = 0; i < lore.length(); i++) spec.getLore().add(lore.optString(i, ""));
            // trailing empty lines are what an empty text box leaves behind, not something anybody meant
            while (!spec.getLore().isEmpty() && spec.getLore().get(spec.getLore().size() - 1).isBlank()) {
                spec.getLore().remove(spec.getLore().size() - 1);
            }
        }
        JSONArray enchants = json.optJSONArray("enchantments");
        if (enchants != null) {
            for (int i = 0; i < enchants.length(); i++) {
                JSONObject enchant = enchants.optJSONObject(i);
                if (enchant == null) continue;
                String key = enchant.optString("key", "");
                if (key.isBlank()) continue;
                spec.withEnchantment(key, enchant.optInt("level", 1));
            }
        }
        JSONArray modifiers = json.optJSONArray("attributes");
        if (modifiers != null) {
            for (int i = 0; i < modifiers.length(); i++) {
                JSONObject modifier = modifiers.optJSONObject(i);
                if (modifier == null) continue;
                String attribute = modifier.optString("attribute", "");
                if (attribute.isBlank()) continue;
                spec.withAttribute(new Modifier(attribute, modifier.optDouble("amount", 0d),
                        Operation.byName(modifier.optString("operation", null)), modifier.optString("slot", "any")));
            }
        }
        spec.unbreakable = json.optBoolean("unbreakable", false);
        spec.setDamage(json.optInt("damage", 0));
        return spec;
    }

    /**
     * @return the item as one line without separators, so it fits into the {@code key=value;...} form prizes
     *         are stored in
     */
    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(toJson().toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @param text an item as {@link #encode()} wrote it
     * @return the item, or {@code null} if the text cannot be read
     */
    public static ItemSpec decode(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            String json = new String(Base64.getUrlDecoder().decode(text.trim()), StandardCharsets.UTF_8);
            return fromJson(new JSONObject(json));
        } catch (IllegalArgumentException | JSONException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------------------------------- names */

    /**
     * @param text a material as somebody typed it, with or without the namespace
     * @return it the way bukkit spells it
     */
    public static String normaliseMaterial(String text) {
        String value = text == null ? "" : text.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (value.startsWith("MINECRAFT:")) value = value.substring("MINECRAFT:".length());
        return value;
    }

    /**
     * @param text a registry key as somebody typed it - {@code Sharpness}, {@code sharpness},
     *             {@code minecraft:sharpness}
     * @return it as a full key, {@code minecraft:sharpness}, or an empty string for nothing
     */
    public static String normaliseKey(String text) {
        String value = text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (value.isEmpty()) return value;
        // the attribute keys lost their "generic." prefix in 1.21.2 - an old name still means the same thing
        if (value.startsWith("generic.")) value = value.substring("generic.".length());
        if (value.startsWith("minecraft:generic.")) value = "minecraft:" + value.substring("minecraft:generic.".length());
        return value.contains(":") ? value : "minecraft:" + value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemSpec spec && isSimilar(spec) && amount == spec.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, amount, name, getLore(), getEnchantments(), getAttributes(), unbreakable, damage);
    }

    @Override
    public String toString() {
        return describe();
    }
}
