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
                "     the shop shows one button per chain: the step the buyer may buy next.",
                "     Swords and armour are not chains - every level of those is bought on its own",
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
        teamBlock(lore(item("blocks", "wool", Material.WHITE_WOOL, 16, Currency.IRON, 4, "<white>Wool"),
                "<gray>The cheapest way across a gap.",
                "<gray>Comes in your team's colour, so",
                "<gray>everybody can see whose bridge it is.",
                "<red>Burns, and breaks in seconds."), "WOOL");
        teamBlock(lore(item("blocks", "terracotta", Material.TERRACOTTA, 16, Currency.IRON, 12,
                "<white>Hardened Clay"),
                "<gray>Three times the iron of wool and worth it:",
                "<gray>it takes real time to break through",
                "<gray>and it does not burn."), "TERRACOTTA");
        teamBlock(lore(item("blocks", "glass", Material.GLASS, 4, Currency.IRON, 12,
                "<white>Blast-Proof Glass"),
                "<gray>No explosion touches it - not tnt,",
                "<gray>not a fireball.",
                "<gray>Four blocks around a bed are four blocks",
                "<gray>nobody blows their way through."), "GLASS");
        lore(item("blocks", "end-stone", Material.END_STONE, 12, Currency.IRON, 24, "<white>End Stone"),
                "<gray>Slow to mine and immune to fireballs.",
                "<gray>The block a defence is actually built of.");
        lore(item("blocks", "ladder", Material.LADDER, 8, Currency.IRON, 4, "<white>Ladder"),
                "<gray>Straight up, or straight down without",
                "<gray>the fall. Cheap and unnoticed.");
        lore(item("blocks", "planks", Material.OAK_PLANKS, 16, Currency.GOLD, 4, "<white>Oak Planks"),
                "<gray>Survives an explosion where wool does not,",
                "<gray>and goes down fast enough to be placed",
                "<gray>in the middle of a fight.");
        lore(item("blocks", "obsidian", Material.OBSIDIAN, 4, Currency.EMERALD, 4, "<white>Obsidian"),
                "<gray>The last word in defence.",
                "<gray>Only a diamond pickaxe gets through it,",
                "<gray>and not quickly.");

        category("melee", "<red>Melee", Material.GOLDEN_SWORD, 3);
        // no chain: a sword is bought outright, and whichever one you can afford is the one you get.
        // A death costs it entirely - what comes back is the wooden sword of the starting kit
        sword(lore(item("melee", "stone-sword", Material.STONE_SWORD, 1, Currency.IRON, 10,
                "<white>Stone Sword"),
                "<gray>The first upgrade anybody buys.",
                "<red>Lost on death - you start again",
                "<red>with the wooden one."));
        sword(lore(item("melee", "iron-sword", Material.IRON_SWORD, 1, Currency.GOLD, 7,
                "<white>Iron Sword"),
                "<gray>Enough to win a fight against anybody",
                "<gray>who spent their gold on something else.",
                "<red>Lost on death."));
        sword(lore(item("melee", "diamond-sword", Material.DIAMOND_SWORD, 1, Currency.EMERALD, 4,
                "<white>Diamond Sword"),
                "<gray>The best there is. Four emeralds is",
                "<gray>most of a trip to the middle.",
                "<red>Lost on death, so carry it carefully."));
        enchant(lore(item("melee", "knockback-stick", Material.STICK, 1, Currency.GOLD, 5,
                "<white>Stick <gray>(Knockback I)"),
                "<gray>Does almost no damage and wins fights",
                "<gray>anyway: it throws people off bridges.",
                "<gray>The void does not care about armour."), "KNOCKBACK:1");

        category("armor", "<aqua>Armor", Material.CHAINMAIL_BOOTS, 4);
        armor(lore(item("armor", "chainmail-armor", Material.CHAINMAIL_BOOTS, 1, Currency.IRON, 40,
                "<white>Chainmail Armor"),
                "<green>Kept for the whole round, deaths included.",
                "<gray>Boots and leggings. The chestplate stays",
                "<gray>your team's leather so people can still",
                "<gray>tell whose side you are on."), 1);
        armor(lore(item("armor", "iron-armor", Material.IRON_BOOTS, 1, Currency.GOLD, 12,
                "<white>Iron Armor"),
                "<green>Kept for the whole round, deaths included.",
                "<gray>Bought outright - you do not have to",
                "<gray>own the chainmail first."), 2);
        armor(lore(item("armor", "diamond-armor", Material.DIAMOND_BOOTS, 1, Currency.EMERALD, 6,
                "<white>Diamond Armor"),
                "<green>Kept for the whole round, deaths included.",
                "<gray>Six emeralds once, and every fight for",
                "<gray>the rest of the round is easier."), 3);

        category("tools", "<yellow>Tools", Material.STONE_PICKAXE, 5);
        permanent(lore(item("tools", "shears", Material.SHEARS, 1, Currency.IRON, 20, "<white>Shears"),
                "<green>Kept for the whole round, deaths included.",
                "<gray>Takes wool down instantly. Twenty iron once,",
                "<gray>and every wool defence stops mattering."));
        tool(enchant(lore(item("tools", "wooden-pickaxe", Material.WOODEN_PICKAXE, 1, Currency.IRON, 10,
                "<white>Wooden Pickaxe"),
                "<gray>Step one of the pickaxe. Buy it again and",
                "<gray>the same button hands you the next one.",
                "<yellow>A death costs one step, never this one."), "EFFICIENCY:1"), "pickaxe", 1);
        tool(enchant(item("tools", "iron-pickaxe", Material.IRON_PICKAXE, 1, Currency.IRON, 10,
                "<white>Iron Pickaxe"), "EFFICIENCY:2"), "pickaxe", 2);
        tool(enchant(item("tools", "golden-pickaxe", Material.GOLDEN_PICKAXE, 1, Currency.GOLD, 3,
                "<white>Golden Pickaxe"), "EFFICIENCY:3"), "pickaxe", 3);
        tool(enchant(item("tools", "diamond-pickaxe", Material.DIAMOND_PICKAXE, 1, Currency.GOLD, 6,
                "<white>Diamond Pickaxe"), "EFFICIENCY:3"), "pickaxe", 4);
        tool(enchant(lore(item("tools", "wooden-axe", Material.WOODEN_AXE, 1, Currency.IRON, 10,
                "<white>Wooden Axe"),
                "<gray>Step one of the axe, for wood and wool.",
                "<gray>Upgrades through the same button.",
                "<yellow>A death costs one step, never this one."), "EFFICIENCY:1"), "axe", 1);
        tool(enchant(item("tools", "stone-axe", Material.STONE_AXE, 1, Currency.IRON, 10,
                "<white>Stone Axe"), "EFFICIENCY:1"), "axe", 2);
        tool(enchant(item("tools", "iron-axe", Material.IRON_AXE, 1, Currency.GOLD, 3,
                "<white>Iron Axe"), "EFFICIENCY:2"), "axe", 3);
        tool(enchant(item("tools", "diamond-axe", Material.DIAMOND_AXE, 1, Currency.GOLD, 6,
                "<white>Diamond Axe"), "EFFICIENCY:3"), "axe", 4);

        category("potions", "<light_purple>Potions", Material.BREWING_STAND, 6);
        effect(lore(item("potions", "speed-potion", Material.POTION, 1, Currency.EMERALD, 1,
                "<white>Speed II Potion <gray>(45s)"),
                "<gray>Forty-five seconds of outrunning everybody.",
                "<gray>Bought for the trip to the middle, or for",
                "<gray>getting out of one that went wrong."), "SPEED:1:45");
        effect(lore(item("potions", "jump-potion", Material.POTION, 1, Currency.EMERALD, 1,
                "<white>Jump V Potion <gray>(45s)"),
                "<gray>Jump onto anything, and take no fall damage",
                "<gray>getting down again.",
                "<gray>Crosses gaps that have no bridge."), "JUMP_BOOST:4:45");
        effect(lore(item("potions", "invisibility-potion", Material.POTION, 1, Currency.EMERALD, 2,
                "<white>Invisibility Potion <gray>(30s)"),
                "<gray>Thirty seconds of not being there.",
                "<red>Your armour still shows. Take it off first",
                "<red>or you are a floating set of boots."), "INVISIBILITY:0:30");

        category("utility", "<green>Utility", Material.TNT, 7);
        lore(item("utility", "golden-apple", Material.GOLDEN_APPLE, 1, Currency.GOLD, 3,
                "<white>Golden Apple"),
                "<gray>Eaten mid fight: four hearts back and two",
                "<gray>extra on top for two minutes.",
                "<gray>The cheapest way to survive a rush.");
        lore(item("utility", "fireball", Material.FIRE_CHARGE, 1, Currency.IRON, 40, "<white>Fireball"),
                "<gray>Right click to throw. Flies straight,",
                "<gray>explodes on contact and sets nothing alight.",
                "<gray>Breaks what players built - never the map,",
                "<gray>never glass, never end stone.",
                "<yellow>It will not hurt your own team.");
        lore(item("utility", "tnt", Material.TNT, 1, Currency.GOLD, 8, "<white>TNT"),
                "<gray>Lights itself the moment it is placed.",
                "<gray>Two seconds, then a hole big enough to",
                "<gray>walk through.",
                "<red>Blast-proof glass stops it dead.");
        lore(item("utility", "ender-pearl", Material.ENDER_PEARL, 1, Currency.EMERALD, 4,
                "<white>Ender Pearl"),
                "<gray>Straight into a base, over every defence.",
                "<gray>Or straight out of one.",
                "<yellow>A few seconds between two throws, so it is",
                "<yellow>an escape rather than a way of travelling.");
        lore(item("utility", "water-bucket", Material.WATER_BUCKET, 1, Currency.EMERALD, 3,
                "<white>Water Bucket"),
                "<gray>Poured down a wall it stops anybody",
                "<gray>climbing it, and it takes the blast out",
                "<gray>of tnt placed in it.",
                "<gray>Also a way down that does not hurt.");
        lore(item("utility", "magic-milk", Material.MILK_BUCKET, 1, Currency.GOLD, 4,
                "<white>Magic Milk"),
                "<gray>Drink it and the traps of every base ignore",
                "<gray>you for a while.",
                "<gray>What you drink before you go rushing.",
                "<green>Your own potions are not washed off.");
        // what is left of the locator bar once it is switched off: the same information, but it costs
        // something, it has to be held in a hand that could be holding a sword, and every team it is
        // pointed at costs another emerald
        permanent(lore(item("utility", "tracker-compass", Material.COMPASS, 1, Currency.EMERALD, 1,
                "<white>Tracker Compass"),
                "<green>Kept for the whole round, deaths included.",
                "<gray>Hold it and the needle points at the",
                "<gray>nearest player of the team you picked.",
                "<gray>Right click to point it at another team.",
                "<yellow>One emerald per team, the first time only."));
        lore(item("utility", "sponge", Material.SPONGE, 4, Currency.GOLD, 3, "<white>Sponge"),
                "<gray>Drinks a water defence up so you can walk",
                "<gray>through where somebody poured one.");
        lifetime(lore(item("utility", "bedbug", Material.SILVERFISH_SPAWN_EGG, 1, Currency.EMERALD, 2,
                "<white>Bedbug"),
                "<gray>A silverfish on your side for fifteen seconds.",
                "<gray>It does little damage and takes up all of",
                "<gray>somebody's attention, which is the point."), 15);
        lifetime(lore(item("utility", "dream-defender", Material.IRON_GOLEM_SPAWN_EGG, 1, Currency.IRON, 120,
                "<white>Dream Defender"),
                "<gray>An iron golem that guards your base for",
                "<gray>four minutes and hits very hard.",
                "<gray>It never turns on your own team.",
                "<yellow>Needs room: it is three blocks tall."), 240);
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

    /**
     * Writes what an entry is for, above its price.
     * <p>
     * Every entry has one. A shop where half the items are self explanatory and the other half are not is
     * a shop you have to be told about by somebody who already knows it - and the ones that are not self
     * explanatory are exactly the ones that decide rounds.
     *
     * @param path  the entry
     * @param lines what it does, MiniMessage
     * @return the path, so this chains onto the call that created the entry
     */
    private String lore(String path, String... lines) {
        if (!file.contains(path + ".lore")) file.set(path + ".lore", List.of(lines));
        return path;
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
        // an earlier version wrote the swords into the file as a tool chain, which turned the three of
        // them into one button that had to be climbed. They are not a chain, so the keys go again
        file.set(path + ".tool", null);
        file.set(path + ".tool-tier", null);
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
