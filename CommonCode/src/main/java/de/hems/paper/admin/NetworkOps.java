package de.hems.paper.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.admin.OpChangedEvent;
import de.hems.paper.PaperContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Applies operator rights that were handed out somewhere else.
 * <p>
 * The launcher keeps the list every new server is built from, but a server that is already running was
 * built from the old one. So it listens: a change announced by the launcher is applied here, which is also
 * what writes it into this server's own {@code ops.json} and makes it survive the restart it did not need.
 */
public final class NetworkOps {

    private static boolean initialized;

    private NetworkOps() {
    }

    /**
     * @param plugin the plugin the work belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        PaperContext.setPlugin(plugin);
        ListenerAdapter.register(OpChangedEvent.class, event -> apply((OpChangedEvent) event));
    }

    private static void apply(OpChangedEvent event) {
        if (event.getPlayerId() == null) return;
        // setOp touches the server's op list, which is main thread work
        PaperContext.sync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(event.getPlayerId());
            if (target.isOp() == event.isOperator()) return;
            target.setOp(event.isOperator());
            Bukkit.getLogger().info("[Network] " + event.getPlayerName()
                    + (event.isOperator() ? " is now an operator." : " is no longer an operator."));
            Player online = target.getPlayer();
            if (online == null) return;
            online.sendMessage(Component.text(event.isOperator()
                            ? "Du hast jetzt Operator-Rechte im ganzen Netzwerk."
                            : "Deine Operator-Rechte wurden entfernt.",
                    event.isOperator() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        });
    }
}
