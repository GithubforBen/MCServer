package de.schnorrenbergers.survival.featrues.chunklimiter;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the chunk limiter can be tuned with, backed by {@code ./configs/chunklimiter.yml}.
 * <p>
 * The old limiter had all its numbers hard coded, which made it impossible to react to a server that
 * behaves differently. Every value here has a sensible default, so the file can stay empty.
 */
public class ChunkLimiterSettings {

    private final File file;
    private final YamlConfiguration config;

    private boolean enabled;
    private long checkIntervalTicks;
    private int smoothingSamples;
    private int raiseDelayChecks;
    private double raiseHysteresisTps;
    private boolean notifyPlayers;
    private long notifyCooldownSeconds;

    private Group paying;
    private Group free;

    /** The penalty steps, sorted from the highest tps threshold to the lowest. */
    private List<Tier> tiers;

    public ChunkLimiterSettings() {
        file = new File("./configs/chunklimiter.yml");
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    /**
     * Reads the file, writing back every value that was missing so the file documents itself.
     */
    public final void load() {
        enabled = get("enabled", true, "Whether the view distance is lowered while the server lags.");
        checkIntervalTicks = Math.max(20L, get("check-interval-ticks", 40L,
                "How often the tps is sampled and the view distance is adjusted."));
        smoothingSamples = Math.max(1, get("smoothing-samples", 5,
                "How many samples the tps is averaged over. Higher means calmer, slower reactions."));
        raiseDelayChecks = Math.max(0, get("raise-delay-checks", 3,
                "How many good samples in a row are needed before the view distance is raised again."));
        raiseHysteresisTps = get("raise-hysteresis-tps", 1.5d,
                "How far above a threshold the tps has to climb before that step is left again.",
                "This stops the view distance from flickering around a threshold.");
        notifyPlayers = get("notify-players", true, "Whether players are told when their view distance changes.");
        notifyCooldownSeconds = Math.max(0L, get("notify-cooldown-seconds", 60L,
                "The shortest time between two messages to the same player."));

        paying = loadGroup("paying", 12, 8, 0.0d,
                "The players that pay for the server. With a penalty-factor of 0 they are never limited.");
        free = loadGroup("free", 10, 4, 1.0d,
                "Everybody else. The penalty-factor scales how hard the tiers below hit them.");

        loadTiers();
        save();
    }

    /**
     * Reads one of the two player groups.
     *
     * @param path          the section the group lives in
     * @param defaultMax    the view distance the group gets while the server runs well
     * @param defaultMin    the view distance the group is never pushed below
     * @param defaultFactor how strongly the tiers apply to the group
     * @param comment       what the section is for
     * @return the group
     */
    private Group loadGroup(String path, int defaultMax, int defaultMin, double defaultFactor, String comment) {
        if (!config.contains(path)) {
            config.createSection(path);
            config.setComments(path, List.of(comment));
        }
        int max = get(path + ".max-view-distance", defaultMax, "The view distance while the server runs well.");
        int min = get(path + ".min-view-distance", defaultMin, "The view distance is never lowered below this.");
        double factor = get(path + ".penalty-factor", defaultFactor,
                "How strongly the tiers apply. 0 disables the limiting for this group.");
        int simulationOffset = get(path + ".simulation-distance-offset", 0,
                "Added to the view distance to get the simulation distance.");
        return new Group(clampDistance(max), clampDistance(min), Math.max(0.0d, factor), simulationOffset);
    }

    /**
     * Reads the penalty steps. Each entry says how many chunks are taken away once the tps drops to or
     * below a threshold.
     * <p>
     * The steps are a list of {@code tps}/{@code penalty} pairs rather than a section keyed by the tps.
     * A key like {@code 15.0} would be read as the path {@code 15} -> {@code 0}, because yaml configs treat
     * a dot as a separator, and every step would silently end up costing nothing.
     */
    private void loadTiers() {
        if (!config.contains("tiers")) {
            config.set("tiers", List.of(
                    tier(18.0d, 0),
                    tier(15.0d, 2),
                    tier(10.0d, 4),
                    tier(5.0d, 6),
                    tier(3.0d, 8)));
            config.setComments("tiers", List.of(
                    "How many chunks are taken away once the tps drops to or below a threshold.",
                    "Only the lowest matching threshold counts, so the steps do not add up."));
        }
        List<Tier> parsed = new ArrayList<>();
        for (Map<?, ?> entry : config.getMapList("tiers")) {
            Object tps = entry.get("tps");
            Object penalty = entry.get("penalty");
            if (!(tps instanceof Number threshold) || !(penalty instanceof Number chunks)) {
                warn("Ignoring the chunk limiter tier " + entry + ": it needs a tps and a penalty.");
                continue;
            }
            parsed.add(new Tier(threshold.doubleValue(), Math.max(0, chunks.intValue())));
        }
        if (parsed.isEmpty()) parsed.add(new Tier(20.0d, 0));
        parsed.sort(Comparator.comparingDouble(Tier::tps).reversed());
        tiers = List.copyOf(parsed);
    }

    /**
     * @param tps     the threshold the tps has to fall to
     * @param penalty how many chunks are taken away from then on
     * @return that step in the shape it is written to the config in
     */
    private static Map<String, Object> tier(double tps, int penalty) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("tps", tps);
        entry.put("penalty", penalty);
        return entry;
    }

    /**
     * Looks a value up, writing the default into the file if it is not there yet.
     *
     * @param path     where the value lives
     * @param fallback the value to use and store if it is missing
     * @param comments what the value does
     * @return the configured value
     */
    @SuppressWarnings("unchecked")
    private <T> T get(String path, T fallback, String... comments) {
        if (!config.contains(path)) {
            config.set(path, fallback);
            if (comments.length > 0) config.setComments(path, List.of(comments));
            return fallback;
        }
        Object value = config.get(path);
        try {
            if (fallback instanceof Boolean) return (T) Boolean.valueOf(config.getBoolean(path));
            if (fallback instanceof Integer) return (T) Integer.valueOf(config.getInt(path));
            if (fallback instanceof Long) return (T) Long.valueOf(config.getLong(path));
            if (fallback instanceof Double) return (T) Double.valueOf(config.getDouble(path));
            return value == null ? fallback : (T) value;
        } catch (ClassCastException e) {
            warn("'" + path + "' in chunklimiter.yml is not usable, falling back to " + fallback + ".");
            return fallback;
        }
    }

    /**
     * @param distance the value that was configured
     * @return that value, forced into the range minecraft accepts
     */
    private static int clampDistance(int distance) {
        return Math.max(2, Math.min(32, distance));
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            warn("Could not save chunklimiter.yml: " + e.getMessage());
        }
    }

    /**
     * Logs through the plugin when it is up, and to the console when the settings are read before it is.
     *
     * @param message what went wrong
     */
    private static void warn(String message) {
        Survival plugin = Survival.getInstance();
        if (plugin == null) {
            System.out.println("[ChunkLimiter] " + message);
            return;
        }
        plugin.getLogger().warning(message);
    }

    /**
     * How many chunks are taken away at the given tps.
     *
     * @param tps the tps to look up
     * @return the penalty of the lowest threshold the tps has fallen to
     */
    public int penaltyFor(double tps) {
        int penalty = 0;
        for (Tier tier : tiers) {
            if (tps <= tier.tps()) penalty = tier.penalty();
        }
        return penalty;
    }

    /**
     * The tps a server has to climb back to before the given penalty is lifted. Sitting exactly on a
     * threshold would otherwise make the view distance jump up and down every check.
     *
     * @param penalty the penalty that is currently applied
     * @return the tps needed to leave that step, or {@link Double#NEGATIVE_INFINITY} if there is none
     */
    public double releaseTpsFor(int penalty) {
        double threshold = Double.NEGATIVE_INFINITY;
        for (Tier tier : tiers) {
            if (tier.penalty() == penalty && tier.tps() > threshold) threshold = tier.tps();
        }
        return threshold == Double.NEGATIVE_INFINITY ? threshold : threshold + raiseHysteresisTps;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public int getSmoothingSamples() {
        return smoothingSamples;
    }

    public int getRaiseDelayChecks() {
        return raiseDelayChecks;
    }

    public boolean isNotifyPlayers() {
        return notifyPlayers;
    }

    public long getNotifyCooldownSeconds() {
        return notifyCooldownSeconds;
    }

    public Group getPaying() {
        return paying;
    }

    public Group getFree() {
        return free;
    }

    public List<Tier> getTiers() {
        return tiers;
    }

    /**
     * One penalty step.
     *
     * @param tps     the threshold the tps has to fall to
     * @param penalty how many chunks are taken away from then on
     */
    public record Tier(double tps, int penalty) {
    }

    /**
     * The limits of one group of players.
     *
     * @param maxViewDistance        what the group gets while the server runs well
     * @param minViewDistance        what the group is never pushed below
     * @param penaltyFactor          how strongly the tiers apply, 0 meaning not at all
     * @param simulationDistanceOffset added to the view distance to get the simulation distance
     */
    public record Group(int maxViewDistance, int minViewDistance, double penaltyFactor, int simulationDistanceOffset) {

        /**
         * @param penalty the chunks the tiers want to take away
         * @return the view distance this group ends up with
         */
        public int viewDistanceFor(int penalty) {
            int scaled = (int) Math.round(penalty * penaltyFactor);
            return Math.max(minViewDistance, Math.min(maxViewDistance, maxViewDistance - scaled));
        }

        /**
         * @param viewDistance the view distance that was decided on
         * @return the simulation distance that belongs to it
         */
        public int simulationDistanceFor(int viewDistance) {
            return Math.max(2, Math.min(viewDistance, viewDistance + simulationDistanceOffset));
        }
    }
}
