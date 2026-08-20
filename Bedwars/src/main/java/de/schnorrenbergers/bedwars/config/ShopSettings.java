package de.schnorrenbergers.bedwars.config;

import de.schnorrenbergers.bedwars.shop.Cost;
import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.shop.item.ShopCategory;
import de.schnorrenbergers.bedwars.shop.item.ShopItem;
import de.schnorrenbergers.bedwars.util.ConfigFile;
import de.schnorrenbergers.bedwars.util.Registries;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the item shop sells, out of {@code shop.yml}.
 * <p>
 * The sortiment and the prices are hypixel's, but not a single one of them is written in code: the file is
 * created with those numbers and read back afterwards, so a server that wants cheaper diamonds or a
 * category of its own changes the file rather than the plugin.
 */
public final class ShopSettings {

    private final ConfigFile file;
    private final List<ShopCategory> categories = new ArrayList<>();
    private final Map<String, ShopItem> byId = new LinkedHashMap<>();
    /** What addons put into the shop while they are switched on; never written to the file. */
    private final Map<String, ShopItem> extraItems = new LinkedHashMap<>();
    private final Map<String, ShopCategory> extraCategories = new LinkedHashMap<>();

    public ShopSettings() {
        file = new ConfigFile("shop.yml");
        load();
    }

    /**
     * Reads the file, writing the hypixel shop into it the first time.
     */
    public void load() {
        file.reload();
        categories.clear();
        byId.clear();
        file.section("categories",
                "One block per page of the shop. 'slot' is where the tab sits in the top row.",
                "Every entry underneath 'items' is one thing to buy:",
                "  material / amount / name / price-currency (IRON, GOLD, DIAMOND, EMERALD) / price-amount",
                "  enchantments: ['SHARPNESS:1'], effects: ['SPEED:1:45'] as type:amplifier:seconds",
                "  team-block: WOOL, GLASS, TERRACOTTA or CONCRETE hands it over in the buyer's colour",
                "  permanent: the buyer keeps it through a death",
                "  armor-tier: this entry is an armour level - boots and leggings, worn for good",
                "  tool / tool-tier: a step of a tool chain, which falls back one step when the buyer dies",
                "  sword: buying it takes the sword the buyer already carries",
                "  lifetime-seconds: how long what a spawn egg summons stays around",
                "  slot: where it sits on the page, -1 fills the page from the top left");
        writeDefaults();
        readAll();
        file.save();
    }

    // ------------------------------------------------------------ the hypixel shop

    private void writeDefaults() {
        category("blocks", "<white>Blocks", Material.TERRACOTTA, 2);
        teamBlock(item("blocks", "wool", Material.WHITE_WOOL, 16, Currency.IRON, 4, "<white>Wool"), "WOOL");
        teamBlock(item("blocks", "terracotta", Material.TERRACOTTA, 16, Currency.IRON, 12,
                "<white>Hardened Clay"), "TERRACOTTA");
        teamBlock(item("blocks", "glass", Material.GLASS, 4, Currency.IRON, 12,
                "<white>Blast-Proof Glass"), "GLASS");
        item("blocks", "end-stone", Material.END_STONE, 12, Currency.IRON, 24, "<white>End Stone");
        item("blocks", "ladder", Material.LADDER, 8, Currency.IRON, 4, "<white>Ladder");
        item("blocks", "planks", Material.OAK_PLANKS, 16, Currency.GOLD, 4, "<white>Oak Planks");
        item("blocks", "obsidian", Material.OBSIDIAN, 4, Currency.EMERALD, 4, "<white>Obsidian");

        category("melee", "<red>Melee", Material.GOLDEN_SWORD, 3);
        sword(item("melee", "stone-sword", Material.STONE_SWORD, 1, Currency.IRON, 10, "<white>Stone Sword"));
        sword(item("melee", "iron-sword", Material.IRON_SWORD, 1, Currency.GOLD, 7, "<white>Iron Sword"));
        sword(item("melee", "diamond-sword", Material.DIAMOND_SWORD, 1, Currency.EMERALD, 4,
                "<white>Diamond Sword"));
        enchant(item("melee", "knockback-stick", Material.STICK, 1, Currency.GOLD, 5,
                "<white>Stick <gray>(Knockback I)"), "KNOCKBACK:1");

        category("armor", "<aqua>Armor", Material.CHAINMAIL_BOOTS, 4);
        armor(item("armor", "chainmail-armor", Material.CHAINMAIL_BOOTS, 1, Currency.IRON, 40,
                "<white>Chainmail Armor"), 1);
        armor(item("armor", "iron-armor", Material.IRON_BOOTS, 1, Currency.GOLD, 12,
                "<white>Iron Armor"), 2);
        armor(item("armor", "diamond-armor", Material.DIAMOND_BOOTS, 1, Currency.EMERALD, 6,
                "<white>Diamond Armor"), 3);

        category("tools", "<yellow>Tools", Material.STONE_PICKAXE, 5);
        permanent(item("tools", "shears", Material.SHEARS, 1, Currency.IRON, 20, "<white>Shears"));
        tool(enchant(item("tools", "wooden-pickaxe", Material.WOODEN_PICKAXE, 1, Currency.IRON, 10,
                "<white>Wooden Pickaxe"), "EFFICIENCY:1"), "pickaxe", 1);
        tool(enchant(item("tools", "iron-pickaxe", Material.IRON_PICKAXE, 1, Currency.IRON, 10,
                "<white>Iron Pickaxe"), "EFFICIENCY:2"), "pickaxe", 2);
        tool(enchant(item("tools", "golden-pickaxe", Material.GOLDEN_PICKAXE, 1, Currency.GOLD, 3,
                "<white>Golden Pickaxe"), "EFFICIENCY:3"), "pickaxe", 3);
        tool(enchant(item("tools", "diamond-pickaxe", Material.DIAMOND_PICKAXE, 1, Currency.GOLD, 6,
                "<white>Diamond Pickaxe"), "EFFICIENCY:3"), "pickaxe", 4);
        tool(enchant(item("tools", "wooden-axe", Material.WOODEN_AXE, 1, Currency.IRON, 10,
                "<white>Wooden Axe"), "EFFICIENCY:1"), "axe", 1);
        tool(enchant(item("tools", "stone-axe", Material.STONE_AXE, 1, Currency.IRON, 10,
                "<white>Stone Axe"), "EFFICIENCY:1"), "axe", 2);
        tool(enchant(item("tools", "iron-axe", Material.IRON_AXE, 1, Currency.GOLD, 3,
                "<white>Iron Axe"), "EFFICIENCY:2"), "axe", 3);
        tool(enchant(item("tools", "diamond-axe", Material.DIAMOND_AXE, 1, Currency.GOLD, 6,
                "<white>Diamond Axe"), "EFFICIENCY:3"), "axe", 4);

        category("potions", "<light_purple>Potions", Material.BREWING_STAND, 6);
        effect(item("potions", "speed-potion", Material.POTION, 1, Currency.EMERALD, 1,
                "<white>Speed II Potion <gray>(45s)"), "SPEED:1:45");
        effect(item("potions", "jump-potion", Material.POTION, 1, Currency.EMERALD, 1,
                "<white>Jump V Potion <gray>(45s)"), "JUMP_BOOST:4:45");
        effect(item("potions", "invisibility-potion", Material.POTION, 1, Currency.EMERALD, 2,
                "<white>Invisibility Potion <gray>(30s)"), "INVISIBILITY:0:30");

        category("utility", "<green>Utility", Material.TNT, 7);
        item("utility", "golden-apple", Material.GOLDEN_APPLE, 1, Currency.GOLD, 3, "<white>Golden Apple");
        item("utility", "fireball", Material.FIRE_CHARGE, 1, Currency.IRON, 40, "<white>Fireball");
        item("utility", "tnt", Material.TNT, 1, Currency.GOLD, 8, "<white>TNT");
        item("utility", "ender-pearl", Material.ENDER_PEARL, 1, Currency.EMERALD, 4, "<white>Ender Pearl");
        item("utility", "water-bucket", Material.WATER_BUCKET, 1, Currency.EMERALD, 3, "<white>Water Bucket");
        item("utility", "magic-milk", Material.MILK_BUCKET, 1, Currency.GOLD, 4, "<white>Magic Milk");
        item("utility", "sponge", Material.SPONGE, 4, Currency.GOLD, 3, "<white>Sponge");
        lifetime(item("utility", "bedbug", Material.SILVERFISH_SPAWN_EGG, 1, Currency.EMERALD, 2,
                "<white>Bedbug"), 15);
        lifetime(item("utility", "dream-defender", Material.IRON_GOLEM_SPAWN_EGG, 1, Currency.IRON, 120,
                "<white>Dream Defender"), 240);
    }

    /**
     * Writes one page, without touching what is already in the file.
     */
    private void category(String id, String displayName, Material icon, int slot) {
        String path = "categories." + id;
        file.get(path + ".display-name", displayName);
        file.get(path + ".icon", icon.name());
        file.get(path + ".slot", slot);
    }

    /**
     * Writes one entry, without touching what is already in the file.
     *
     * @return the path it was written under, so the flags below can be added to it
     */
    private String item(String category, String id, Material material, int amount, Currency currency,
                        int price, String displayName) {
        String path = "categories." + category + ".items." + id;
        file.get(path + ".material", material.name());
        file.get(path + ".amount", amount);
        file.get(path + ".name", displayName);
        file.get(path + ".price-currency", currency.name());
        file.get(path + ".price-amount", price);
        return path;
    }

    private void teamBlock(String path, String family) {
        file.get(path + ".team-block", family);
    }

    private String enchant(String path, String... enchantments) {
        if (!file.contains(path + ".enchantments")) file.set(path + ".enchantments", List.of(enchantments));
        return path;
    }

    private void effect(String path, String... effects) {
        if (!file.contains(path + ".effects")) file.set(path + ".effects", List.of(effects));
    }

    private void lifetime(String path, int seconds) {
        file.get(path + ".lifetime-seconds", seconds);
    }

    private void permanent(String path) {
        file.get(path + ".permanent", true);
    }

    private void armor(String path, int tier) {
        file.get(path + ".armor-tier", tier);
    }

    private void tool(String path, String group, int tier) {
        file.get(path + ".tool", group);
        file.get(path + ".tool-tier", tier);
    }

    private void sword(String path) {
        file.get(path + ".sword", true);
    }

    // ------------------------------------------------------------------ reading

    /**
     * Reads every page and every entry back out of the file, so that what is played is what is written -
     * including the parts nobody in this class ever wrote.
     */
    private void readAll() {
        List<ShopCategory> read = new ArrayList<>();
        for (String categoryId : file.keys("categories")) {
            String base = "categories." + categoryId;
            Material icon = material(file.read(base + ".icon", Material.STONE.name()),
                    categoryId + "'s icon");
            List<ShopItem> items = new ArrayList<>();
            for (String itemId : file.keys(base + ".items")) {
                ShopItem item = readItem(categoryId, itemId, base + ".items." + itemId);
                if (item == null) continue;
                items.add(item);
                byId.put(item.id(), item);
            }
            read.add(new ShopCategory(categoryId,
                    file.read(base + ".display-name", categoryId),
                    icon == null ? Material.STONE : icon,
                    file.read(base + ".slot", -1),
                    List.copyOf(items)));
        }
        read.sort(Comparator.comparingInt(category -> category.slot() < 0 ? Integer.MAX_VALUE : category.slot()));
        categories.addAll(read);
    }

    /**
     * @return the entry, or {@code null} when it names a material this server does not have
     */
    private @Nullable ShopItem readItem(String categoryId, String itemId, String path) {
        Material material = material(file.read(path + ".material", Material.STONE.name()), itemId);
        if (material == null) return null;
        String tool = file.read(path + ".tool", "");
        return new ShopItem(
                itemId,
                categoryId,
                file.read(path + ".name", itemId),
                material,
                Math.max(1, file.read(path + ".amount", 1)),
                costs(path, itemId),
                List.copyOf(file.raw().getStringList(path + ".lore")),
                enchantments(path, itemId),
                effects(path, itemId),
                ShopItem.TeamBlock.byName(file.read(path + ".team-block", "NONE")),
                file.read(path + ".permanent", false),
                Math.max(0, file.read(path + ".armor-tier", 0)),
                tool.isBlank() ? null : tool.toLowerCase(Locale.ROOT),
                Math.max(0, file.read(path + ".tool-tier", 0)),
                file.read(path + ".sword", false),
                file.read(path + ".slot", -1),
                Math.max(0, file.read(path + ".lifetime-seconds", 0)),
                file.read(path + ".enemy-only", false));
    }

    /**
     * Reads what an entry costs.
     * <p>
     * The second half is optional and almost never used - it exists for the one kind of item that has to
     * be expensive in two currencies at once, and an entry that says nothing about it costs one thing like
     * everything else in the shop.
     *
     * @param path   the entry
     * @param itemId what it is called, for the warning
     * @return the price
     */
    private List<Cost> costs(String path, String itemId) {
        List<Cost> costs = new ArrayList<>();
        costs.add(new Cost(currency(path + ".price-currency", itemId),
                Math.max(0, file.read(path + ".price-amount", 1))));
        int extra = Math.max(0, file.read(path + ".extra-price-amount", 0));
        if (extra > 0) {
            costs.add(new Cost(currency(path + ".extra-price-currency", itemId), extra));
        }
        return List.copyOf(costs);
    }

    /**
     * @param path   where the currency is written
     * @param itemId what it is called, for the warning
     * @return the currency, iron when the file names one this server does not have
     */
    private Currency currency(String path, String itemId) {
        Currency currency = Currency.byName(file.read(path, Currency.IRON.name()));
        if (currency != null) return currency;
        warn("'" + itemId + "' is paid in a currency that does not exist, iron is used instead.");
        return Currency.IRON;
    }

    /**
     * @return what the entry comes enchanted with, skipping every enchantment this server does not know
     */
    private Map<Enchantment, Integer> enchantments(String path, String itemId) {
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        for (String line : file.raw().getStringList(path + ".enchantments")) {
            String[] parts = line.split(":");
            Enchantment enchantment = enchantment(parts[0]);
            if (enchantment == null) {
                warn("'" + itemId + "' asks for the enchantment '" + parts[0] + "', which does not exist.");
                continue;
            }
            enchantments.put(enchantment, parts.length > 1 ? number(parts[1], 1) : 1);
        }
        return Map.copyOf(enchantments);
    }

    /**
     * @return the potion effects the entry carries, written as {@code TYPE:amplifier:seconds}
     */
    private List<PotionEffect> effects(String path, String itemId) {
        List<PotionEffect> effects = new ArrayList<>();
        for (String line : file.raw().getStringList(path + ".effects")) {
            String[] parts = line.split(":");
            PotionEffectType type = effectType(parts[0]);
            if (type == null) {
                warn("'" + itemId + "' asks for the effect '" + parts[0] + "', which does not exist.");
                continue;
            }
            int amplifier = parts.length > 1 ? number(parts[1], 0) : 0;
            int seconds = parts.length > 2 ? number(parts[2], 30) : 30;
            effects.add(new PotionEffect(type, seconds * 20, amplifier, false, true, true));
        }
        return List.copyOf(effects);
    }

    // ------------------------------------------------------------------- addons

    /**
     * Gives the shop a page that is not in the file.
     * <p>
     * This is how an addon sells something: it hands its entries over while it is switched on and takes
     * them back when it is switched off, and nothing of it is left in {@code shop.yml} either way. A
     * disabled addon therefore has no shop entry at all, rather than one that refuses to be bought.
     *
     * @param category the page
     */
    public void register(ShopCategory category) {
        extraCategories.put(category.id(), category);
    }

    /**
     * Gives the shop an entry that is not in the file.
     *
     * @param item the entry, whose category has to exist by then
     */
    public void register(ShopItem item) {
        if (getCategory(item.category()) == null) {
            warn("'" + item.id() + "' is sold on a page called '" + item.category()
                    + "', which no addon and no config created - it will not be shown.");
        }
        extraItems.put(item.id(), item);
    }

    /**
     * Takes an addon's page or entry back out again.
     *
     * @param id what to remove
     */
    public void unregister(String id) {
        extraItems.remove(id);
        extraCategories.remove(id);
    }

    // ------------------------------------------------------------------- lookups

    /**
     * @param id the entry's id
     * @return that entry, or {@code null} when the shop does not sell it
     */
    public @Nullable ShopItem get(String id) {
        if (id == null) return null;
        ShopItem item = byId.get(id);
        return item != null ? item : extraItems.get(id);
    }

    /**
     * @return every page, with whatever the addons added to it
     */
    public List<ShopCategory> getCategories() {
        if (extraItems.isEmpty() && extraCategories.isEmpty()) return List.copyOf(categories);
        List<ShopCategory> merged = new ArrayList<>();
        for (ShopCategory category : categories) merged.add(withExtras(category));
        for (ShopCategory category : extraCategories.values()) {
            if (configured(category.id()) == null) merged.add(withExtras(category));
        }
        merged.sort(Comparator.comparingInt(category -> category.slot() < 0 ? Integer.MAX_VALUE : category.slot()));
        return List.copyOf(merged);
    }

    /**
     * @param category a page
     * @return it with the addon entries that belong on it appended
     */
    private ShopCategory withExtras(ShopCategory category) {
        List<ShopItem> items = new ArrayList<>(category.items());
        for (ShopItem item : extraItems.values()) {
            if (category.id().equalsIgnoreCase(item.category())) items.add(item);
        }
        return items.size() == category.items().size() ? category
                : new ShopCategory(category.id(), category.displayName(), category.icon(),
                        category.slot(), List.copyOf(items));
    }

    /**
     * @param id a page
     * @return the page as the file wrote it, or {@code null} when the file has no such page
     */
    private @Nullable ShopCategory configured(String id) {
        for (ShopCategory category : categories) {
            if (category.id().equalsIgnoreCase(id)) return category;
        }
        return null;
    }

    /**
     * @return the page that is opened when somebody clicks a shop keeper, or {@code null} for an empty shop
     */
    public @Nullable ShopCategory getFirstCategory() {
        List<ShopCategory> all = getCategories();
        return all.isEmpty() ? null : all.getFirst();
    }

    public @Nullable ShopCategory getCategory(String id) {
        for (ShopCategory category : getCategories()) {
            if (category.id().equalsIgnoreCase(id)) return category;
        }
        return null;
    }

    /**
     * @param tier the armour level
     * @return the entry that sets it, or {@code null} when no entry does
     */
    public @Nullable ShopItem getArmor(int tier) {
        for (ShopItem item : byId.values()) {
            if (item.armorTier() == tier) return item;
        }
        return null;
    }

    /**
     * @param group a tool chain, e.g. {@code pickaxe}
     * @param tier  the step
     * @return the entry at that step, or {@code null} when the chain does not go that far
     */
    public @Nullable ShopItem getTool(String group, int tier) {
        for (ShopItem item : byId.values()) {
            if (item.isTool() && item.toolGroup().equalsIgnoreCase(group) && item.toolTier() == tier) {
                return item;
            }
        }
        return null;
    }

    public Map<String, ShopItem> all() {
        Map<String, ShopItem> all = new LinkedHashMap<>(byId);
        all.putAll(extraItems);
        return Map.copyOf(all);
    }

    public ConfigFile getFile() {
        return file;
    }

    // -------------------------------------------------------------------- helpers

    private static @Nullable Material material(String name, String what) {
        Material material = Material.matchMaterial(name);
        if (material == null) warn("'" + name + "' of " + what + " is not a material, it is skipped.");
        return material;
    }

    private static @Nullable Enchantment enchantment(String name) {
        return Registries.enchantment(name);
    }

    private static @Nullable PotionEffectType effectType(String name) {
        return Registries.effect(name);
    }

    private static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void warn(String message) {
        Bukkit.getLogger().warning("[Bedwars] shop.yml: " + message);
    }
}
