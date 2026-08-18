package de.schnorrenbergers.survival.featrues.chunklimiter;

import de.schnorrenbergers.survival.Survival;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lowers the view distance of players while the server struggles, so a laggy server stays playable.
 * <p>
 * The old implementation hung off {@link org.bukkit.event.world.ChunkLoadEvent}, which fires hundreds of
 * times a second, and looped over every online player on each of those events. On top of that it asked the
 * launcher over the network - blocking the main thread - whether a player pays. The lag protection was
 * therefore one of the bigger sources of lag itself.
 * <p>
 * Now a single timer samples the tps, smooths it, and only writes a distance to a player when that player's
 * value actually has to change. Going down happens right away, going back up needs the tps to stay clearly
 * above the threshold for a while, so the distance does not flicker.
 */
public class ChunkLimiter {

    private static ChunkLimiter instance;

    private final ChunkLimiterSettings settings;
    /** The measured tps of the last checks, oldest first. */
    private final double[] samples;
    private int sampleCount;
    private int sampleCursor;
    /** When the timer last ran, used to measure how long the ticks really took. */
    private long lastRunAt;
    /** The penalty that is applied at the moment. */
    private int currentPenalty;
    /** How many checks in a row asked for a lower penalty than the one that is applied. */
    private int goodChecks;
    /** The distinct penalties of all tiers, ascending, so the limiter can step back up gradually. */
    private final List<Integer> penaltySteps;
    /** The view distance every player was last set to, to avoid resending the same value. */
    private final Map<UUID, Integer> appliedViewDistance = new ConcurrentHashMap<>();
    /** When each player was last told about a change. */
    private final Map<UUID, Long> lastNotified = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ChunkLimiter(ChunkLimiterSettings settings) {
        this.settings = settings;
        this.samples = new double[settings.getSmoothingSamples()];
        this.penaltySteps = collectPenaltySteps(settings);
        instance = this;
    }

    /**
     * @param settings the settings to read the tiers from
     * @return every penalty a tier can produce, ascending and without duplicates
     */
    private static List<Integer> collectPenaltySteps(ChunkLimiterSettings settings) {
        List<Integer> steps = new ArrayList<>();
        for (ChunkLimiterSettings.Tier tier : settings.getTiers()) {
            if (!steps.contains(tier.penalty())) steps.add(tier.penalty());
        }
        if (!steps.contains(0)) steps.add(0);
        steps.sort(Comparator.naturalOrder());
        return List.copyOf(steps);
    }

    public static ChunkLimiter getInstance() {
        return instance;
    }

    /**
     * Starts the timer. Does nothing if the limiter is switched off in the config.
     */
    public void start() {
        if (!settings.isEnabled()) {
            Survival.getInstance().getLogger().info("The chunk limiter is disabled in chunklimiter.yml.");
            return;
        }
        stop();
        PayingPlayers.refreshNow();
        long interval = settings.getCheckIntervalTicks();
        task = Bukkit.getScheduler().runTaskTimer(Survival.getInstance(), this::check, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * One round: measure, decide, apply.
     */
    private void check() {
        PayingPlayers.refreshIfDue();
        double tps = smooth(measureTps());
        updatePenalty(tps);
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player, true);
        }
    }

    /**
     * Measures how well the server really keeps up, by comparing how long the last interval took to how
     * long it should have taken. That reacts far quicker than the one minute average bukkit reports, which
     * is what made the old limiter respond to lag spikes long after they were over.
     *
     * @return the tps of the interval that just passed
     */
    private double measureTps() {
        long now = System.nanoTime();
        long previous = lastRunAt;
        lastRunAt = now;
        if (previous == 0L) {
            // nothing to compare against yet - fall back to what the server reports
            return Math.min(20.0d, Bukkit.getServer().getTPS()[0]);
        }
        double elapsedMs = (now - previous) / 1_000_000.0d;
        double expectedMs = settings.getCheckIntervalTicks() * 50.0d;
        if (elapsedMs <= 0.0d) return 20.0d;
        return Math.max(0.0d, Math.min(20.0d, 20.0d * expectedMs / elapsedMs));
    }

    /**
     * Averages the last samples so a single spike does not shrink everybody's view distance.
     *
     * @param tps the tps that was just measured
     * @return the smoothed tps
     */
    private double smooth(double tps) {
        samples[sampleCursor] = tps;
        sampleCursor = (sampleCursor + 1) % samples.length;
        if (sampleCount < samples.length) sampleCount++;
        double sum = 0.0d;
        for (int i = 0; i < sampleCount; i++) sum += samples[i];
        return sum / sampleCount;
    }

    /**
     * Moves the applied penalty towards what the tps asks for. Raising it is immediate, because lag has to
     * be answered at once. Lowering it needs the tps to be clearly above the threshold of the current step
     * for several checks in a row, and then only goes one step at a time.
     *
     * @param tps the smoothed tps
     */
    private void updatePenalty(double tps) {
        int wanted = settings.penaltyFor(tps);
        if (wanted > currentPenalty) {
            currentPenalty = wanted;
            goodChecks = 0;
            return;
        }
        if (wanted == currentPenalty) {
            goodChecks = 0;
            return;
        }
        if (tps < settings.releaseTpsFor(currentPenalty)) {
            // above the threshold, but not far enough above it to trust the recovery
            goodChecks = 0;
            return;
        }
        if (++goodChecks < settings.getRaiseDelayChecks()) return;
        goodChecks = 0;
        currentPenalty = nextLowerPenalty(currentPenalty, wanted);
    }

    /**
     * @param current the penalty that is applied
     * @param floor   the penalty the tps asks for
     * @return the next step down, never below {@code floor}
     */
    private int nextLowerPenalty(int current, int floor) {
        int next = floor;
        for (Integer step : penaltySteps) {
            if (step < current && step > next) next = step;
        }
        return Math.max(floor, next);
    }

    /**
     * Gives a player the distance they should have right now.
     *
     * @param player   the player to adjust
     * @param announce whether the player may be told about the change
     */
    public void apply(Player player, boolean announce) {
        if (!settings.isEnabled() || player == null || !player.isOnline()) return;
        ChunkLimiterSettings.Group group = groupOf(player);
        int viewDistance = Math.min(group.viewDistanceFor(currentPenalty), Bukkit.getViewDistance());
        Integer previous = appliedViewDistance.get(player.getUniqueId());
        if (previous != null && previous == viewDistance
                && player.getViewDistance() == viewDistance
                && player.getSimulationDistance() == group.simulationDistanceFor(viewDistance)) {
            return;
        }
        player.setViewDistance(viewDistance);
        player.setSimulationDistance(group.simulationDistanceFor(viewDistance));
        appliedViewDistance.put(player.getUniqueId(), viewDistance);
        if (announce && previous != null && previous != viewDistance) {
            notify(player, previous, viewDistance);
        }
    }

    /**
     * Which limits apply to a player. While the list of paying players has not arrived yet everybody is
     * treated as paying - the old code assumed the opposite and punished paying players whenever the
     * network hiccupped.
     *
     * @param player the player to classify
     * @return the group the player belongs to
     */
    private ChunkLimiterSettings.Group groupOf(Player player) {
        if (!PayingPlayers.isKnown() || PayingPlayers.isPaying(player)) return settings.getPaying();
        return settings.getFree();
    }

    /**
     * Tells a player that their distance changed, at most once per cooldown.
     *
     * @param player   the player to tell
     * @param previous the distance they had
     * @param current  the distance they have now
     */
    private void notify(Player player, int previous, int current) {
        if (!settings.isNotifyPlayers()) return;
        long now = System.currentTimeMillis();
        Long last = lastNotified.get(player.getUniqueId());
        if (last != null && now - last < settings.getNotifyCooldownSeconds() * 1000L) return;
        lastNotified.put(player.getUniqueId(), now);
        if (current < previous) {
            player.sendMessage(ChatColor.YELLOW + "Deine Sichtweite wurde auf " + current
                    + " Chunks gesenkt, damit der Server flüssig bleibt.");
            if (!PayingPlayers.isPaying(player)) {
                player.sendMessage(ChatColor.GRAY + "Unterstützer behalten ihre volle Sichtweite.");
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "Der Server läuft wieder rund - deine Sichtweite ist jetzt "
                    + current + " Chunks.");
        }
    }

    /**
     * Forgets a player that left, so the maps do not grow forever.
     *
     * @param uuid the player that left
     */
    public void forget(UUID uuid) {
        appliedViewDistance.remove(uuid);
        lastNotified.remove(uuid);
    }

    /**
     * @return the penalty that is applied at the moment, in chunks
     */
    public int getCurrentPenalty() {
        return currentPenalty;
    }

    public ChunkLimiterSettings getSettings() {
        return settings;
    }
}
