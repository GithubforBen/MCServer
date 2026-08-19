package de.hems.utils.team;

import de.hems.types.team.BackpackData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the shared team backpacks live.
 * <p>
 * Kept next to the teams on the launcher rather than on a game server, so a team's backpack is the same one
 * no matter which server its members are playing on.
 * <p>
 * The contents are the bytes bukkit serialises items into, stored as base64. The launcher never looks
 * inside them - it has no bukkit to make sense of them with - it only hands them back out again.
 */
public class BackpackStore {

    private final File file;
    private final YamlConfiguration config;
    private final Map<String, BackpackData> backpacks = new ConcurrentHashMap<>();

    public BackpackStore() {
        this(new File("./backpacks.yml"));
    }

    public BackpackStore(File file) {
        this.file = file;
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("backpacks");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            byte[] contents = null;
            String encoded = entry.getString("contents");
            if (encoded != null && !encoded.isBlank()) {
                try {
                    contents = Base64.getDecoder().decode(encoded);
                } catch (IllegalArgumentException e) {
                    System.out.println("The backpack of " + key + " is unreadable and starts empty.");
                }
            }
            backpacks.put(key.toLowerCase(Locale.ROOT), new BackpackData(
                    entry.getString("team", key),
                    entry.getInt("size", BackpackData.SINGLE_CHEST),
                    contents,
                    entry.getLong("revision", 0L)));
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Reads a team's backpack, creating an empty one the first time it is opened.
     *
     * @param teamName   the team
     * @param wantedSize how big it should be, worked out from how many members pay
     * @return the backpack
     */
    public synchronized BackpackData get(String teamName, int wantedSize) {
        String key = teamName.toLowerCase(Locale.ROOT);
        BackpackData backpack = backpacks.get(key);
        if (backpack == null) {
            backpack = new BackpackData(teamName, normalize(wantedSize), null, 0L);
            backpacks.put(key, backpack);
            write(key, backpack);
            save();
            return backpack;
        }
        // the team may have grown or lost paying members since the backpack was last opened
        int size = normalize(wantedSize);
        if (size != backpack.getSize()) {
            backpack.setSize(size);
            write(key, backpack);
            save();
        }
        return backpack;
    }

    /**
     * Writes a backpack back.
     *
     * @param backpack the backpack, carrying the revision it was opened at
     * @return what happened
     */
    public synchronized Result put(BackpackData backpack) {
        if (backpack == null || backpack.getTeamName() == null) {
            return new Result(false, 0L, "Der Rucksack gehört zu keinem Team.");
        }
        String key = backpack.getTeamName().toLowerCase(Locale.ROOT);
        BackpackData stored = backpacks.get(key);
        if (stored != null && stored.getRevision() != backpack.getRevision()) {
            return new Result(false, stored.getRevision(),
                    "Der Rucksack wurde inzwischen woanders geändert. Deine Änderungen wurden nicht gespeichert.");
        }
        backpack.setRevision(backpack.getRevision() + 1);
        backpack.setSize(normalize(backpack.getSize()));
        backpacks.put(key, backpack);
        write(key, backpack);
        save();
        return new Result(true, backpack.getRevision(), "Rucksack gespeichert.");
    }

    /**
     * @param teamName the team whose backpack to drop, used when a team is disbanded
     */
    public synchronized void delete(String teamName) {
        if (teamName == null) return;
        String key = teamName.toLowerCase(Locale.ROOT);
        if (backpacks.remove(key) == null) return;
        config.set("backpacks." + key, null);
        save();
    }

    private void write(String key, BackpackData backpack) {
        String path = "backpacks." + key;
        config.set(path + ".team", backpack.getTeamName());
        config.set(path + ".size", backpack.getSize());
        config.set(path + ".revision", backpack.getRevision());
        config.set(path + ".contents", backpack.getContents() == null
                ? null : Base64.getEncoder().encodeToString(backpack.getContents()));
    }

    /**
     * @param size the size that was asked for
     * @return the nearest size minecraft can actually show as a chest
     */
    private static int normalize(int size) {
        return size >= BackpackData.DOUBLE_CHEST ? BackpackData.DOUBLE_CHEST : BackpackData.SINGLE_CHEST;
    }

    /**
     * How a write ended.
     *
     * @param successful whether it was stored
     * @param revision   the revision that is stored now
     * @param message    what to tell the player
     */
    public record Result(boolean successful, long revision, String message) {
    }
}
