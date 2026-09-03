package de.schnorrenbergers.lobby.parkour;

import de.schnorrenbergers.lobby.LobbyWorld;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * What a course looks like from the outside.
 * <p>
 * A parkour laid out as invisible spheres is a parkour nobody can play: the checkpoints are spots in the
 * air with a radius, so without something to see there is nothing that says where one is, whether it has
 * been taken, or which way the course goes on. The rings are drawn per player rather than into the world,
 * which is what lets a runner see their own next target lit up while everybody else sees a plain course.
 * <p>
 * Colours carry the state: what is behind the runner is dark, what is next is yellow, the rest is aqua and
 * the finish is green. Only courses close enough to be seen are drawn at all.
 */
public final class ParkourMarkers {

    /** How often the rings are redrawn, in ticks. Slow enough to be cheap, fast enough not to flicker. */
    private static final int INTERVAL = 8;
    /** How far a course is drawn from, in blocks. */
    private static final double VIEW_DISTANCE = 48.0d;
    /** How many points make up one ring. */
    private static final int RING_POINTS = 12;
    /** How large the dots are. */
    private static final float DOT_SIZE = 1.0f;

    private static final Color NEXT = Color.fromRGB(255, 214, 0);
    private static final Color OPEN = Color.fromRGB(0, 200, 220);
    private static final Color DONE = Color.fromRGB(70, 80, 90);
    private static final Color GOAL = Color.fromRGB(60, 230, 90);

    private final ParkourService parkour;
    private final ParkourStore store;

    public ParkourMarkers(ParkourService parkour, ParkourStore store) {
        this.parkour = parkour;
        this.store = store;
    }

    /**
     * Starts the redraw.
     *
     * @param plugin the lobby plugin, which owns the task
     */
    public void start(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::draw, INTERVAL, INTERVAL);
    }

    private void draw() {
        World world = LobbyWorld.get();
        if (world == null) return;
        for (Player player : world.getPlayers()) {
            ParkourRun run = parkour.runOf(player);
            for (ParkourCourse course : store.all()) {
                drawCourse(player, world, course, run);
            }
        }
    }

    /**
     * Draws one course for one player, in the colours their own run gives it.
     *
     * @param run the run this player is on, or {@code null} when they are not running
     */
    private void drawCourse(Player player, World world, ParkourCourse course, @Nullable ParkourRun run) {
        if (!course.isComplete()) return;
        // a runner is shown their own course only. Every other course would be a second set of rings in
        // the same air, and the one that matters is the one under their feet
        boolean own = run != null && run.getCourse().getName().equals(course.getName());
        if (run != null && !own) return;
        int reached = own ? run.getReached() : -1;

        int checkpoints = course.getCheckpoints().size();
        for (int i = 0; i < checkpoints; i++) {
            ParkourPoint point = course.getCheckpoint(i);
            if (point == null) continue;
            Color color = i < reached ? DONE : i == reached ? NEXT : OPEN;
            ring(player, point.toLocation(world), point.radius(), color);
        }
        ParkourPoint finish = course.getFinish();
        if (finish == null) return;
        // the finish only lights up once it is the thing left to do, so it cannot be mistaken for the
        // next checkpoint halfway through a course
        ring(player, finish.toLocation(world), finish.radius(),
                own && reached >= checkpoints ? NEXT : GOAL);
    }

    /**
     * Draws one ring, doing nothing when the player is too far away to see it.
     */
    private void ring(Player player, Location at, double radius, Color color) {
        if (!player.getWorld().equals(at.getWorld())) return;
        if (player.getLocation().distanceSquared(at) > VIEW_DISTANCE * VIEW_DISTANCE) return;

        Particle.DustOptions dust = new Particle.DustOptions(color, DOT_SIZE);
        double drawn = Math.max(0.6d, radius);
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = 2 * Math.PI * i / RING_POINTS;
            player.spawnParticle(Particle.DUST,
                    at.getX() + Math.cos(angle) * drawn,
                    at.getY() + 0.2d,
                    at.getZ() + Math.sin(angle) * drawn,
                    1, 0.0d, 0.0d, 0.0d, 0.0d, dust);
        }
        // one dot straight above the middle, so a checkpoint can be picked out from a distance and from
        // above - which is where a runner on a course above it is looking from
        player.spawnParticle(Particle.DUST, at.getX(), at.getY() + 1.2d, at.getZ(),
                1, 0.0d, 0.0d, 0.0d, 0.0d, dust);
    }
}
