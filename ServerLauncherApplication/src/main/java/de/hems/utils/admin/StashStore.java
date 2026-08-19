package de.hems.utils.admin;

import de.hems.types.admin.ItemData;
import de.hems.types.admin.StashData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the admin stash lives.
 * <p>
 * It sits on the launcher rather than on a game server for the same reason the teams do: the website and
 * whichever server the admin happens to be standing on both have to reach the same chest.
 * <p>
 * The launcher never looks inside the contents - it has no bukkit to make sense of them with - it only
 * stores the bytes and hands them back.
 */
public class StashStore {

    /** How big a stash is unless it was stored with another size. */
    private static final int DEFAULT_SIZE = 54;

    private final File file;
    private final YamlConfiguration config;
    private final Map<String, StashData> stashes = new ConcurrentHashMap<>();

    public StashStore() {
        this(new File("./stashes.yml"));
    }

    public StashStore(File file) {
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
        ConfigurationSection section = config.getConfigurationSection("stashes");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            stashes.put(key.toLowerCase(Locale.ROOT), new StashData(
                    entry.getString("id", key),
                    entry.getInt("size", DEFAULT_SIZE),
                    readItems(entry),
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
     * Reads a stash, creating an empty one the first time it is opened.
     *
     * @param id which stash
     * @return it, never {@code null}
     */
    public synchronized StashData get(String id) {
        String key = key(id);
        StashData stash = stashes.get(key);
        if (stash != null) return stash;
        stash = new StashData(id == null ? StashData.GLOBAL : id, DEFAULT_SIZE, new ArrayList<>(), 0L);
        stashes.put(key, stash);
        write(key, stash);
        save();
        return stash;
    }

    /**
     * Writes a stash back.
     *
     * @param stash the stash, carrying the revision it was read at
     * @return what happened
     */
    public synchronized Result put(StashData stash) {
        if (stash == null) return new Result(false, 0L, "Es wurde keine Ablage übergeben.");
        String key = key(stash.getId());
        StashData stored = stashes.get(key);
        if (stored != null && stored.getRevision() != stash.getRevision()) {
            return new Result(false, stored.getRevision(),
                    "Die Ablage wurde inzwischen woanders geändert. Deine Änderungen wurden nicht gespeichert.");
        }
        stash.setRevision(stash.getRevision() + 1);
        stash.setSize(normalize(stash.getSize()));
        stashes.put(key, stash);
        write(key, stash);
        save();
        return new Result(true, stash.getRevision(), "Ablage gespeichert.");
    }

    private void write(String key, StashData stash) {
        String path = "stashes." + key;
        config.set(path + ".id", stash.getId());
        config.set(path + ".size", stash.getSize());
        config.set(path + ".revision", stash.getRevision());
        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemData item : stash.getItems()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("slot", item.getSlot());
            entry.put("material", item.getMaterial());
            entry.put("amount", item.getAmount());
            if (item.getRawBase64() != null) entry.put("raw", item.getRawBase64());
            items.add(entry);
        }
        config.set(path + ".items", items.isEmpty() ? null : items);
    }

    /**
     * Reads the slots of a stash back out of the config.
     *
     * @param entry the section the stash is stored in
     * @return its items, skipping anything that is no longer readable
     */
    private static List<ItemData> readItems(ConfigurationSection entry) {
        List<ItemData> items = new ArrayList<>();
        for (Map<?, ?> stored : entry.getMapList("items")) {
            Object material = stored.get("material");
            if (material == null) continue;
            ItemData item = new ItemData();
            item.setSlot(stored.get("slot") instanceof Number slot ? slot.intValue() : -1);
            item.setMaterial(String.valueOf(material));
            item.setAmount(stored.get("amount") instanceof Number amount ? amount.intValue() : 1);
            Object raw = stored.get("raw");
            if (raw != null) {
                try {
                    item.setRawBase64(String.valueOf(raw));
                } catch (IllegalArgumentException e) {
                    // a mangled blob only costs this item its extra data, not the whole stash
                }
            }
            if (item.getSlot() >= 0) items.add(item);
        }
        return items;
    }

    /**
     * @param size the size that was asked for
     * @return that size rounded to whole rows, within what a chest window can show
     */
    private static int normalize(int size) {
        int rows = Math.max(1, Math.min(6, (size + 8) / 9));
        return rows * 9;
    }

    private static String key(String id) {
        return (id == null || id.isBlank() ? StashData.GLOBAL : id).toLowerCase(Locale.ROOT);
    }

    /**
     * How a write ended.
     *
     * @param successful whether it was stored
     * @param revision   the revision that is stored now
     * @param message    what to tell the caller
     */
    public record Result(boolean successful, long revision, String message) {
    }
}
