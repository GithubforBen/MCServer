package de.schnorrenbergers.bedwars.config;

import de.schnorrenbergers.bedwars.util.ConfigFile;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the generators drop and how fast, out of {@code generators.yml}.
 * <p>
 * The hypixel timings are the defaults, but every one of them is a number in a file - a map with wide
 * distances can slow its diamonds down without a second code path. Which types exist is decided here as
 * well: a map naming a type this server does not know is reported rather than silently ignored.
 */
public final class GeneratorSettings {

    /**
     * One kind of generator.
     *
     * @param id           the name maps refer to, e.g. {@code DIAMOND}
     * @param material     what it drops
     * @param displayName  what the floating text calls it
     * @param atTeamBase   whether it belongs to a team rather than standing out in the middle
     * @param splitInBase  whether the drop goes straight to the team standing around it
     * @param amount       how many items per drop
     * @param groundCap    how many may lie around before it stops dropping, 0 for no limit
     * @param splitRadius  how far the split reaches
     * @param hologram     whether it carries floating text with a countdown
     * @param tierSeconds  the delay per level, first entry being level one
     */
    public record Type(String id, Material material, String displayName, boolean atTeamBase, boolean splitInBase,
                       int amount, int groundCap, double splitRadius, boolean hologram, List<Double> tierSeconds) {

        /**
         * @param tier the level the generator has reached, starting at one
         * @return how many seconds between two drops
         */
        public double secondsAt(int tier) {
            if (tierSeconds.isEmpty()) return 30.0d;
            int index = Math.max(0, Math.min(tierSeconds.size() - 1, tier - 1));
            return Math.max(0.05d, tierSeconds.get(index));
        }

        /**
         * @return the highest level this type has timings for
         */
        public int maximumTier() {
            return Math.max(1, tierSeconds.size());
        }
    }

    private final ConfigFile file;
    private final Map<String, Type> types = new LinkedHashMap<>();

    public GeneratorSettings() {
        file = new ConfigFile("generators.yml");
        load();
    }

    /**
     * Reads the file, writing the hypixel defaults into it when they are missing.
     */
    public void load() {
        file.reload();
        types.clear();
        file.section("types",
                "One block per kind of generator. 'seconds' is the delay per level: the first entry is",
                "level one, the second what the first forge upgrade makes of it, and so on.");
        define("IRON", Material.IRON_INGOT, "Iron", true, 1, 48, List.of(1.0d, 0.8d, 0.6d, 0.4d), false);
        define("GOLD", Material.GOLD_INGOT, "Gold", true, 1, 24, List.of(5.0d, 4.0d, 3.0d, 2.0d), false);
        define("EMERALD", Material.EMERALD, "Emerald", false, 1, 4, List.of(60.0d, 40.0d, 30.0d), true);
        define("DIAMOND", Material.DIAMOND, "Diamond", false, 1, 8, List.of(30.0d, 23.0d, 12.0d), true);

        for (String id : file.keys("types")) {
            String path = "types." + id;
            String materialName = file.get(path + ".material", Material.IRON_INGOT.name());
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                Bukkit.getLogger().warning("[Bedwars] generators.yml: '" + materialName
                        + "' of " + id + " is not an item, the generator is skipped.");
                continue;
            }
            List<Double> tiers = new ArrayList<>();
            for (Object entry : file.raw().getList(path + ".seconds", List.of())) {
                if (entry instanceof Number number) tiers.add(number.doubleValue());
            }
            types.put(id.toUpperCase(Locale.ROOT), new Type(
                    id.toUpperCase(Locale.ROOT),
                    material,
                    file.get(path + ".display-name", id),
                    file.get(path + ".at-team-base", false),
                    file.get(path + ".split-in-base", false),
                    Math.max(1, file.get(path + ".amount", 1)),
                    Math.max(0, file.get(path + ".ground-cap", 0)),
                    Math.max(0.0d, file.get(path + ".split-radius", 4.0d)),
                    file.get(path + ".hologram", false),
                    List.copyOf(tiers)));
        }
        file.save();
    }

    /**
     * Writes one of the standard types, without touching what is already in the file.
     */
    private void define(String id, Material material, String displayName, boolean atTeamBase,
                        int amount, int groundCap, List<Double> seconds, boolean hologram) {
        String path = "types." + id;
        file.get(path + ".material", material.name());
        file.get(path + ".display-name", displayName);
        file.get(path + ".at-team-base", atTeamBase);
        file.get(path + ".split-in-base", atTeamBase);
        file.get(path + ".amount", amount);
        file.get(path + ".ground-cap", groundCap);
        file.get(path + ".split-radius", 4.0d);
        file.get(path + ".hologram", hologram);
        if (!file.contains(path + ".seconds")) file.set(path + ".seconds", seconds);
    }

    /**
     * @param id the name a map used
     * @return that type, or {@code null} when this server does not know it
     */
    public Type get(String id) {
        return id == null ? null : types.get(id.toUpperCase(Locale.ROOT));
    }

    /**
     * @return the types that belong to a team base
     */
    public List<Type> teamTypes() {
        return types.values().stream().filter(Type::atTeamBase).toList();
    }

    public Map<String, Type> all() {
        return Map.copyOf(types);
    }
}
