package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.api.BedwarsBedDestroyEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * The beds.
 * <p>
 * Everything about a round hangs off these blocks, so the rules around them are deliberately strict: your
 * own bed cannot be broken, a bed that is already gone cannot be broken again, and a bed that falls is
 * announced to everybody - loudly to the team that just lost it, because they have to know before they
 * next die rather than after.
 */
public class BedListener implements Listener {

    /** How far from the written down spot a block still counts as that bed - a bed is two blocks. */
    private static final double BED_RANGE = 1.6d;

    private static final Sound BROKEN = Sound.sound(Key.key("entity.ender_dragon.growl"), Sound.Source.MASTER, 1f, 1f);

    public BedListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        Block block = event.getBlock();
        if (!block.getType().name().endsWith("_BED")) return;

        GameTeam owner = teamOfBed(game, block);
        if (owner == null) return;

        Player player = event.getPlayer();
        GamePlayer breaker = game.get(player);
        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (breaker != null && breaker.getTeam() == owner) {
            event.setCancelled(true);
            Messages.send(player, "bed.own");
            return;
        }
        if (!owner.isBedAlive()) {
            event.setCancelled(true);
            return;
        }

        BedwarsBedDestroyEvent destroy = new BedwarsBedDestroyEvent(game, owner, breaker);
        Bukkit.getPluginManager().callEvent(destroy);
        if (destroy.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        event.setDropItems(false);
        owner.setBedAlive(false);
        if (breaker != null) breaker.addBedBroken();

        Messages.broadcast("bed.destroyed",
                "team", owner.getColor().getDisplayName(),
                "player", breaker == null ? Messages.raw("bed.nobody") : breaker.getName());
        announceToOwner(owner);
    }

    /**
     * Tells the team that lost its bed, in the middle of their screen and with a sound they cannot miss.
     *
     * @param owner whose bed fell
     */
    private void announceToOwner(GameTeam owner) {
        Title title = Title.title(
                Messages.get("bed.title"),
                Messages.get("bed.subtitle"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500)));
        for (GamePlayer member : owner.getPlayingMembers()) {
            Player player = member.getPlayer();
            if (player == null) continue;
            player.showTitle(title);
            player.playSound(BROKEN);
        }
    }

    /**
     * @param game  the round
     * @param block a bed block that was broken
     * @return whose bed it is, or {@code null} when it is a bed somebody placed themselves
     */
    private @Nullable GameTeam teamOfBed(Game game, Block block) {
        ArenaMap arena = game.getArena();
        if (arena == null) return null;
        Location at = block.getLocation();
        for (GameTeam team : game.getTeams()) {
            TeamSpot spot = arena.getTeam(team.getColor());
            if (spot == null) continue;
            MapPoint bed = spot.getBed();
            if (bed == null) continue;
            double dx = at.getX() + 0.5d - bed.x();
            double dy = at.getY() - bed.y();
            double dz = at.getZ() + 0.5d - bed.z();
            if (dx * dx + dy * dy + dz * dz <= BED_RANGE * BED_RANGE) return team;
        }
        return null;
    }

    private static Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}
