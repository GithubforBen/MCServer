package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.util.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Chat that knows about teams.
 * <p>
 * While a round runs, what you say goes to your team - that is the channel people actually need, and
 * having to prefix every call for help would mean nobody uses it. A message starting with {@code @} goes
 * to everybody instead, which is the rarer case and therefore the one that costs a character.
 */
public class ChatListener implements Listener {

    /** What turns a team message into a message to everybody. */
    private static final String GLOBAL_PREFIX = "@";

    public ChatListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Game game = game();
        if (game == null) return;

        Player player = event.getPlayer();
        GamePlayer sender = game.get(player);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());

        boolean global = !game.isRunning() || sender == null || sender.getTeam() == null
                || text.startsWith(GLOBAL_PREFIX);
        if (global) {
            event.setCancelled(true);
            broadcast(game, sender, strip(text));
            return;
        }
        event.setCancelled(true);
        toTeam(sender.getTeam(), sender, text);
    }

    /**
     * @param text what was typed
     * @return it without the marker that made it global
     */
    private static String strip(String text) {
        return text.startsWith(GLOBAL_PREFIX) ? text.substring(GLOBAL_PREFIX.length()).trim() : text;
    }

    private void broadcast(Game game, GamePlayer sender, String text) {
        String team = sender == null || sender.getTeam() == null
                ? Messages.raw("chat.no-team")
                : sender.getTeam().getColor().getDisplayName();
        Component line = Messages.get("chat.global",
                "player", sender == null ? "?" : sender.getName(),
                "team", team,
                "message", text);
        Bukkit.getServer().sendMessage(line);
    }

    private void toTeam(GameTeam team, GamePlayer sender, String text) {
        Component line = Messages.get("chat.team",
                "player", sender.getName(),
                "team", team.getColor().getDisplayName(),
                "message", text);
        for (GamePlayer member : team.getMembers()) {
            Player online = member.getPlayer();
            if (online != null) online.sendMessage(line);
        }
    }

    private static Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}
