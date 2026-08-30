package de.hems.paper.commands;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.warp.ServerConnector;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The way back to the hub.
 * <p>
 * {@code /warp} needs the host to answer before it knows where it may send anybody. On a server that has
 * lost the launcher - or on one that never had a working plugin - that answer never comes, and a player is
 * left with no way out. The lobby is the one destination that is always there, so this asks the proxy
 * directly and needs nothing else to be working.
 */
public class LobbyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können gewarpt werden.");
            return true;
        }
        if (ListenerAdapter.ServerName.LOBBY.equals(ListenerAdapter.getName())) {
            player.sendMessage(ChatColor.YELLOW + "Du bist bereits in der Lobby.");
            return true;
        }
        ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
        return true;
    }
}
