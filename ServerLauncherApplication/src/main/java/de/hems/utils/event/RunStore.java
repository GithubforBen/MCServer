package de.hems.utils.event;

import de.hems.types.event.RunData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Where the attempts at the run events live.
 * <p>
 * These are the results of the whole thing, so they are written through on every change rather than kept
 * until shutdown - a crash halfway through a five day event must not cost the leaderboard.
 */
public class RunStore {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, RunData> runs = new ConcurrentHashMap<>();

    public RunStore() {
        this(new File("./runs.yml"));
    }

    public RunStore(File file) {
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
        ConfigurationSection section = config.getConfigurationSection("runs");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            RunData run = read(key, entry);
            if (run != null) runs.put(run.getId(), run);
        }
        System.out.println("Loaded " + runs.size() + " runs from " + file.getName());
    }

    private static RunData read(String key, ConfigurationSection entry) {
        UUID id;
        UUID eventId;
        try {
            id = UUID.fromString(key);
            eventId = UUID.fromString(entry.getString("event", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
        RunData run = new RunData();
        run.setId(id);
        run.setEventId(eventId);
        Set<UUID> participants = new LinkedHashSet<>();
        for (String participant : entry.getStringList("participants")) {
            try {
                participants.add(UUID.fromString(participant));
            } catch (IllegalArgumentException ignored) {
                // a broken entry costs one participant, not the whole run
            }
        }
        run.setParticipants(participants);
        run.setServerName(entry.getString("server"));
        run.setStartedAt(entry.getLong("started-at"));
        run.setFinishedAt(entry.getLong("finished-at"));
        run.setElapsedTicks(entry.getLong("elapsed-ticks", 0L));
        // a run that was being played when the launcher went down comes back paused rather than with a
        // clock that has been running the whole time it was off
        run.setActiveSince(0L);
        run.setIntendedTeamSize(entry.getInt("intended-team-size", 0));
        run.setCompleted(new LinkedHashSet<>(entry.getStringList("completed")));
        try {
            run.setState(RunData.State.valueOf(entry.getString("state", "RUNNING")));
        } catch (IllegalArgumentException e) {
            run.setState(RunData.State.RUNNING);
        }
        return run;
    }

    private void write(RunData run) {
        String path = "runs." + run.getId();
        config.set(path + ".event", run.getEventId() == null ? null : run.getEventId().toString());
        List<String> participants = new ArrayList<>();
        for (UUID participant : run.getParticipants()) participants.add(participant.toString());
        config.set(path + ".participants", participants);
        config.set(path + ".server", run.getServerName());
        config.set(path + ".started-at", run.getStartedAt());
        config.set(path + ".finished-at", run.getFinishedAt());
        config.set(path + ".elapsed-ticks", run.getElapsedTicksRaw());
        config.set(path + ".intended-team-size", run.getIntendedTeamSize());
        config.set(path + ".completed", new ArrayList<>(run.getCompleted()));
        config.set(path + ".state", run.getState().name());
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Stores a run, creating it if it is new.
     *
     * @param run the run to store
     * @return the run as it is stored now, or {@code null} if it was unusable
     */
    public synchronized RunData put(RunData run) {
        if (run == null || run.getId() == null || run.getEventId() == null) return null;
        runs.put(run.getId(), run);
        write(run);
        save();
        return run;
    }

    /**
     * @return every run
     */
    public List<RunData> getRuns() {
        return new ArrayList<>(runs.values());
    }

    /**
     * @param id the run to look up
     * @return that run, or {@code null}
     */
    public RunData getRun(UUID id) {
        return id == null ? null : runs.get(id);
    }

    /**
     * @param eventId the event to look at
     * @return its runs, fastest finished first, unfinished ones after them
     */
    public List<RunData> getRunsOf(UUID eventId) {
        List<RunData> found = new ArrayList<>();
        for (RunData run : runs.values()) {
            if (run.getEventId().equals(eventId)) found.add(run);
        }
        found.sort(leaderboardOrder());
        return found;
    }

    /**
     * The order a leaderboard is read in: finished runs by time, everything else behind them.
     *
     * @return the comparator
     */
    public static Comparator<RunData> leaderboardOrder() {
        return (left, right) -> {
            if (left.isRanked() != right.isRanked()) return left.isRanked() ? -1 : 1;
            if (left.isRanked()) {
                return Long.compare(left.getElapsedTicks(), right.getElapsedTicks());
            }
            // neither counts, so the newer attempt is the more interesting one
            return Long.compare(right.getStartedAt(), left.getStartedAt());
        };
    }

    /**
     * @param eventId the event to look at
     * @param player  the player to count for
     * @return how many runs that player has already used up
     */
    public int countRunsOf(UUID eventId, UUID player) {
        int count = 0;
        for (RunData run : runs.values()) {
            if (run.getEventId().equals(eventId) && run.getParticipants().contains(player)) count++;
        }
        return count;
    }

    /**
     * @param id the run to remove
     * @return whether it existed
     */
    public synchronized boolean delete(UUID id) {
        if (id == null || runs.remove(id) == null) return false;
        config.set("runs." + id, null);
        save();
        return true;
    }
}
