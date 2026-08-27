package de.hems.paper.warp;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Warps players to any server of the network.
 * <p>
 * The proxy channel is used instead of a fixed list of destinations, so every server the proxy knows about
 * is a valid warp target - including the ones that were created while the network was already running.
 */
public final class ServerConnector {

    /** The plugin messaging channel velocity listens on. */
    public static final String CHANNEL = "BungeeCord";

    /**
     * Whoever holds this - and every operator - may warp even when the host is not answering, so an admin
     * is never stuck on a server whose network connection is the thing that is broken.
     */
    public static final String ALWAYS_ALLOWED_PERMISSION = "network.warp.always";

    private ServerConnector() {
    }

    /**
     * @param player the player to check
     * @return whether they may warp without the host confirming the destination first
     */
    public static boolean mayWarpUnchecked(Player player) {
        return player.isOp() || player.hasPermission(ALWAYS_ALLOWED_PERMISSION);
    }

    /**
     * Registers the channel that is needed to send players to another server. Has to be called once per
     * plugin, normally in {@code onEnable}.
     *
     * @param plugin the plugin that warps players
     */
    public static void register(Plugin plugin) {
        PaperContext.setPlugin(plugin);
        if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, CHANNEL)) {
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        }
    }

    /**
     * Sends a player to another server.
     *
     * @param player     the player to warp
     * @param serverName the name of the destination server
     * @return whether the request could be sent
     */
    public static boolean connect(Player player, String serverName) {
        return connect(player, ListenerAdapter.ServerName.valueOf(serverName));
    }

    /**
     * Sends a player to another server.
     *
     * @param player     the player to warp
     * @param serverName the destination server
     * @return whether the request could be sent
     */
    public static boolean connect(Player player, ListenerAdapter.ServerName serverName) {
        if (serverName == null || serverName.isReserved()) {
            player.sendMessage(ChatColor.RED + "Auf diesen Server kann man nicht warpen.");
            return false;
        }
        if (serverName.equals(ListenerAdapter.getName())) {
            player.sendMessage(ChatColor.YELLOW + "Du bist bereits auf " + serverName + ".");
            return false;
        }
        Plugin plugin = PaperContext.getPlugin();
        register(plugin);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(serverName.toString());
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Der Warp konnte nicht gesendet werden.");
            return false;
        }
        player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        player.sendMessage(ChatColor.GRAY + "Du wirst nach " + ChatColor.AQUA + serverName + ChatColor.GRAY + " gewarpt...");
        return true;
    }
}
