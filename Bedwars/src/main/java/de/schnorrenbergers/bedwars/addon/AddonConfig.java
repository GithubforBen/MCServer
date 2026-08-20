package de.schnorrenbergers.bedwars.addon;

import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.util.ConfigFile;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.List;

/**
 * One addon's own settings, underneath its block in {@code addons.yml}.
 * <p>
 * Every addon reads through this rather than through the file directly, so that its keys land in its own
 * block, are written with a comment the first time, and cannot collide with another addon's. It is the
 * same self documenting config the rest of the plugin uses, only with a prefix.
 */
public final class AddonConfig {

    private final ConfigFile file;
    private final String base;

    /**
     * @param settings the addon file
     * @param addonId  whose settings these are
     */
    public AddonConfig(AddonSettings settings, String addonId) {
        this.file = settings.getFile();
        this.base = AddonSettings.settingsPath(addonId);
    }

    /**
     * @param path     the key underneath this addon's settings
     * @param fallback what it is when the file does not say
     * @param comments what it does, written above it the first time
     * @return the configured value
     */
    public <T> T get(String path, T fallback, String... comments) {
        return file.get(base + "." + path, fallback, comments);
    }

    /**
     * @param path     the key
     * @param fallback what to fall back to
     * @param comments what it does
     * @return the material written there, or the fallback when it names one this server does not have
     */
    public Material material(String path, Material fallback, String... comments) {
        String name = get(path, fallback.name(), comments);
        Material material = Material.matchMaterial(name);
        if (material != null) return material;
        Bukkit.getLogger().warning("[Bedwars] addons.yml: '" + name + "' at " + base + "." + path
                + " is not a material, " + fallback.name() + " is used instead.");
        return fallback;
    }

    /**
     * @param path     the key
     * @param fallback what to fall back to
     * @param comments what it does
     * @return the currency written there, or the fallback when it names one that does not exist
     */
    public Currency currency(String path, Currency fallback, String... comments) {
        Currency currency = Currency.byName(get(path, fallback.name(), comments));
        return currency == null ? fallback : currency;
    }

    /**
     * Writes a list the first time, and reads it every time.
     *
     * @param path     the key
     * @param fallback what to write when it is missing
     * @param comments what it is for
     * @return what the file holds
     */
    public List<String> strings(String path, List<String> fallback, String... comments) {
        String full = base + "." + path;
        if (!file.contains(full)) {
            file.set(full, fallback);
            if (comments.length > 0) file.raw().setComments(full, List.of(comments));
            return fallback;
        }
        return List.copyOf(file.raw().getStringList(full));
    }

    /**
     * @param path     the key
     * @param fallback what to write when it is missing
     * @param comments what it is for
     * @return the numbers the file holds
     */
    public List<Integer> numbers(String path, List<Integer> fallback, String... comments) {
        String full = base + "." + path;
        if (!file.contains(full)) {
            file.set(full, fallback);
            if (comments.length > 0) file.raw().setComments(full, List.of(comments));
            return fallback;
        }
        return List.copyOf(file.raw().getIntegerList(full));
    }

    /**
     * @param path the key
     * @return whether the file has it at all
     */
    public boolean contains(String path) {
        return file.contains(base + "." + path);
    }

    /**
     * Writes everything that was filled in while reading back to disk.
     */
    public void save() {
        file.save();
    }
}
