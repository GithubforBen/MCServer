package de.schnorrenbergers.hungergames;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What lies in the chests.
 * <p>
 * Three tables: ordinary chests out on the map, the cornucopia in the middle - better, which is the reason
 * to run into the fight at the start - and the supply packages, the best of the three, which is the reason
 * to leave cover later on.
 * <p>
 * The tables live in {@code hungergames-loot.yml} next to the launcher, so a change there holds for every
 * arena from then on. An arena is a fresh server each time and a file on it would be thrown away with it.
 * When the file is missing it is written with the defaults, which are also what is used when it cannot be
 * read.
 */
public final class Loot {

    /** Where the tables live, relative to a server directory. */
    private static final String SHARED_FILE = "../../hungergames-loot.yml";

    /** Which table a chest is filled from. */
    public enum Tier {
        NORMAL("normal", 3, 6),
        CORNUCOPIA("cornucopia", 5, 8),
        SUPPLY("supply", 6, 9);

        private final String key;
        private final int minStacks;
        private final int maxStacks;

        Tier(String key, int minStacks, int maxStacks) {
            this.key = key;
            this.minStacks = minStacks;
            this.maxStacks = maxStacks;
        }
    }

    /** One line of a table. */
    private record Entry(Material material, int min, int max, int weight, Map<Enchantment, Integer> enchantments) {

        ItemStack roll() {
            int amount = min >= max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
            ItemStack stack = new ItemStack(material, Math.max(1, Math.min(amount, material.getMaxStackSize())));
            for (Map.Entry<Enchantment, Integer> enchantment : enchantments.entrySet()) {
                stack.addUnsafeEnchantment(enchantment.getKey(), enchantment.getValue());
            }
            return stack;
        }
    }

    private static final Map<Tier, List<Entry>> tables = new EnumMap<>(Tier.class);

    private Loot() {
    }

    /**
     * Reads the tables, writing the defaults first if there are none yet.
     *
     * @param plugin the plugin, for its logger
     */
    public static void load(Plugin plugin) {
        File file = new File(SHARED_FILE);
        if (!file.isFile()) {
            try {
                defaults().save(file);
                plugin.getLogger().info("Wrote the default loot to " + file.getPath() + " - edit it there.");
            } catch (IOException e) {
                plugin.getLogger().warning("Could not write the default loot (" + e.getMessage()
                        + ") - the defaults are used.");
            }
        }
        YamlConfiguration config = file.isFile() ? YamlConfiguration.loadConfiguration(file) : defaults();
        for (Tier tier : Tier.values()) {
            List<Entry> entries = read(plugin, config.getMapList(tier.key));
            if (entries.isEmpty()) {
                plugin.getLogger().warning("The loot table '" + tier.key + "' is empty or unreadable - "
                        + "the default is used.");
                entries = read(plugin, defaults().getMapList(tier.key));
            }
            tables.put(tier, entries);
        }
    }

    private static List<Entry> read(Plugin plugin, List<Map<?, ?>> lines) {
        List<Entry> entries = new ArrayList<>();
        for (Map<?, ?> line : lines) {
            Object name = line.get("material");
            Material material = name == null ? null : Material.matchMaterial(name.toString());
            if (material == null || material.isAir() || !material.isItem()) {
                plugin.getLogger().warning("Unknown loot item: " + name);
                continue;
            }
            int min = number(line.get("min"), 1);
            int max = number(line.get("max"), min);
            int weight = number(line.get("weight"), 1);
            Map<Enchantment, Integer> enchantments = new java.util.LinkedHashMap<>();
            Object listed = line.get("enchantments");
            if (listed instanceof List<?> list) {
                for (Object spec : list) {
                    String[] parts = spec.toString().toLowerCase(Locale.ROOT).split(":");
                    Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(parts[0].trim()));
                    if (enchantment == null) {
                        plugin.getLogger().warning("Unknown enchantment in the loot: " + spec);
                        continue;
                    }
                    enchantments.put(enchantment, parts.length > 1 ? number(parts[1].trim(), 1) : 1);
                }
            }
            if (weight > 0) entries.add(new Entry(material, Math.max(1, min), Math.max(1, max), weight, enchantments));
        }
        return entries;
    }

    private static int number(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Empties an inventory and fills it from a table, the stacks in random slots.
     *
     * @param inventory the chest
     * @param tier      the table
     */
    public static void fill(Inventory inventory, Tier tier) {
        List<Entry> entries = tables.get(tier);
        inventory.clear();
        if (entries == null || entries.isEmpty()) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int total = 0;
        for (Entry entry : entries) total += entry.weight();
        int stacks = random.nextInt(tier.minStacks, tier.maxStacks + 1);
        List<Integer> free = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) free.add(slot);
        for (int i = 0; i < stacks && !free.isEmpty(); i++) {
            int pick = random.nextInt(total);
            for (Entry entry : entries) {
                pick -= entry.weight();
                if (pick >= 0) continue;
                inventory.setItem(free.remove(random.nextInt(free.size())), entry.roll());
                break;
            }
        }
    }

    /**
     * @return the tables the network ships with
     */
    private static YamlConfiguration defaults() {
        YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(List.of(
                "Loot for hunger games. Every arena reads this file when it starts.",
                "material: the item, min/max: how many, weight: how often compared to the rest,",
                "enchantments: optional list like [sharpness:1, protection:2]"));
        config.set("normal", List.of(
                line("WOODEN_SWORD", 1, 1, 8), line("STONE_SWORD", 1, 1, 5), line("STONE_AXE", 1, 1, 4),
                line("BOW", 1, 1, 3), line("ARROW", 3, 8, 6), line("LEATHER_HELMET", 1, 1, 4),
                line("LEATHER_CHESTPLATE", 1, 1, 4), line("LEATHER_LEGGINGS", 1, 1, 4),
                line("LEATHER_BOOTS", 1, 1, 4), line("CHAINMAIL_HELMET", 1, 1, 2),
                line("CHAINMAIL_BOOTS", 1, 1, 2), line("BREAD", 1, 3, 8), line("APPLE", 1, 3, 7),
                line("COOKED_BEEF", 1, 2, 5), line("CARROT", 2, 4, 5), line("STICK", 1, 3, 4),
                line("FLINT", 1, 2, 3), line("FEATHER", 1, 3, 3), line("IRON_INGOT", 1, 2, 3),
                line("OAK_PLANKS", 4, 12, 5), line("COBWEB", 1, 2, 2), line("FISHING_ROD", 1, 1, 2)));
        config.set("cornucopia", List.of(
                line("IRON_SWORD", 1, 1, 6), line("STONE_SWORD", 1, 1, 5), line("IRON_AXE", 1, 1, 3),
                line("BOW", 1, 1, 5), line("ARROW", 6, 16, 7), line("CROSSBOW", 1, 1, 2),
                line("IRON_HELMET", 1, 1, 4), line("IRON_CHESTPLATE", 1, 1, 3), line("IRON_LEGGINGS", 1, 1, 3),
                line("IRON_BOOTS", 1, 1, 4), line("CHAINMAIL_CHESTPLATE", 1, 1, 4), line("SHIELD", 1, 1, 3),
                line("COOKED_BEEF", 2, 5, 6), line("GOLDEN_CARROT", 2, 4, 4), line("GOLDEN_APPLE", 1, 1, 2),
                line("IRON_INGOT", 2, 4, 4), line("DIAMOND", 1, 1, 1), line("COBWEB", 2, 4, 3),
                line("WATER_BUCKET", 1, 1, 2), line("ENDER_PEARL", 1, 1, 1)));
        config.set("supply", List.of(
                lineEnchanted("DIAMOND_SWORD", 1, 3, List.of("sharpness:1")), line("IRON_SWORD", 1, 1, 4),
                lineEnchanted("BOW", 1, 3, List.of("power:2")), line("ARROW", 12, 24, 5),
                line("DIAMOND_HELMET", 1, 1, 2), line("DIAMOND_CHESTPLATE", 1, 1, 1),
                line("DIAMOND_LEGGINGS", 1, 1, 1), line("DIAMOND_BOOTS", 1, 1, 2),
                lineEnchanted("IRON_CHESTPLATE", 1, 3, List.of("protection:2")),
                line("GOLDEN_APPLE", 1, 2, 5), line("ENDER_PEARL", 1, 2, 3), line("COOKED_BEEF", 4, 8, 4),
                line("LAVA_BUCKET", 1, 1, 2), line("TNT", 1, 2, 2),
                line("FLINT_AND_STEEL", 1, 1, 2), line("EXPERIENCE_BOTTLE", 3, 8, 2)));
        return config;
    }

    private static Map<String, Object> line(String material, int min, int max, int weight) {
        Map<String, Object> line = new java.util.LinkedHashMap<>();
        line.put("material", material);
        line.put("min", min);
        line.put("max", max);
        line.put("weight", weight);
        return line;
    }

    private static Map<String, Object> lineEnchanted(String material, int min, int weight, List<String> enchantments) {
        Map<String, Object> line = line(material, min, min, weight);
        line.put("enchantments", enchantments);
        return line;
    }
}
