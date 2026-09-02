package de.hems.utils.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticSnapshot;
import de.hems.types.cosmetic.CosmeticType;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.PlayerCosmetics;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cosmetics of the network and who owns them.
 * <p>
 * The catalogue is seeded from {@link Cosmetics#shipped()} the first time and never again: after that the
 * file wins, because an admin who lowered a price or switched an effect off did that on purpose and an
 * update should not undo it. A cosmetic that is new in a version is added, everything that is already
 * there is left alone.
 */
public class CosmeticStore {

    private final File file;
    private final YamlConfiguration config;
    private final Map<String, CosmeticData> catalog = new LinkedHashMap<>();
    private final Map<UUID, PlayerCosmetics> players = new ConcurrentHashMap<>();

    public CosmeticStore() {
        this(new File("./cosmetics.yml"));
    }

    public CosmeticStore(File file) {
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
        seed();
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("cosmetics");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) continue;
                CosmeticData cosmetic = read(key, entry);
                if (cosmetic != null) catalog.put(key(key), cosmetic);
            }
        }
        ConfigurationSection owners = config.getConfigurationSection("players");
        if (owners != null) {
            for (String key : owners.getKeys(false)) {
                ConfigurationSection entry = owners.getConfigurationSection(key);
                if (entry == null) continue;
                try {
                    UUID id = UUID.fromString(key);
                    players.put(id, readPlayer(id, entry));
                } catch (IllegalArgumentException ignored) {
                    // an unreadable key is one player's cosmetics, not a reason to lose everybody's
                }
            }
        }
        System.out.println("Loaded " + catalog.size() + " cosmetics and " + players.size()
                + " players from " + file.getName());
    }

    /**
     * Adds what this version ships and the file does not have yet.
     */
    private void seed() {
        boolean added = false;
        for (CosmeticData shipped : Cosmetics.shipped()) {
            if (catalog.containsKey(key(shipped.getId()))) continue;
            catalog.put(key(shipped.getId()), shipped);
            write(shipped);
            added = true;
        }
        if (added) save();
    }

    private static CosmeticData read(String id, ConfigurationSection entry) {
        CosmeticData cosmetic = new CosmeticData();
        cosmetic.setId(id);
        cosmetic.setType(type(entry.getString("type")));
        cosmetic.setDisplayName(entry.getString("name", id));
        cosmetic.setDescription(entry.getString("description", ""));
        cosmetic.setIcon(entry.getString("icon"));
        cosmetic.setEnabled(entry.getBoolean("enabled", true));
        cosmetic.setBuyable(entry.getBoolean("buyable", true));
        cosmetic.setPriceBits(entry.getInt("price", 0));
        cosmetic.setFree(entry.getBoolean("free", false));
        ConfigurationSection settings = entry.getConfigurationSection("settings");
        if (settings != null) {
            Map<String, String> values = new HashMap<>();
            for (String key : settings.getKeys(false)) values.put(key, settings.getString(key, ""));
            cosmetic.setSettings(values);
        }
        return cosmetic;
    }

    private static CosmeticType type(String name) {
        if (name == null) return CosmeticType.WIN_EFFECT;
        for (CosmeticType type : CosmeticType.values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return CosmeticType.WIN_EFFECT;
    }

    private static PlayerCosmetics readPlayer(UUID id, ConfigurationSection entry) {
        PlayerCosmetics cosmetics = new PlayerCosmetics(id);
        cosmetics.setOwned(new LinkedHashSet<>(entry.getStringList("owned")));
        ConfigurationSection selected = entry.getConfigurationSection("selected");
        if (selected != null) {
            HashMap<String, String> selections = new HashMap<>();
            for (String key : selected.getKeys(false)) selections.put(key, selected.getString(key));
            cosmetics.setSelections(selections);
        }
        return cosmetics;
    }

    private void write(CosmeticData cosmetic) {
        String path = "cosmetics." + cosmetic.getId();
        config.set(path + ".type", cosmetic.getType().name());
        config.set(path + ".name", cosmetic.getDisplayName());
        config.set(path + ".description", cosmetic.getDescription());
        config.set(path + ".icon", cosmetic.getIcon());
        config.set(path + ".enabled", cosmetic.isEnabled());
        config.set(path + ".buyable", cosmetic.isBuyable());
        config.set(path + ".price", cosmetic.getPriceBits());
        config.set(path + ".free", cosmetic.isFree());
        config.set(path + ".settings", null);
        for (Map.Entry<String, String> setting : cosmetic.getSettings().entrySet()) {
            config.set(path + ".settings." + setting.getKey(), setting.getValue());
        }
    }

    private void writePlayer(PlayerCosmetics cosmetics) {
        String path = "players." + cosmetics.getPlayer();
        config.set(path + ".owned", new ArrayList<>(cosmetics.getOwned()));
        config.set(path + ".selected", null);
        for (Map.Entry<String, String> selection : cosmetics.getSelections().entrySet()) {
            config.set(path + ".selected." + selection.getKey(), selection.getValue());
        }
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    private static String key(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    /**
     * @param id a cosmetic
     * @return it, or {@code null} when the network has none by that name
     */
    public CosmeticData get(String id) {
        return catalog.get(key(id));
    }

    public List<CosmeticData> getCatalog() {
        return new ArrayList<>(catalog.values());
    }

    /**
     * @param player somebody
     * @return what they own, never {@code null}
     */
    public PlayerCosmetics of(UUID player) {
        return players.computeIfAbsent(player, PlayerCosmetics::new);
    }

    /**
     * @return the catalogue and the ownership in one object, ready to be sent
     */
    public CosmeticSnapshot snapshot() {
        return new CosmeticSnapshot(new ArrayList<>(catalog.values()), new HashMap<>(players));
    }

    /**
     * Stores what an admin decided about a cosmetic. Only the decisions are taken over - the name, the
     * description and the effect behind it belong to the code, not to a menu.
     *
     * @param cosmetic the cosmetic as the admin left it
     * @return the stored state, or {@code null} when there is no such cosmetic
     */
    public synchronized CosmeticData put(CosmeticData cosmetic) {
        if (cosmetic == null || cosmetic.getId() == null) return null;
        CosmeticData known = catalog.get(key(cosmetic.getId()));
        if (known == null) return null;
        known.setEnabled(cosmetic.isEnabled());
        known.setBuyable(cosmetic.isBuyable());
        known.setPriceBits(cosmetic.getPriceBits());
        known.setFree(cosmetic.isFree());
        known.setSettings(cosmetic.getSettings());
        write(known);
        save();
        return known;
    }

    /**
     * Gives somebody a cosmetic without charging for it.
     *
     * @param player   who gets it
     * @param cosmetic what they get
     * @return their new state
     */
    public synchronized PlayerCosmetics grant(UUID player, String cosmetic) {
        PlayerCosmetics cosmetics = of(player);
        cosmetics.add(cosmetic);
        writePlayer(cosmetics);
        save();
        return cosmetics;
    }

    /**
     * Puts a cosmetic on somebody, or takes it off.
     *
     * @param player who
     * @param type   which kind
     * @param id     what to wear, {@code null} for nothing
     * @return their new state, or {@code null} when they do not own it
     */
    public synchronized PlayerCosmetics select(UUID player, CosmeticType type, String id) {
        PlayerCosmetics cosmetics = of(player);
        if (id != null && !owns(cosmetics, id)) return null;
        cosmetics.select(type, id);
        writePlayer(cosmetics);
        save();
        return cosmetics;
    }

    /**
     * @param cosmetics somebody's cosmetics
     * @param id        a cosmetic
     * @return whether it is theirs - bought, or free for everybody
     */
    public boolean owns(PlayerCosmetics cosmetics, String id) {
        if (cosmetics != null && cosmetics.owns(id)) return true;
        CosmeticData cosmetic = get(id);
        return cosmetic != null && cosmetic.isFree() && cosmetic.isEnabled();
    }

    /**
     * @param player a player
     * @param id     a cosmetic
     * @return whether they may use it
     */
    public boolean owns(UUID player, String id) {
        return owns(players.get(player), id);
    }

    /**
     * @param player a player
     * @return everything they may use, including what is free for everybody
     */
    public Set<String> ownedBy(UUID player) {
        Set<String> owned = new LinkedHashSet<>(of(player).getOwned());
        for (CosmeticData cosmetic : catalog.values()) {
            if (cosmetic.isFree() && cosmetic.isEnabled()) owned.add(cosmetic.getId());
        }
        return owned;
    }
}
