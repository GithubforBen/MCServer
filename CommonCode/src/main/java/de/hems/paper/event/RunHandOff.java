package de.hems.paper.event;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.ServerTemplate;
import de.hems.types.event.EventData;
import de.hems.types.event.RunData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Takes a team that waits for the server of its next run and warps it there once it is ready.
 * <p>
 * After a {@code /reset} the team does not wait on the server of the run it just threw away - that server
 * would hold its memory for the whole minute the next world takes to generate. It goes to the lobby
 * instead, the old server switches itself off, and the warp to the new one is sent from wherever the team
 * now stands. That is what this does: whoever joins with a run that was just started and never played is
 * sent on as soon as its server accepts players.
 */
public final class RunHandOff implements Listener {

    /**
     * How long a run counts as just started. A little longer than a warp waits for a server to come up -
     * after that the run is reached through the event panel like any other.
     */
    private static final long FRESH_MS = 6L * 60L * 1000L;
    /** How long after a join to look, so the run announced a moment ago has arrived here. */
    private static final long LOOK_DELAY_TICKS = 40L;

    /** Who is being sent on right now, so a second join in the meantime does not send them twice. */
    private final Set<UUID> sending = ConcurrentHashMap.newKeySet();
    private final Plugin plugin;

    private RunHandOff(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param plugin the plugin the listener belongs to
     */
    static void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new RunHandOff(plugin), plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(id);
            if (player == null || sending.contains(id)) return;
            RunData run = waitingRunOf(id);
            if (run == null) return;
            sending.add(id);
            player.sendMessage(Component.text("Dein nächster Lauf wird vorbereitet - du wirst verbunden, "
                    + "sobald der Server bereit ist.", NamedTextColor.GREEN));
            ServerStartup.ensureAndWarp(List.of(player), run.getServerName(), ServerTemplate.EVENT);
            // the warp gives up by itself after a few minutes, and a later join may try again
            Bukkit.getScheduler().runTaskLater(plugin, () -> sending.remove(id), FRESH_MS / 50L);
        }, LOOK_DELAY_TICKS);
    }

    /**
     * @param player a player
     * @return the run that player waits to be sent to, or {@code null}
     */
    static RunData waitingRunOf(UUID player) {
        String self = ListenerAdapter.getName().toString();
        long now = System.currentTimeMillis();
        for (EventData event : EventService.getEvents()) {
            if (!event.getType().isTimed()) continue;
            RunData run = RunService.getActiveRunOf(event.getId(), player);
            if (run == null || run.getServerName() == null || self.equals(run.getServerName())) continue;
            // not one tick played and only just started: nobody has been on it yet, so this is the team on
            // its way there and not somebody who left a run to have a break
            if (run.getElapsedTicksRaw() > 0 || run.getActiveSince() > 0) continue;
            if (now - run.getStartedAt() > FRESH_MS) continue;
            return run;
        }
        return null;
    }
}
