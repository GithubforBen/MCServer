package de.hems.utils.whitelist;

import de.hems.utils.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The rules of the network and the players who accepted them, which is what puts them on the whitelist.
 * <p>
 * Kept apart from the names the admins type into {@code main-config.yml}: those are a handful, these can
 * be every player the network ever had, and they are stored by uuid so a server start does not have to ask
 * mojang about each of them again. Which version of the rules somebody accepted is kept as a hash, so it
 * can be told afterwards whether they saw the rules as they are now.
 */
public class WhitelistStore {

    /** What a fresh network shows until an admin writes its own rules. */
    public static final String DEFAULT_RULES = """
            1. Sei freundlich zu allen - keine Beleidigungen, kein Spam, keine Hetze.
            2. Kein Griefing und kein Stehlen: was andere gebaut haben, bleibt, wie es ist.
            3. Keine Hacks, Cheat-Clients, X-Ray oder Exploits.
            4. Die Anweisungen der Admins gelten.

            Wer sich nicht daran hält, fliegt von der Whitelist.""";

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Entry> players = new ConcurrentHashMap<>();

    /**
     * Somebody who put themselves on the whitelist.
     *
     * @param uuid       the minecraft account
     * @param name       its name when it was added
     * @param acceptedAt when the rules were accepted
     * @param rules      the hash of the rules that were accepted
     */
    public record Entry(UUID uuid, String name, long acceptedAt, String rules) {
    }

    public WhitelistStore() {
        this(new File("./whitelist.yml"));
    }

    public WhitelistStore(File file) {
        this.file = file;
        this.config = YamlFiles.load(file);
        load();
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                players.put(uuid, new Entry(uuid, section.getString(key + ".name", "?"),
                        section.getLong(key + ".accepted-at"), section.getString(key + ".rules", "")));
            } catch (IllegalArgumentException ignored) {
                // one unreadable entry costs one player, not the list
            }
        }
        System.out.println("Loaded " + players.size() + " self-whitelisted players from " + file.getName());
    }

    private synchronized void save() {
        YamlFiles.saveOrLog(config, file);
    }

    /* ------------------------------------------------------------------ settings */

    /**
     * @return whether the servers only let in who is on the whitelist. Off until an admin switches it on,
     * because switching it on locks out everybody who is not on it yet
     */
    public boolean isEnforced() {
        return config.getBoolean("enforced", false);
    }

    public synchronized void setEnforced(boolean enforced) {
        config.set("enforced", enforced);
        save();
    }

    /**
     * @return whether players can put themselves on the whitelist through the rules page
     */
    public boolean isSelfService() {
        return config.getBoolean("self-service", true);
    }

    public synchronized void setSelfService(boolean selfService) {
        config.set("self-service", selfService);
        save();
    }

    public String getRules() {
        return config.getString("rules", DEFAULT_RULES);
    }

    public synchronized void setRules(String rules) {
        config.set("rules", rules);
        save();
    }

    /**
     * @return the address of the rules page as players reach it, or {@code null} to work it out from the
     * address of the network and the port of the website
     */
    public String getPublicUrl() {
        String url = config.getString("public-url");
        return url == null || url.isBlank() ? null : url.trim();
    }

    public synchronized void setPublicUrl(String url) {
        config.set("public-url", url == null || url.isBlank() ? null : url.trim());
        save();
    }

    /**
     * @return a short fingerprint of the rules as they are now
     */
    public String rulesHash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(getRules().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /* ------------------------------------------------------------------ players */

    public boolean contains(UUID uuid) {
        return players.containsKey(uuid);
    }

    /**
     * @return everybody who accepted the rules, newest first
     */
    public List<Entry> list() {
        List<Entry> list = new ArrayList<>(players.values());
        list.sort(Comparator.comparingLong(Entry::acceptedAt).reversed());
        return list;
    }

    /**
     * Puts somebody on the whitelist, or renews the date and rules of somebody who is on it already.
     *
     * @param uuid the minecraft account
     * @param name its name as mojang writes it
     * @return the entry
     */
    public synchronized Entry add(UUID uuid, String name) {
        Entry entry = new Entry(uuid, name, System.currentTimeMillis(), rulesHash());
        players.put(uuid, entry);
        String path = "players." + uuid;
        config.set(path + ".name", entry.name());
        config.set(path + ".accepted-at", entry.acceptedAt());
        config.set(path + ".rules", entry.rules());
        save();
        return entry;
    }

    /**
     * @param uuid the minecraft account
     * @return the entry that was removed, or {@code null} if there was none
     */
    public synchronized Entry remove(UUID uuid) {
        Entry entry = players.remove(uuid);
        if (entry == null) return null;
        config.set("players." + uuid, null);
        save();
        return entry;
    }
}
