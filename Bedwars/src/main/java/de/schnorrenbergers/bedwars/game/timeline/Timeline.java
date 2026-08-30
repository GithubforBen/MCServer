package de.schnorrenbergers.bedwars.game.timeline;

import de.schnorrenbergers.bedwars.api.BedwarsBedDestroyEvent;
import de.schnorrenbergers.bedwars.api.BedwarsGameEndEvent;
import de.schnorrenbergers.bedwars.api.BedwarsTimelineEvent;
import de.schnorrenbergers.bedwars.config.TimelineSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.Standings;
import de.schnorrenbergers.bedwars.generator.GeneratorManager;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * The clock of the round.
 * <p>
 * Everything that happens on its own - faster generators, the beds falling, the dragons, the final
 * whistle - happens here, in one list that is walked once a second. That is what keeps a bedwars round
 * from lasting forever: the round has an opinion about how long it should take, and acts on it.
 * <p>
 * The clock counts from the moment the round started, not from when the server did. A server that stood
 * around waiting for players for ten minutes would otherwise open with bed destruction.
 */
public final class Timeline {

    private static final Sound MAJOR = Sound.sound(Key.key("entity.wither.spawn"), Sound.Source.MASTER, 1f, 1f);

    private final TimelineSettings settings;
    /** Read again whenever a round starts, so an edited schedule needs a reload and not a restart. */
    private List<TimelineEvent> events;

    /** The tick the round began on, -1 while it has not. */
    private long startTick = -1L;
    /** Which event is next; everything before it has happened. */
    private int index;
    private int elapsed;
    private boolean suddenDeath;

    public Timeline(TimelineSettings settings) {
        this.settings = settings;
        this.events = settings.getEvents();
    }

    /**
     * Starts the clock.
     *
     * @param ticks where the loop stands right now
     */
    public void start(long ticks) {
        events = settings.getEvents();
        startTick = ticks;
        elapsed = 0;
        index = 0;
        suddenDeath = false;
    }

    /**
     * Runs everything whose time has come.
     *
     * @param game  the round
     * @param ticks where the loop stands
     */
    public void tick(Game game, long ticks) {
        if (startTick < 0L || ticks % 20L != 0L) return;
        elapsed = (int) ((ticks - startTick) / 20L);
        while (index < events.size() && events.get(index).seconds() <= elapsed && !game.isEnded()) {
            run(game, events.get(index++));
        }
    }

    /**
     * Runs the next event now and moves the clock to it.
     * <p>
     * This is a testing tool and it is deliberately part of the timeline rather than of the command:
     * waiting thirty minutes to find out whether bed destruction works is not a test anybody runs twice.
     *
     * @param game the round
     * @return the event that was set off, or {@code null} when there are none left
     */
    public @Nullable TimelineEvent skip(Game game) {
        if (index >= events.size()) return null;
        TimelineEvent event = events.get(index++);
        if (startTick >= 0L && event.seconds() > elapsed) {
            // move the start backwards instead of the clock forwards, so the events after this one keep
            // their distance to it
            startTick -= (long) (event.seconds() - elapsed) * 20L;
            elapsed = event.seconds();
        }
        run(game, event);
        return event;
    }

    // ----------------------------------------------------------------- actions

    private void run(Game game, TimelineEvent event) {
        // before the action, not after: an addon that wants to be ahead of bed destruction has nowhere
        // else to stand, and one that wants to come after it can simply act a tick later
        Bukkit.getPluginManager().callEvent(new BedwarsTimelineEvent(game, event));
        switch (event.action()) {
            case GENERATOR_TIER -> raiseGenerators(game, event);
            case BED_DESTRUCTION -> destroyBeds(game, event);
            case SUDDEN_DEATH -> releaseDragons(game, event);
            case GAME_END -> decide(game);
            case ANNOUNCE -> announce(event, "timeline.event");
        }
    }

    /**
     * Moves the generators in the middle up a level. The ones in the bases belong to the forge upgrade and
     * are deliberately left alone.
     */
    private void raiseGenerators(Game game, TimelineEvent event) {
        GeneratorManager generators = game.getGenerators();
        if (generators != null) generators.setTier(event.generator(), null, event.tier());
        announce(event, "timeline.generator");
    }

    /**
     * Takes every bed that is still standing.
     */
    private void destroyBeds(Game game, TimelineEvent event) {
        announce(event, "timeline.bed-destruction");
        for (GameTeam team : game.getTeams()) {
            if (!team.isBedAlive()) continue;
            BedwarsBedDestroyEvent destroy = new BedwarsBedDestroyEvent(game, team, null);
            Bukkit.getPluginManager().callEvent(destroy);
            if (destroy.isCancelled()) continue;
            team.setBedAlive(false);
            takeBed(game, team);
            for (GamePlayer member : team.getPlayingMembers()) {
                Player player = member.getPlayer();
                if (player != null) Messages.send(player, "timeline.bed-destruction-yours");
            }
        }
    }

    /**
     * Takes the two blocks of one bed out of the world.
     * <p>
     * Both halves, and without dropping anything: half a bed left lying in a base looks like a bed that is
     * still there, which is the one thing a player must never be wrong about.
     */
    private void takeBed(Game game, GameTeam team) {
        World world = game.getWorld();
        if (world == null || game.getArena() == null) return;
        TeamSpot spot = game.getArena().getTeam(team.getColor());
        MapPoint point = spot == null ? null : spot.getBed();
        if (point == null) return;

        Location at = point.toLocation(world);
        Block block = at.getBlock();
        if (block.getBlockData() instanceof Bed bed) {
            Block other = block.getRelative(bed.getPart() == Bed.Part.HEAD
                    ? bed.getFacing().getOppositeFace() : bed.getFacing());
            if (other.getBlockData() instanceof Bed) other.setType(Material.AIR, false);
        }
        if (block.getType().name().endsWith("_BED")) block.setType(Material.AIR, false);
        world.playSound(at, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
    }

    /**
     * Lets the dragons out.
     */
    private void releaseDragons(Game game, TimelineEvent event) {
        suddenDeath = true;
        int spawned = game.getDragons() == null ? 0 : game.getDragons().spawn(game);
        announce(event, "timeline.sudden-death");
        if (spawned == 0) {
            Bukkit.getLogger().warning("[Bedwars] Sudden death spawned no dragons - the map has no middle"
                    + " to put them over.");
        }
    }

    /**
     * Ends the round on time, by the score.
     */
    private void decide(Game game) {
        Standings.Weights weights = settings.getWeights();
        List<Standings.TeamScore> ranking = Standings.rankTeams(game, weights);
        Messages.broadcast("end.time-limit");
        Messages.broadcast("end.score.header");
        for (Standings.TeamScore score : ranking) {
            Messages.broadcast(score.team().isAlive() ? "end.score.entry" : "end.score.entry-out",
                    "team", score.team().getColor().getDisplayName(),
                    "initial", score.team().getColor().getInitial(),
                    "points", String.valueOf(score.points()),
                    "beds", String.valueOf(score.beds()),
                    "finals", String.valueOf(score.finals()),
                    "kills", String.valueOf(score.kills()));
        }
        game.end(Standings.winner(game, ranking), BedwarsGameEndEvent.Reason.TIME_LIMIT);
    }

    /**
     * Says what just happened, on everybody's screen when it is one of the big ones.
     *
     * @param event what happened
     * @param key   which text says so
     */
    private void announce(TimelineEvent event, String key) {
        Messages.broadcast(key, "event", Text.plain(event.displayName()));
        // and what it means, once, as it happens. Most players never type /bw timeline, and the sidebar
        // only ever had room for the name and the countdown
        if (event.hasDescription()) {
            Messages.broadcast("timeline.explained", "description", event.description());
        }
        if (!event.isMajor()) return;
        Bukkit.getServer().showTitle(Title.title(
                Messages.get(key + "-title"),
                Messages.get(key + "-subtitle"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(700))));
        Bukkit.getServer().playSound(MAJOR);
    }

    // ----------------------------------------------------------------- lookups

    /**
     * @return what happens next, or {@code null} once the list is done
     */
    public @Nullable TimelineEvent getNext() {
        return index < events.size() ? events.get(index) : null;
    }

    /**
     * @return how many seconds until the next event, 0 when there is none left
     */
    public int getSecondsUntilNext() {
        TimelineEvent next = getNext();
        return next == null ? 0 : Math.max(0, next.seconds() - elapsed);
    }

    /**
     * @return how long the round has been running, in seconds
     */
    public int getElapsedSeconds() {
        return elapsed;
    }

    /**
     * @return whether the round is past its sudden death
     */
    public boolean isSuddenDeath() {
        return suddenDeath;
    }

    /**
     * @return whether the clock is running
     */
    public boolean isStarted() {
        return startTick >= 0L;
    }

    /**
     * @param event one of the events
     * @return whether it has already happened
     */
    public boolean hasHappened(TimelineEvent event) {
        int at = events.indexOf(event);
        return at >= 0 && at < index;
    }

    public List<TimelineEvent> getEvents() {
        return events;
    }
}
