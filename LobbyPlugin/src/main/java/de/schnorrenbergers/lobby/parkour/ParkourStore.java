package de.schnorrenbergers.lobby.parkour;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The courses and the times, in {@code parkour.yml}.
 * <p>
 * Both in one file on purpose: a time only means anything together with the course it was run on, and a
 * course that is deleted should take its leaderboard with it rather than leave times behind that nobody
 * can beat any more.
 */
public class ParkourStore {

    private final File file;
    private final Logger logger;
    private YamlConfiguration config;

    private final Map<String, ParkourCourse> courses = new LinkedHashMap<>();
    /** Course to player to their best time in milliseconds. */
    private final Map<String, Map<UUID, Record>> times = new LinkedHashMap<>();

    /**
     * One player's best run of one course.
     *
     * @param player who ran it
     * @param name   what they were called at the time, so a leaderboard reads without a lookup
     * @param millis how long they took
     */
    public record Record(UUID player, String name, long millis) {
    }

    public ParkourStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    /**
     * Reads the file, writing an empty one when there is none yet.
     */
    public final void load() {
        courses.clear();
        times.clear();
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
        }
        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("courses");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection course = section.getConfigurationSection(name);
                if (course == null) continue;
                courses.put(name.toLowerCase(Locale.ROOT), ParkourCourse.read(name, course));
            }
        }
        ConfigurationSection board = config.getConfigurationSection("times");
        if (board != null) {
            for (String course : board.getKeys(false)) {
                ConfigurationSection entries = board.getConfigurationSection(course);
                if (entries == null) continue;
                Map<UUID, Record> perCourse = new LinkedHashMap<>();
                for (String id : entries.getKeys(false)) {
                    UUID player = uuid(id);
                    if (player == null) continue;
                    perCourse.put(player, new Record(player,
                            entries.getString(id + ".name", "?"),
                            entries.getLong(id + ".millis")));
                }
                times.put(course.toLowerCase(Locale.ROOT), perCourse);
            }
        }
    }

    /**
     * Writes everything back.
     */
    public void save() {
        config.set("courses", null);
        ConfigurationSection section = config.createSection("courses");
        section.setComments("", List.of(
                "The parkour courses of the lobby. Built with /parkour setup rather than by hand:",
                "every point is where somebody was standing when they set it."));
        for (ParkourCourse course : courses.values()) {
            course.write(section.createSection(course.getName()));
        }

        config.set("times", null);
        ConfigurationSection board = config.createSection("times");
        board.setComments("", List.of("The best time of every player, per course, in milliseconds."));
        for (Map.Entry<String, Map<UUID, Record>> entry : times.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            ConfigurationSection perCourse = board.createSection(entry.getKey());
            for (Record record : entry.getValue().values()) {
                perCourse.set(record.player() + ".name", record.name());
                perCourse.set(record.player() + ".millis", record.millis());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            logger.warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------- courses

    public @Nullable ParkourCourse get(String name) {
        return name == null ? null : courses.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<ParkourCourse> all() {
        return List.copyOf(courses.values());
    }

    /**
     * @param name the course to create
     * @return it, or the one that already had that name
     */
    public ParkourCourse getOrCreate(String name) {
        return courses.computeIfAbsent(name.toLowerCase(Locale.ROOT), ParkourCourse::new);
    }

    /**
     * @param name the course to remove, with its times
     * @return whether there was one
     */
    public boolean remove(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        times.remove(key);
        return courses.remove(key) != null;
    }

    // --------------------------------------------------------------------- times

    /**
     * @param course the course
     * @param player who ran it
     * @return their best time in milliseconds, or {@code null} when they have never finished it
     */
    public @Nullable Record best(String course, UUID player) {
        Map<UUID, Record> perCourse = times.get(course.toLowerCase(Locale.ROOT));
        return perCourse == null ? null : perCourse.get(player);
    }

    /**
     * Writes a time down, keeping only the better of the two.
     *
     * @param course the course
     * @param player who ran it
     * @param name   what they are called
     * @param millis how long they took
     * @return whether this beat what they had before, which is what makes it worth saying out loud
     */
    public boolean submit(String course, UUID player, String name, long millis) {
        Map<UUID, Record> perCourse = times.computeIfAbsent(course.toLowerCase(Locale.ROOT),
                key -> new LinkedHashMap<>());
        Record previous = perCourse.get(player);
        if (previous != null && previous.millis() <= millis) return false;
        perCourse.put(player, new Record(player, name, millis));
        save();
        return true;
    }

    /**
     * @param course the course
     * @param limit  how many places to hand back
     * @return the fastest runs, fastest first
     */
    public List<Record> leaderboard(String course, int limit) {
        Map<UUID, Record> perCourse = times.get(course.toLowerCase(Locale.ROOT));
        if (perCourse == null) return List.of();
        List<Record> board = new ArrayList<>(perCourse.values());
        board.sort(Comparator.comparingLong(Record::millis));
        return List.copyOf(board.subList(0, Math.min(limit, board.size())));
    }

    private static @Nullable UUID uuid(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
