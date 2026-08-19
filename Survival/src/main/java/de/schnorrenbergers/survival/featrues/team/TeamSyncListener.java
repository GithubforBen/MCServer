package de.schnorrenbergers.survival.featrues.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.team.TeamUpdatedEvent;
import de.hems.paper.PaperContext;
import de.hems.paper.team.TeamService;
import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Keeps what players see in step with the teams stored on the launcher.
 * <p>
 * The scoreboard is only a display of the real thing: it carries the tag, the colour and friendly fire, and
 * it is rebuilt from the stored team whenever that changes. Nothing is ever read back out of it, so it
 * cannot drift into being a second source of truth.
 */
public class TeamSyncListener implements Listener {

    /** How long to keep waiting for the teams before giving up on the migration, in ticks. */
    private static final long MIGRATION_TIMEOUT_TICKS = 20L * 60L;

    public TeamSyncListener() {
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        scheduleMigration();
        // a team changed anywhere in the network - mirror it here
        ListenerAdapter.register(TeamUpdatedEvent.class,
                event -> PaperContext.sync(TeamManager::syncAllOnline));
    }

    /**
     * Moves the teams of an older install onto the launcher, once they have been loaded from there.
     * <p>
     * Waiting matters: migrating before the list has arrived would create teams that are already stored,
     * so this only runs when the local copy is known to be current.
     */
    private void scheduleMigration() {
        long deadline = System.currentTimeMillis() + MIGRATION_TIMEOUT_TICKS * 50L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(Survival.getInstance(), task -> {
            if (TeamService.isLoaded()) {
                task.cancel();
                TeamMigration.run();
                PaperContext.sync(TeamManager::syncAllOnline);
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                task.cancel();
                Survival.getInstance().getLogger().warning(
                        "The teams never arrived from the host - old teams were not migrated.");
            }
        }, 40L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        TeamManager.syncPlayer(event.getPlayer());
    }
}
