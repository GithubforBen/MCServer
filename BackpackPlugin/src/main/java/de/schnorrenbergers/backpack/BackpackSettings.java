package de.schnorrenbergers.backpack;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Everything the backpack can be tuned with, backed by {@code ./configs/backpack.yml}.
 * <p>
 * Even the two chest sizes are configurable: the rule is "a team whose paying members are in the majority
 * gets the bigger backpack", but how big the two are is a decision for whoever runs the server.
 */
public class BackpackSettings {

    private final File file;
    private final YamlConfiguration config;

    private int freeSize;
    private int payingSize;
    private String title;
    private boolean announceSize;

    public BackpackSettings(Plugin plugin) {
        file = new File("./configs/backpack.yml");
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    /**
     * Reads the file, writing back anything that was missing so the file documents itself.
     */
    public final void load() {
        freeSize = rows(get("size.default-rows", 3,
                "How many rows a normal team's backpack has. One row is nine slots.",
                "Three rows is a chest."));
        payingSize = rows(get("size.paying-majority-rows", 6,
                "How many rows a team gets once most of its members pay for the server.",
                "Six rows is a double chest."));
        title = get("title", "&6Team-Rucksack &7- &f%team%",
                "The title of the backpack window. %team% is replaced with the team name.");
        announceSize = get("announce-size", true,
                "Whether a team is told why their backpack is the size it is when they open it.");
        save();
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String path, T fallback, String... comments) {
        if (!config.contains(path)) {
            config.set(path, fallback);
            if (comments.length > 0) config.setComments(path, List.of(comments));
            return fallback;
        }
        if (fallback instanceof Integer) return (T) Integer.valueOf(config.getInt(path));
        if (fallback instanceof Boolean) return (T) Boolean.valueOf(config.getBoolean(path));
        if (fallback instanceof String) return (T) config.getString(path, (String) fallback);
        Object value = config.get(path);
        return value == null ? fallback : (T) value;
    }

    /**
     * @param configured how many rows were configured
     * @return that many slots, forced into what minecraft can actually show
     */
    private static int rows(int configured) {
        int clamped = Math.max(1, Math.min(6, configured));
        return clamped * 9;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("[Backpack] Could not save backpack.yml: " + e.getMessage());
        }
    }

    /**
     * @param majorityPaying whether most members of the team pay
     * @return how many slots that team's backpack has
     */
    public int sizeFor(boolean majorityPaying) {
        return majorityPaying ? payingSize : freeSize;
    }

    public int getFreeSize() {
        return freeSize;
    }

    public int getPayingSize() {
        return payingSize;
    }

    public boolean isAnnounceSize() {
        return announceSize;
    }

    /**
     * @param teamName the team the backpack belongs to
     * @return the window title, with colours applied
     */
    public String titleFor(String teamName) {
        return ChatColor.translateAlternateColorCodes('&', title.replace("%team%", teamName));
    }
}
