package de.schnorrenbergers.bedwars.cosmetic;

import de.hems.paper.cosmetic.Gadgets;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.api.BedwarsGameStateChangeEvent;
import de.schnorrenbergers.bedwars.api.BedwarsPlayerRespawnEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.phase.PhaseType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * The gadgets, as far as a bedwars round is concerned.
 * <p>
 * What a gadget <em>does</em> is not here and must not be: the endless pearl and the grappling hook are
 * the same two things on every server of this network, so they live with the rest of the cosmetics. What
 * is bedwars' own is exactly two answers - when somebody is in the round rather than watching it, and at
 * which moments they get their gadget handed to them.
 */
public class GadgetListener implements Listener {

    private final Plugin plugin;

    public GadgetListener(Plugin plugin) {
        this.plugin = plugin;
        // a spectator with a grappling hook is somebody flying around the map of a round they are out of
        Gadgets.setGuard(GadgetListener::isPlaying);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStateChange(BedwarsGameStateChangeEvent event) {
        if (event.getTo() != PhaseType.RUNNING) return;
        Game game = event.getGame();
        // a tick later: the running phase is handing out the starting kit right now, and anything given
        // before that is given to an inventory that is about to be cleared
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (GamePlayer participant : game.getPlayers()) {
                if (!participant.isPlaying()) continue;
                Player player = participant.getPlayer();
                if (player != null) Gadgets.handOut(player, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(BedwarsPlayerRespawnEvent event) {
        GamePlayer participant = event.getPlayer();
        // a gadget that is gone the first time its owner dies is a gadget they bought once
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = participant.getPlayer();
            if (player != null) Gadgets.handOut(player, false);
        });
    }

    /**
     * @param player somebody
     * @return whether they are actually in the round rather than watching it
     */
    private static boolean isPlaying(Player player) {
        Bedwars plugin = Bedwars.getInstance();
        Game game = plugin == null ? null : plugin.getGame();
        if (game == null || !game.isRunning()) return false;
        GamePlayer participant = game.get(player);
        return participant != null && participant.isPlaying();
    }
}
