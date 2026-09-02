package de.hems.utils.round;

import de.hems.types.round.RoundData;
import de.hems.types.round.RoundPolicy;
import de.hems.types.round.RoundState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The rounds players put up themselves, and the rules they are allowed to.
 * <p>
 * Owned by the launcher for the same reason the events are: a round outlives the server it runs on and has
 * to be visible from every lobby, and the rules have to mean the same thing everywhere the moment an admin
 * changes them.
 */
public class RoundStore {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, RoundData> rounds = new ConcurrentHashMap<>();
    private RoundPolicy policy = new RoundPolicy();

    public RoundStore() {
        this(new File("./rounds.yml"));
    }

    public RoundStore(File file) {
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
        // written out even when nothing was ever changed, so the file says what the rules are instead of
        // being an empty file an admin has to guess the defaults of
        if (!config.contains("policy")) {
            writePolicy();
            save();
        }
    }

    private void load() {
        readPolicy();
        ConfigurationSection section = config.getConfigurationSection("rounds");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            RoundData round = read(key, entry);
            if (round != null) rounds.put(round.getId(), round);
        }
        System.out.println("Loaded " + rounds.size() + " rounds from " + file.getName());
    }

    private void readPolicy() {
        ConfigurationSection section = config.getConfigurationSection("policy");
        if (section == null) return;
        RoundPolicy loaded = new RoundPolicy();
        loaded.setSelfStartEnabled(section.getBoolean("self-start", loaded.isSelfStartEnabled()));
        loaded.setMaxPerPlayer(section.getInt("max-per-player", loaded.getMaxPerPlayer()));
        loaded.setCooldownSeconds(section.getInt("cooldown-seconds", loaded.getCooldownSeconds()));
        loaded.setMaxRounds(section.getInt("max-rounds", loaded.getMaxRounds()));
        loaded.setBlockWhileEventRunning(section.getBoolean("block-while-event-running",
                loaded.isBlockWhileEventRunning()));
        loaded.setBlockBeforeEventMinutes(section.getInt("block-before-event-minutes",
                loaded.getBlockBeforeEventMinutes()));
        loaded.setMemoryMB(section.getInt("memory-mb", loaded.getMemoryMB()));
        policy = loaded;
    }

    private static RoundData read(String key, ConfigurationSection entry) {
        RoundData round = new RoundData();
        try {
            round.setId(UUID.fromString(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
        round.setServerName(entry.getString("server"));
        String owner = entry.getString("owner");
        if (owner != null) {
            try {
                round.setOwnerId(UUID.fromString(owner));
            } catch (IllegalArgumentException ignored) {
                // a round without a readable owner is still a round, it just has no admin
            }
        }
        round.setOwnerName(entry.getString("owner-name"));
        round.setMap(entry.getString("map"));
        round.setTeamSize(entry.getInt("team-size", 2));
        round.setAddons(new LinkedHashSet<>(entry.getStringList("addons")));
        round.setOpen(entry.getBoolean("open", true));
        Set<UUID> invited = new LinkedHashSet<>();
        for (String guest : entry.getStringList("invited")) {
            try {
                invited.add(UUID.fromString(guest));
            } catch (IllegalArgumentException ignored) {
                // one unreadable entry costs one guest, not the guest list
            }
        }
        round.setInvited(invited);
        round.setState(state(entry.getString("state")));
        round.setCreatedAt(entry.getLong("created-at", System.currentTimeMillis()));
        round.setEndedAt(entry.getLong("ended-at", 0L));
        round.setPlayers(entry.getInt("players", 0));
        return round;
    }

    private static RoundState state(String name) {
        if (name == null) return RoundState.PREPARING;
        for (RoundState state : RoundState.values()) {
            if (state.name().equalsIgnoreCase(name)) return state;
        }
        return RoundState.PREPARING;
    }

    private void write(RoundData round) {
        String path = "rounds." + round.getId();
        config.set(path + ".server", round.getServerName());
        config.set(path + ".owner", round.getOwnerId() == null ? null : round.getOwnerId().toString());
        config.set(path + ".owner-name", round.getOwnerName());
        config.set(path + ".map", round.getMap());
        config.set(path + ".team-size", round.getTeamSize());
        config.set(path + ".addons", new ArrayList<>(round.getAddons()));
        config.set(path + ".open", round.isOpen());
        List<String> invited = new ArrayList<>();
        for (UUID guest : round.getInvited()) invited.add(guest.toString());
        config.set(path + ".invited", invited);
        config.set(path + ".state", round.getState().name());
        config.set(path + ".created-at", round.getCreatedAt());
        config.set(path + ".ended-at", round.getEndedAt());
        config.set(path + ".players", round.getPlayers());
    }

    private void writePolicy() {
        config.set("policy.self-start", policy.isSelfStartEnabled());
        config.set("policy.max-per-player", policy.getMaxPerPlayer());
        config.set("policy.cooldown-seconds", policy.getCooldownSeconds());
        config.set("policy.max-rounds", policy.getMaxRounds());
        config.set("policy.block-while-event-running", policy.isBlockWhileEventRunning());
        config.set("policy.block-before-event-minutes", policy.getBlockBeforeEventMinutes());
        config.set("policy.memory-mb", policy.getMemoryMB());
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * @return every round, as a fresh list the caller may keep
     */
    public List<RoundData> getRounds() {
        return new ArrayList<>(rounds.values());
    }

    public RoundData get(UUID id) {
        return id == null ? null : rounds.get(id);
    }

    /**
     * @param serverName a server
     * @return the round running on it, or {@code null}
     */
    public RoundData byServer(String serverName) {
        if (serverName == null) return null;
        String wanted = serverName.toUpperCase(Locale.ROOT);
        for (RoundData round : rounds.values()) {
            if (round.getServerName() != null && round.getServerName().toUpperCase(Locale.ROOT).equals(wanted)) {
                return round;
            }
        }
        return null;
    }

    public RoundPolicy getPolicy() {
        return policy;
    }

    public synchronized void setPolicy(RoundPolicy policy) {
        this.policy = policy == null ? new RoundPolicy() : policy;
        writePolicy();
        save();
    }

    /**
     * Writes a round down.
     *
     * @param round the round
     * @return the stored state
     */
    public synchronized RoundData put(RoundData round) {
        if (round == null || round.getId() == null) return null;
        rounds.put(round.getId(), round);
        write(round);
        save();
        return round;
    }

    /**
     * @param id the round to remove
     * @return whether there was one
     */
    public synchronized boolean delete(UUID id) {
        if (id == null || rounds.remove(id) == null) return false;
        config.set("rounds." + id, null);
        save();
        return true;
    }

    /**
     * How many rounds one player has open right now.
     *
     * @param player the player
     * @return the count
     */
    public int openOf(UUID player) {
        int open = 0;
        for (RoundData round : rounds.values()) {
            if (round.isOwner(player) && round.getState().isAlive()) open++;
        }
        return open;
    }

    /**
     * @return how many self started rounds are alive across the network
     */
    public int openRounds() {
        int open = 0;
        for (RoundData round : rounds.values()) {
            if (round.getState().isAlive()) open++;
        }
        return open;
    }

    /**
     * @param player the player
     * @return when they last started a round, {@code 0} when they never did
     */
    public long lastStartOf(UUID player) {
        long last = 0L;
        for (RoundData round : rounds.values()) {
            if (round.isOwner(player) && round.getCreatedAt() > last) last = round.getCreatedAt();
        }
        return last;
    }
}
