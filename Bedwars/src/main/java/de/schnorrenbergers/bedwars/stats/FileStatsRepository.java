package de.schnorrenbergers.bedwars.stats;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The rounds of this server, as one file each.
 * <p>
 * A file rather than a database because a bedwars server is thrown away after one round: there is exactly
 * one file to write, nothing to read back, and nothing that could still be open when the server stops.
 * Whoever collects them later - the launcher - reads a directory of small yaml files.
 */
public final class FileStatsRepository implements StatsRepository {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final File directory;

    /**
     * @param directory where the files go
     */
    public FileStatsRepository(File directory) {
        this.directory = directory;
    }

    @Override
    public void save(RoundStats stats) {
        try {
            if (!directory.exists() && !directory.mkdirs()) {
                Bukkit.getLogger().warning("[Bedwars] Could not create " + directory.getPath()
                        + ", the round's numbers are not kept.");
                return;
            }
            YamlConfiguration file = new YamlConfiguration();
            file.set("map", stats.map());
            file.set("mode", stats.mode());
            file.set("seconds", stats.seconds());
            file.set("winner", stats.winner() == null ? "none" : stats.winner().name());
            file.set("players", rows(stats));
            file.save(new File(directory, "round_" + LocalDateTime.now().format(STAMP) + ".yml"));
        } catch (IOException | RuntimeException e) {
            // a round that is over has to end either way
            Bukkit.getLogger().warning("[Bedwars] The round's numbers could not be written: "
                    + e.getMessage());
        }
    }

    /**
     * @param stats the round
     * @return the player lines, as the plain maps yaml writes without any of bukkit's serialisation
     */
    private static List<Map<String, Object>> rows(RoundStats stats) {
        List<Map<String, Object>> written = new ArrayList<>();
        for (RoundStats.Row row : stats.rows()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("uuid", row.uuid().toString());
            entry.put("name", row.name());
            entry.put("team", row.team() == null ? "none" : row.team().name());
            entry.put("kills", row.kills());
            entry.put("final-kills", row.finals());
            entry.put("deaths", row.deaths());
            entry.put("beds", row.beds());
            entry.put("points", row.points());
            written.add(entry);
        }
        return written;
    }
}
