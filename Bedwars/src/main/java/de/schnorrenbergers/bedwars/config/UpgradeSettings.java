package de.schnorrenbergers.bedwars.config;

import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.shop.trap.Trap;
import de.schnorrenbergers.bedwars.shop.upgrade.Upgrade;
import de.schnorrenbergers.bedwars.util.ConfigFile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The team upgrades and the traps, out of {@code upgrades.yml}.
 * <p>
 * Both live in one file because both are bought at the same villager and paid for out of the same team
 * pocket. The trap prices are the exception to everything else in this plugin being per entry: what a trap
 * costs depends on how many are already queued, not on which trap it is.
 */
public final class UpgradeSettings {

    private final ConfigFile file;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();
    private final Map<String, Trap> traps = new LinkedHashMap<>();

    private int queueSize;
    private List<Integer> trapPrices = List.of(1, 2, 4);
    private Currency trapCurrency = Currency.DIAMOND;
    private double trapRadius;
    private int trapImmunitySeconds;
    private int trapCooldownSeconds;

    public UpgradeSettings() {
        file = new ConfigFile("upgrades.yml");
        load();
    }

    /**
     * Reads the file, writing the hypixel upgrades and traps into it the first time.
     */
    public void load() {
        file.reload();
        upgrades.clear();
        traps.clear();
        file.section("upgrades",
                "What a team can buy for the whole team. 'prices' is one price per level.",
                "'effect' is what buying it does and has to be one of:",
                "  SHARPNESS, PROTECTION, HASTE, FORGE, HEAL_POOL, DRAGON_BUFF, NONE");
        writeUpgradeDefaults();
        writeTrapDefaults();
        readUpgrades();
        readTraps();
        file.save();
    }

    // ----------------------------------------------------------------- defaults

    private void writeUpgradeDefaults() {
        upgrade("sharpened-swords", "<aqua>Sharpened Swords", Material.IRON_SWORD, 19,
                Upgrade.Effect.SHARPNESS, 1, List.of(4),
                "<gray>Every sword and axe of your team is sharpened.");
        upgrade("reinforced-armor", "<aqua>Reinforced Armor", Material.IRON_CHESTPLATE, 20,
                Upgrade.Effect.PROTECTION, 4, List.of(2, 4, 8, 16),
                "<gray>Your team's armour gains protection.");
        upgrade("maniac-miner", "<aqua>Maniac Miner", Material.GOLDEN_PICKAXE, 21,
                Upgrade.Effect.HASTE, 2, List.of(2, 4),
                "<gray>Your team digs faster.");
        upgrade("iron-forge", "<aqua>Iron Forge", Material.FURNACE, 22,
                Upgrade.Effect.FORGE, 4, List.of(2, 4, 6, 8),
                "<gray>Your base generator runs faster.");
        upgrade("heal-pool", "<aqua>Heal Pool", Material.BEACON, 23,
                Upgrade.Effect.HEAL_POOL, 1, List.of(1),
                "<gray>Your team regenerates while at your base.");
        upgrade("dragon-buff", "<aqua>Dragon Buff", Material.DRAGON_EGG, 24,
                Upgrade.Effect.DRAGON_BUFF, 1, List.of(5),
                "<gray>Your team gets a second dragon in sudden death.");
    }

    private void writeTrapDefaults() {
        file.section("traps",
                "Traps wait in a queue and go off when an enemy walks into your base.",
                "The price depends on how many are already queued, not on which trap it is.",
                "'effect' has to be one of: BLINDNESS, MINING_FATIGUE, COUNTER_OFFENSIVE, ALARM, NONE");
        file.get("traps.queue-size", 3, "How many traps a team can have waiting at once.");
        if (!file.contains("traps.prices")) file.set("traps.prices", List.of(1, 2, 4));
        file.get("traps.currency", Currency.DIAMOND.name());
        file.get("traps.radius", 0.0d,
                "How far from the team spawn a trap goes off.",
                "0 means: as far as that team's protection radius reaches on this map.");
        file.get("traps.immunity-seconds", 30,
                "How long magic milk keeps traps from going off for whoever drank it.");
        file.get("traps.cooldown-seconds", 5,
                "How long a team's next trap waits after one went off.",
                "Without it, an enemy standing in a base sets off the whole queue in a second.");

        trap("its-a-trap", "<red>It's a Trap!", Material.TRIPWIRE_HOOK, 37,
                Trap.Effect.BLINDNESS, 8, 0,
                "<gray>Blinds and slows the enemy who set it off.");
        trap("counter-offensive", "<red>Counter-Offensive Trap", Material.FEATHER, 38,
                Trap.Effect.COUNTER_OFFENSIVE, 15, 1,
                "<gray>Your team at home gets speed and jump boost.");
        trap("alarm-trap", "<red>Alarm Trap", Material.REDSTONE_TORCH, 39,
                Trap.Effect.ALARM, 5, 0,
                "<gray>Reveals invisible enemies and names them.");
        trap("miner-fatigue", "<red>Miner Fatigue Trap", Material.IRON_PICKAXE, 40,
                Trap.Effect.MINING_FATIGUE, 10, 0,
                "<gray>The enemy who set it off cannot mine.");
    }

    private void upgrade(String id, String displayName, Material icon, int slot, Upgrade.Effect effect,
                         int maxLevel, List<Integer> prices, String... lore) {
        String path = "upgrades." + id;
        file.get(path + ".display-name", displayName);
        file.get(path + ".icon", icon.name());
        file.get(path + ".slot", slot);
        file.get(path + ".effect", effect.name());
        file.get(path + ".max-level", maxLevel);
        file.get(path + ".currency", Currency.DIAMOND.name());
        if (!file.contains(path + ".prices")) file.set(path + ".prices", prices);
        if (!file.contains(path + ".lore")) file.set(path + ".lore", List.of(lore));
    }

    private void trap(String id, String displayName, Material icon, int slot, Trap.Effect effect,
                      int seconds, int amplifier, String... lore) {
        String path = "traps.entries." + id;
        file.get(path + ".display-name", displayName);
        file.get(path + ".icon", icon.name());
        file.get(path + ".slot", slot);
        file.get(path + ".effect", effect.name());
        file.get(path + ".seconds", seconds);
        file.get(path + ".amplifier", amplifier);
        if (!file.contains(path + ".lore")) file.set(path + ".lore", List.of(lore));
    }

    // ------------------------------------------------------------------ reading

    private void readUpgrades() {
        List<Upgrade> read = new ArrayList<>();
        for (String id : file.keys("upgrades")) {
            String path = "upgrades." + id;
            Material icon = material(file.read(path + ".icon", Material.STONE.name()), id);
            Currency currency = Currency.byName(file.read(path + ".currency", Currency.DIAMOND.name()));
            List<Integer> prices = new ArrayList<>();
            for (Object entry : file.raw().getList(path + ".prices", List.of())) {
                if (entry instanceof Number number) prices.add(number.intValue());
            }
            read.add(new Upgrade(id,
                    file.read(path + ".display-name", id),
                    icon == null ? Material.STONE : icon,
                    file.read(path + ".slot", -1),
                    Upgrade.Effect.byName(file.read(path + ".effect", Upgrade.Effect.NONE.name())),
                    Math.max(1, file.read(path + ".max-level", 1)),
                    List.copyOf(prices),
                    currency == null ? Currency.DIAMOND : currency,
                    List.copyOf(file.raw().getStringList(path + ".lore"))));
        }
        read.sort(Comparator.comparingInt(upgrade -> upgrade.slot() < 0 ? Integer.MAX_VALUE : upgrade.slot()));
        read.forEach(upgrade -> upgrades.put(upgrade.id(), upgrade));
    }

    private void readTraps() {
        queueSize = Math.max(0, file.read("traps.queue-size", 3));
        List<Integer> prices = new ArrayList<>();
        for (Object entry : file.raw().getList("traps.prices", List.of())) {
            if (entry instanceof Number number) prices.add(Math.max(0, number.intValue()));
        }
        if (!prices.isEmpty()) trapPrices = List.copyOf(prices);
        Currency currency = Currency.byName(file.read("traps.currency", Currency.DIAMOND.name()));
        if (currency != null) trapCurrency = currency;
        trapRadius = Math.max(0.0d, file.read("traps.radius", 0.0d));
        trapImmunitySeconds = Math.max(0, file.read("traps.immunity-seconds", 30));
        trapCooldownSeconds = Math.max(0, file.read("traps.cooldown-seconds", 5));

        List<Trap> read = new ArrayList<>();
        for (String id : file.keys("traps.entries")) {
            String path = "traps.entries." + id;
            Material icon = material(file.read(path + ".icon", Material.STONE.name()), id);
            read.add(new Trap(id,
                    file.read(path + ".display-name", id),
                    icon == null ? Material.STONE : icon,
                    Trap.Effect.byName(file.read(path + ".effect", Trap.Effect.NONE.name())),
                    Math.max(1, file.read(path + ".seconds", 8)),
                    Math.max(0, file.read(path + ".amplifier", 0)),
                    List.copyOf(file.raw().getStringList(path + ".lore"))));
        }
        read.sort(Comparator.comparingInt(trap -> slotOf(trap.id())));
        read.forEach(trap -> traps.put(trap.id(), trap));
    }

    /**
     * @param id a trap
     * @return where it sits in the menu, at the end when it says nothing
     */
    public int slotOf(String id) {
        int slot = file.read("traps.entries." + id + ".slot", -1);
        return slot < 0 ? Integer.MAX_VALUE : slot;
    }

    // ------------------------------------------------------------------ lookups

    public @Nullable Upgrade getUpgrade(String id) {
        return id == null ? null : upgrades.get(id);
    }

    public List<Upgrade> getUpgrades() {
        return List.copyOf(upgrades.values());
    }

    public @Nullable Trap getTrap(String id) {
        return id == null ? null : traps.get(id);
    }

    public List<Trap> getTraps() {
        return List.copyOf(traps.values());
    }

    public int getQueueSize() {
        return queueSize;
    }

    /**
     * @param queued how many traps the team already has waiting
     * @return what the next one costs
     */
    public int getTrapPrice(int queued) {
        if (trapPrices.isEmpty()) return 0;
        int index = Math.max(0, Math.min(trapPrices.size() - 1, queued));
        return trapPrices.get(index);
    }

    public Currency getTrapCurrency() {
        return trapCurrency;
    }

    /**
     * @param protection how far the team's base is protected on this map
     * @return how far from the spawn a trap goes off
     */
    public double getTrapRadius(double protection) {
        return trapRadius > 0.0d ? trapRadius : protection;
    }

    public int getTrapImmunitySeconds() {
        return trapImmunitySeconds;
    }

    /**
     * @return how long a team's next trap waits after one of them went off
     */
    public int getTrapCooldownSeconds() {
        return trapCooldownSeconds;
    }

    public ConfigFile getFile() {
        return file;
    }

    private static @Nullable Material material(String name, String what) {
        Material material = Material.matchMaterial(name);
        if (material == null) {
            Bukkit.getLogger().warning("[Bedwars] upgrades.yml: '" + name + "' of " + what
                    + " is not a material, stone is used instead.");
        }
        return material;
    }
}
