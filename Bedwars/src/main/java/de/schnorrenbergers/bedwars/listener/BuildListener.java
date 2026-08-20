package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.BlockTracker;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.GeneratorSpot;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

/**
 * What may be built and what may be broken.
 * <p>
 * Two rules carry the whole game mode: you may only break what somebody put there during this round, and
 * there are places nobody may build at all. The first needs a memory of every placed block - there is
 * nothing on a block that says who put it there - and the second keeps a generator from being walled in or
 * a spawn from being sealed shut.
 */
public class BuildListener implements Listener {

    private final BlockTracker tracker;

    public BuildListener(Plugin plugin, BlockTracker tracker) {
        this.tracker = tracker;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Block block = event.getBlock();
        ArenaMap arena = game.getArena();
        if (arena != null && block.getY() > arena.getBuildMaxY()) {
            deny(event, player, "build.too-high", "limit", String.valueOf(arena.getBuildMaxY()));
            return;
        }
        if (arena != null && nearProtectedSpot(game, arena, player, block)) {
            deny(event, player, "build.protected");
            return;
        }
        tracker.remember(block);
    }

    /**
     * Beds are not covered here: {@link BedListener} runs first and decides about them.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (event.getBlock().getType().name().endsWith("_BED")) return;

        if (!tracker.wasPlaced(event.getBlock())) {
            deny(event, player, "build.not-yours");
            return;
        }
        tracker.forget(event.getBlock());
    }

    /**
     * What an explosion is allowed to take with it: what players built, and nothing else.
     * <p>
     * Tnt and fireballs are bought to break a defence, not to blow a hole into the map - and a bed that
     * could be blown up would turn the one thing a round is about into a matter of eight gold.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        event.blockList().removeIf(block -> !tracker.wasPlaced(block)
                || block.getType().name().endsWith("_BED"));
        event.blockList().forEach(tracker::forget);
    }

    /**
     * @return whether this block would land somewhere nobody may build
     */
    private boolean nearProtectedSpot(Game game, ArenaMap arena, Player player, Block block) {
        Location at = block.getLocation();
        int generatorRadius = game.getSettings().getGeneratorRadius();
        for (GeneratorSpot generator : arena.getGenerators()) {
            if (within(at, generator.point(), generatorRadius)) return true;
        }
        int shopRadius = game.getSettings().getShopRadius();
        GamePlayer builder = game.get(player);
        GameTeam own = builder == null ? null : builder.getTeam();
        for (GameTeam team : game.getTeams()) {
            TeamSpot spot = arena.getTeam(team.getColor());
            if (spot == null) continue;
            if (within(at, spot.getShop(), shopRadius)) return true;
            if (within(at, spot.getUpgrade(), shopRadius)) return true;
            // a team's own spawn stays open to them: only the enemy is kept from sealing it shut
            if (team != own && within(at, spot.getSpawn(), spot.getProtection())) return true;
        }
        return false;
    }

    /**
     * @param at     where the block would go
     * @param point  the spot to keep clear, may be missing
     * @param radius how far it is kept clear
     * @return whether the block is inside that radius
     */
    private static boolean within(Location at, MapPoint point, double radius) {
        if (point == null || radius <= 0.0d) return false;
        double dx = at.getX() + 0.5d - point.x();
        double dy = at.getY() + 0.5d - point.y();
        double dz = at.getZ() + 0.5d - point.z();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static void deny(org.bukkit.event.Cancellable event, Player player, String message,
                             String... placeholders) {
        event.setCancelled(true);
        Messages.send(player, message, placeholders);
    }

    private static Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}
