package de.schnorrenbergers.lobby.parkour;

import de.schnorrenbergers.lobby.LobbyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who is running what, and what happens when they touch something.
 * <p>
 * The whole course is driven from the player's position rather than from blocks: a checkpoint is a spot
 * with a radius, and walking into it is what counts. That is why the course survives somebody breaking a
 * pressure plate, and why a course can be laid out in mid air where no block could be stood on at all.
 */
public class ParkourService {

    /** How many places a leaderboard shows. */
    private static final int BOARD_SIZE = 10;

    private final ParkourStore store;
    private final ParkourHolograms holograms;
    private final Map<UUID, ParkourRun> running = new HashMap<>();

    public ParkourService(ParkourStore store) {
        this.store = store;
        this.holograms = new ParkourHolograms(store);
    }

    public ParkourStore getStore() {
        return store;
    }

    /**
     * @return the floating text of the courses, which has to be redrawn whenever one of them changes
     */
    public ParkourHolograms getHolograms() {
        return holograms;
    }

    /**
     * @param player who to look up
     * @return the run they are in, or {@code null}
     */
    public @Nullable ParkourRun runOf(Player player) {
        return running.get(player.getUniqueId());
    }

    /**
     * Puts a player at the start of a course and starts their clock.
     *
     * @param player who is running
     * @param course what they are running
     */
    public void start(Player player, ParkourCourse course) {
        if (!course.isComplete()) {
            player.sendMessage(Component.text("Diese Strecke ist noch nicht fertig gebaut.",
                    NamedTextColor.RED));
            return;
        }
        World world = LobbyWorld.get();
        if (world == null || course.getStart() == null) return;

        running.put(player.getUniqueId(), new ParkourRun(course));
        player.teleport(course.getStart().toLocation(world));
        player.showTitle(Title.title(
                Component.text(course.getDisplayName(), NamedTextColor.AQUA),
                Component.text("Los!", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(300))));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.4f);

        ParkourStore.Record best = store.best(course.getName(), player.getUniqueId());
        player.sendMessage(best == null
                ? Component.text("Erster Versuch auf " + course.getDisplayName() + ".", NamedTextColor.GRAY)
                : Component.text("Deine Bestzeit: " + format(best.millis()), NamedTextColor.GRAY));
    }

    /**
     * Ends a run without a time.
     *
     * @param player who is giving up
     * @param say    whether to tell them, which a player who simply logged off does not need
     */
    public void quit(Player player, boolean say) {
        if (running.remove(player.getUniqueId()) == null) return;
        if (say) player.sendMessage(Component.text("Lauf abgebrochen.", NamedTextColor.GRAY));
    }

    /**
     * Looks at where a player is standing and moves their run on.
     * <p>
     * Called from every move, so it does as little as it can: the finish first, then the one checkpoint
     * that is actually up next. Checking every checkpoint of the course would let somebody skip half of it
     * by falling into a later one.
     *
     * @param player who moved
     * @param to     where they are now
     */
    public void onMove(Player player, Location to) {
        ParkourRun run = running.get(player.getUniqueId());
        if (run == null) {
            enterStart(player, to);
            return;
        }
        ParkourCourse course = run.getCourse();
        ParkourPoint next = course.getCheckpoint(run.getReached());
        if (next != null && next.reachedFrom(to)) {
            run.reachedOneMore();
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.6f);
            player.sendActionBar(Component.text("Checkpoint " + run.getReached() + "/"
                    + course.getCheckpoints().size() + "  ·  " + format(run.elapsed()),
                    NamedTextColor.AQUA));
            return;
        }
        // only once every checkpoint is behind them, so the finish cannot be walked into from the start
        if (run.getReached() >= course.getCheckpoints().size()
                && course.getFinish() != null && course.getFinish().reachedFrom(to)) {
            finish(player, run);
        }
    }

    /**
     * Starts a run for somebody who walked onto a start without asking for it.
     */
    private void enterStart(Player player, Location to) {
        for (ParkourCourse course : store.all()) {
            if (!course.isComplete() || !course.getStart().reachedFrom(to)) continue;
            start(player, course);
            return;
        }
    }

    /**
     * Stops the clock, writes the time down and says what it was worth.
     */
    private void finish(Player player, ParkourRun run) {
        long millis = run.elapsed();
        running.remove(player.getUniqueId());
        ParkourCourse course = run.getCourse();
        ParkourStore.Record previous = store.best(course.getName(), player.getUniqueId());
        boolean improved = store.submit(course.getName(), player.getUniqueId(), player.getName(), millis);

        player.showTitle(Title.title(
                Component.text(format(millis), NamedTextColor.GREEN),
                Component.text(improved
                        ? (previous == null ? "Erste Zeit auf dieser Strecke" : "Neue Bestzeit!")
                        : "Bestzeit bleibt " + format(previous.millis()), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));
        player.playSound(player, improved ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP,
                1.0f, 1.0f);
        if (improved) {
            // the boards over the courses show the times, so a new one has to reach them
            holograms.refresh();
            player.getServer().sendMessage(Component.text(player.getName() + " hat "
                    + course.getDisplayName() + " in " + format(millis) + " geschafft.",
                    NamedTextColor.AQUA));
        }
    }

    /**
     * Puts a runner back at their last checkpoint, which is what falling off costs.
     *
     * @param player who fell
     * @return whether they were running at all
     */
    public boolean returnToCheckpoint(Player player) {
        ParkourRun run = running.get(player.getUniqueId());
        World world = LobbyWorld.get();
        if (run == null || world == null) return false;
        ParkourPoint spot = run.lastSafeSpot();
        if (spot == null) return false;
        player.setFallDistance(0f);
        player.teleport(spot.toLocation(world));
        player.playSound(player, Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
        return true;
    }

    /**
     * @param course the course
     * @return its leaderboard, fastest first
     */
    public List<ParkourStore.Record> leaderboard(ParkourCourse course) {
        return store.leaderboard(course.getName(), BOARD_SIZE);
    }

    /**
     * @param millis a time
     * @return it as {@code m:ss.mmm}, which is the shape a parkour time is read in
     */
    public static String format(long millis) {
        long minutes = millis / 60_000L;
        long seconds = (millis % 60_000L) / 1000L;
        long rest = millis % 1000L;
        return minutes > 0
                ? String.format("%d:%02d.%03d", minutes, seconds, rest)
                : String.format("%d.%03ds", seconds, rest);
    }
}
