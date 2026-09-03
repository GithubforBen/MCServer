package de.schnorrenbergers.survival.featrues.team;

import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes the claims visible while walking, instead of only when somebody types a command.
 * <p>
 * Three things, in the order a player meets them: a title the moment they cross into somebody's land, an
 * action bar that keeps saying whose land it is for as long as they stand on it, and - on request - the
 * chunk borders drawn in particles, because "which side of this line am I on" is a question a title cannot
 * answer.
 * <p>
 * The title is deliberately tied to the <em>owner</em> changing rather than to the chunk changing. Walking
 * across a team's territory crosses a chunk border every sixteen blocks, and a title on each of them would
 * be the reason somebody asks for the feature to be removed again.
 */
public final class ClaimDisplay implements Listener {

    /** How often the action bar is redrawn, in ticks. It fades after about three seconds. */
    private static final int BAR_INTERVAL = 30;
    /** How long the borders stay up after {@code /cteam grenze}, in ticks. */
    private static final int BORDER_TICKS = 20 * 10;
    /** How often they are redrawn, in ticks. */
    private static final int BORDER_INTERVAL = 10;
    /** How many chunks around the player get an outline. 1 means the eight neighbours as well. */
    private static final int BORDER_RADIUS = 1;
    /** One block apart along an edge, and two heights, is enough to read as a wall. */
    private static final double[] BORDER_HEIGHTS = {0.5d, 2.5d};

    /** What everybody is standing on right now, by team name; the empty string means wilderness. */
    private static final Map<UUID, String> standingIn = new ConcurrentHashMap<>();

    private final Plugin plugin;

    public ClaimDisplay(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, ClaimDisplay::drawBars, BAR_INTERVAL, BAR_INTERVAL);
    }

    /* ------------------------------------------------------------------ crossing */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        // the cheap half of this: a move event fires several times a second for everybody, and all but a
        // handful of them are inside the same chunk they started in
        if (from.getWorld() == to.getWorld()
                && from.getBlockX() >> 4 == to.getBlockX() >> 4
                && from.getBlockZ() >> 4 == to.getBlockZ() >> 4) {
            return;
        }
        crossed(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // a teleport is a crossing like any other - /cteam home lands somebody in their own territory and
        // they should be told so, the same way walking in would
        crossed(event.getPlayer(), event.getTo());
    }

    /**
     * Somebody who logs in inside a claim is remembered without a title: the first seconds after a join
     * are already full of messages, and the action bar says where they are anyway.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        standingIn.put(event.getPlayer().getUniqueId(), nameOf(ownerAt(event.getPlayer().getLocation())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        standingIn.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Says whose land somebody just walked onto, if it changed hands since their last step.
     *
     * @param player who moved
     * @param to     where they ended up
     */
    private static void crossed(Player player, @Nullable Location to) {
        if (to == null) return;
        TeamData owner = ownerAt(to);
        String name = nameOf(owner);
        String before = standingIn.put(player.getUniqueId(), name);
        if (before != null && before.equals(name)) return;
        // the first step of a player nobody has seen yet is not a crossing: onJoin remembers where they
        // are, and a null here means the join was missed, not that they came from somewhere
        if (before == null) return;
        showTitle(player, owner);
    }

    /**
     * The title itself: whose land, and whether this is a way in or a way out.
     */
    private static void showTitle(Player player, @Nullable TeamData owner) {
        Component headline = owner == null
                ? Component.text("Wildnis", NamedTextColor.GRAY)
                : Component.text()
                        .append(Component.text("» ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(owner.getName(), colourOf(owner))
                                .decorate(TextDecoration.BOLD))
                        .append(Component.text(" «", NamedTextColor.DARK_GRAY))
                        .build();
        Component sub = Component.text(owner == null ? "Verlassen" : "Betreten", NamedTextColor.GRAY);
        player.showTitle(Title.title(headline, sub, Title.Times.times(
                Duration.ofMillis(300), Duration.ofMillis(1500), Duration.ofMillis(500))));
    }

    /* ----------------------------------------------------------------- action bar */

    /**
     * Keeps the name of whoever owns the ground over everybody's hotbar.
     * <p>
     * Redrawn rather than sent once, because an action bar fades on its own after a few seconds. Nothing
     * is sent to somebody standing in the wilderness - an empty action bar is the answer there, and it is
     * also what keeps this from fighting with anything else that wants that line.
     */
    private static void drawBars() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            TeamData owner = ownerAt(player.getLocation());
            if (owner == null) continue;
            Component bar = Component.text(owner.getName(), colourOf(owner));
            TeamData own = TeamService.getTeamOf(player.getUniqueId());
            if (own != null && own.getName().equalsIgnoreCase(owner.getName())) {
                bar = bar.append(Component.text(" · dein Team", NamedTextColor.DARK_GRAY));
            }
            player.sendActionBar(bar);
        }
    }

    /* -------------------------------------------------------------------- borders */

    /**
     * Draws the outlines of the claimed chunks around somebody, for a few seconds.
     * <p>
     * Only that player sees them: a light show around everybody who ever asked where a border runs would
     * be a light show for the whole server. The outlines follow whoever asked, so walking along a border
     * while it is up shows the next chunk as well.
     *
     * @param plugin the plugin it runs on
     * @param player who asked
     */
    public static void showBorders(Plugin plugin, Player player) {
        UUID id = player.getUniqueId();
        new BukkitRunnable() {
            int elapsed;

            @Override
            public void run() {
                Player still = Bukkit.getPlayer(id);
                if (still == null || elapsed >= BORDER_TICKS) {
                    cancel();
                    return;
                }
                elapsed += BORDER_INTERVAL;
                drawBordersAround(still);
            }
        }.runTaskTimer(plugin, 0L, BORDER_INTERVAL);
    }

    private static void drawBordersAround(Player player) {
        Location at = player.getLocation();
        int centreX = at.getBlockX() >> 4;
        int centreZ = at.getBlockZ() >> 4;
        for (int x = centreX - BORDER_RADIUS; x <= centreX + BORDER_RADIUS; x++) {
            for (int z = centreZ - BORDER_RADIUS; z <= centreZ + BORDER_RADIUS; z++) {
                TeamData owner = ClaimManager.getTeamDataOfChunk(at.getWorld(), x, z);
                boolean here = x == centreX && z == centreZ;
                if (owner == null && !here) continue;
                // the chunk somebody is standing in is outlined even when it belongs to nobody: "is this
                // free?" is the other half of the question this command is asked
                Color colour = owner == null
                        ? Color.WHITE
                        : rgbOf(colourOf(owner));
                outline(player, x, z, at.getY(), colour);
            }
        }
    }

    /**
     * Draws one chunk's four edges.
     *
     * @param player who sees it
     * @param chunkX which chunk
     * @param chunkZ which chunk
     * @param y      the height it is drawn from, which is the player's feet
     * @param colour what colour it is
     */
    private static void outline(Player player, int chunkX, int chunkZ, double y, Color colour) {
        Particle.DustOptions dust = new Particle.DustOptions(colour, 1.2f);
        double west = chunkX << 4;
        double north = chunkZ << 4;
        for (double step = 0.0d; step <= 16.0d; step += 1.0d) {
            for (double height : BORDER_HEIGHTS) {
                double atY = y + height;
                point(player, west + step, atY, north, dust);
                point(player, west + step, atY, north + 16.0d, dust);
                point(player, west, atY, north + step, dust);
                point(player, west + 16.0d, atY, north + step, dust);
            }
        }
    }

    private static void point(Player player, double x, double y, double z, Particle.DustOptions dust) {
        player.spawnParticle(Particle.DUST, x, y, z, 1, 0.0d, 0.0d, 0.0d, 0.0d, dust);
    }

    /* -------------------------------------------------------------------- lookups */

    /**
     * @param at somewhere
     * @return the team whose chunk that is, or {@code null} for the wilderness
     */
    public static @Nullable TeamData ownerAt(Location at) {
        if (at == null || at.getWorld() == null) return null;
        return ClaimManager.getTeamDataOfChunk(at.getWorld(), at.getBlockX() >> 4, at.getBlockZ() >> 4);
    }

    /**
     * @param team a team, or {@code null}
     * @return its colour as the chat and the particles want it, white when it has none
     */
    public static NamedTextColor colourOf(@Nullable TeamData team) {
        if (team == null || team.getColor() == null) return NamedTextColor.WHITE;
        NamedTextColor colour = NamedTextColor.NAMES.value(team.getColor().toLowerCase(Locale.ROOT));
        return colour == null ? NamedTextColor.WHITE : colour;
    }

    /**
     * @param colour a chat colour
     * @return the same colour as particles take it
     */
    public static Color rgbOf(NamedTextColor colour) {
        return Color.fromRGB(colour.value());
    }

    private static String nameOf(@Nullable TeamData team) {
        return team == null ? "" : team.getName();
    }
}
