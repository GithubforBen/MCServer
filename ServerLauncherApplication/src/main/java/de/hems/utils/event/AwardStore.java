package de.hems.utils.event;

import de.hems.types.event.AwardData;
import de.hems.types.event.PrizeData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The prizes waiting to be collected.
 * <p>
 * These are the only record that somebody won something, so nothing here is thrown away on its own - an
 * award stays until the player has it in their hands and the game server says so.
 */
public class AwardStore {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, AwardData> awards = new ConcurrentHashMap<>();

    public AwardStore() {
        this(new File("./awards.yml"));
    }

    public AwardStore(File file) {
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
        ConfigurationSection section = config.getConfigurationSection("awards");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            AwardData award = read(key, entry);
            if (award != null) awards.put(award.getId(), award);
        }
        System.out.println("Loaded " + awards.size() + " awards from " + file.getName());
    }

    private static AwardData read(String key, ConfigurationSection entry) {
        AwardData award = new AwardData();
        try {
            award.setId(UUID.fromString(key));
            award.setPlayer(UUID.fromString(entry.getString("player", "")));
        } catch (IllegalArgumentException e) {
            return null;
        }
        String eventId = entry.getString("event");
        if (eventId != null) {
            try {
                award.setEventId(UUID.fromString(eventId));
            } catch (IllegalArgumentException ignored) {
                // the event is gone, the prize still stands
            }
        }
        award.setEventName(entry.getString("event-name", "Event"));
        award.setPlace(entry.getInt("place", AwardData.PARTICIPATION));
        award.setPrize(PrizeData.parse(entry.getString("prize")));
        award.setAwardedAt(entry.getLong("awarded-at"));
        award.setClaimed(entry.getBoolean("claimed", false));
        return award;
    }

    private void write(AwardData award) {
        String path = "awards." + award.getId();
        config.set(path + ".player", award.getPlayer().toString());
        config.set(path + ".event", award.getEventId() == null ? null : award.getEventId().toString());
        config.set(path + ".event-name", award.getEventName());
        config.set(path + ".place", award.getPlace());
        config.set(path + ".prize", award.getPrize().serialize());
        config.set(path + ".awarded-at", award.getAwardedAt());
        config.set(path + ".claimed", award.isClaimed());
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Puts a prize aside for somebody.
     *
     * @param award the prize
     */
    public synchronized void put(AwardData award) {
        if (award == null || award.getId() == null || award.getPlayer() == null) return;
        if (award.getPrize().isEmpty()) return;
        awards.put(award.getId(), award);
        write(award);
        save();
    }

    /**
     * @param player the player to look up
     * @return what they still have to collect, oldest first
     */
    public List<AwardData> getUnclaimed(UUID player) {
        List<AwardData> found = new ArrayList<>();
        for (AwardData award : awards.values()) {
            if (!award.isClaimed() && award.getPlayer().equals(player)) found.add(award);
        }
        found.sort(Comparator.comparingLong(AwardData::getAwardedAt));
        return found;
    }

    /**
     * Marks a prize as collected. Only called once the game server confirms the player has it.
     *
     * @param id the prize
     * @return whether it was still open
     */
    public synchronized boolean claim(UUID id) {
        AwardData award = id == null ? null : awards.get(id);
        if (award == null || award.isClaimed()) return false;
        award.setClaimed(true);
        write(award);
        save();
        return true;
    }

    /**
     * @return every award, for the website
     */
    public List<AwardData> getAwards() {
        return new ArrayList<>(awards.values());
    }
}
