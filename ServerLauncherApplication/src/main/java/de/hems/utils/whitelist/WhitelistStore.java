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
            MC-Server Regelwerk

            §0

            Der Server dient der Unterhaltung und dem Spaß aller Teilnehmer, welcher nicht durch andere Spieler \
            beeinträchtigt werden sollte. Um dies zu gewähren, besteht dieses Regelwerk. Die Regeln gelten für \
            jeden, treten aber erst in Kraft, wenn sich ein unfairer Vorteil erschafft wurde, oder eine Person \
            zu Schaden gekommen ist, die sich beschwert. Wenn sich niemand bei den Admins (Rolle Österreicher auf \
            Discord) beschwert, besteht auch kein Grund zur Bestrafung. Allerdings können auch die Admins selbst \
            eine Beschwerde aufnehmen, das kann zum Beispiel der Fall bei X-Rayen sein.

            §1 - Kein Base-/Chunk-Griefing*

            Griefing an Basen / in geclaimten Chunks (was zu großen/bleibenden/nicht reparierbarem Schaden führt) \
            ist verboten.

            Bestrafung: 12 Stunden - permanenter Bann

            §2 - Cheaten

            §2.1 - Hacken

            Cheaten jeglicher Art (Flyhacks, Rangehacks, ...) ist verboten, wenn es einen unfairen Vorteil \
            verschafft.

            Bestrafung: 12 Stunden - permanenter Bann

            §2.2 - Duping

            Duping jeglicher Art (außer TNT-, Sand-, Carpet- und Gravel-Duper, etc.) ist verboten.

            Bestrafung: Wipe - Base Wipe / 12 Stunden - 24 Stunden Bann

            §2.3 - X-Ray

            X-Ray und Chunk Reloading ist für jeglichen Zweck verboten. Außerdem muss die Freecam-Mod (falls \
            benutzt) auf "Collision on" gespielt werden.

            Bestrafung: Wipe + 12 Stunden - 48 Stunden Bann

            §3 - PVP

            Die PVP-Regeln können auf Absprache mit allen kämpfenden Parteien ausgesetzt werden. Auf \
            wiederholende, unabgesprochene Regelbrüche werden Bestrafungen durchgeführt.

            §3.1 - Spawncamping*

            Spawncamping ist bis zu 5-mal erlaubt. Danach ist es verboten.

            Bestrafung: variiert je nach Situation (Einschätzung der Admins)

            §3.2 - Sukzessives Töten*

            Sukzessives Töten anderer Spieler im Allgemeinen sollte vermieden, und nicht übertrieben werden.

            Bestrafung: variiert je nach Situation (Einschätzung der Admins)

            §4 - Stehlen

            Stehlen von Items/Stuff aus Basen / Farmen ist verboten.

            Bestrafung: variiert je nach Situation (Einschätzung der Admins)

            §5 - Verbindungsoptionen

            Die Server-Internetprotokoll-Adresse ist << mc.samiuen.com >>.

            §6 - Definitionen

            §6.0 - Bestrafungen

            Alle Bestrafungen sind nur grobe Richtlinien und können kleiner, aber auch größer ausfallen.

            §6.1 - Griefen*

            Mutwilliges Zerstören von Blöcken, Entitys usw.

            §6.2 - Spawncamping*

            Das Töten eines Spielers direkt, nachdem dieser gestorben ist, ohne dass er die Möglichkeit hatte \
            zu kämpfen oder sich aus der Situation zu befreien.

            §6.3 - Sukzessives Töten*

            Generell andere Spieler (nicht unbedingt nur einen) wahllos töten, so dass man das Spielerlebnis \
            des anderen zerstört.""";


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
