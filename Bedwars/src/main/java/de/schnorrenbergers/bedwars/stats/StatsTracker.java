package de.schnorrenbergers.bedwars.stats;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.api.BedwarsGameEndEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.timeline.Timeline;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Counts nothing, keeps everything.
 * <p>
 * The numbers themselves live where they are made - on the players and the teams - because a second place
 * that counts kills is a second place that can be wrong about them. All this does is take the snapshot at
 * the one moment it is complete, and hand it to whoever keeps it.
 */
public final class StatsTracker implements Listener {

    private final StatsRepository repository;

    /**
     * @param plugin     the plugin to listen with
     * @param repository where the numbers go
     */
    public StatsTracker(Plugin plugin, StatsRepository repository) {
        this.repository = repository;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * At MONITOR: everything else that reacts to the end of a round has had its say, so what is written
     * down is the round as it finally stood.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(BedwarsGameEndEvent event) {
        repository.save(snapshot(event.getGame()));
    }

    /**
     * @param game the round
     * @return where it stands right now, which is what {@code /bw stats} shows and what is written down
     */
    public RoundStats snapshot(Game game) {
        Timeline timeline = game.getTimeline();
        return RoundStats.of(game, timeline == null ? 0 : timeline.getElapsedSeconds(),
                Bedwars.getInstance().getTimelineSettings().getWeights());
    }
}
