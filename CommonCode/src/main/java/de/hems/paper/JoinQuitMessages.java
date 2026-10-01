package de.hems.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * The same chat line on every server when somebody comes or goes: {@code >> Name} in green, {@code << Name}
 * in red, instead of vanilla's "joined the game".
 * <p>
 * Set early, so a server with a reason of its own has the last word - hunger games hides both, the admin
 * disguise announces somebody else.
 */
public final class JoinQuitMessages implements Listener {

    private static boolean registered;

    private JoinQuitMessages() {
    }

    /**
     * @param plugin the plugin of this server
     */
    public static void register(Plugin plugin) {
        if (registered) return;
        registered = true;
        Bukkit.getPluginManager().registerEvents(new JoinQuitMessages(), plugin);
    }

    /**
     * @param name who came
     * @return the line for it
     */
    public static Component join(String name) {
        return Component.text(">> ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.WHITE));
    }

    /**
     * @param name who left
     * @return the line for it
     */
    public static Component quit(String name) {
        return Component.text("<< ", NamedTextColor.RED).append(Component.text(name, NamedTextColor.WHITE));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(join(event.getPlayer().getName()));
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(quit(event.getPlayer().getName()));
    }
}
