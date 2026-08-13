package de.hems.paper.commands;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.servermanager.ServerManagerUi;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.Server;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /warp} without arguments opens a menu with every running server, {@code /warp <server>} sends the
 * player straight there. Both work for servers that were created while the network was running.
 */
public class WarpCommand implements CommandExecutor, TabCompleter {

    /** Cached names for tab completion, refreshed whenever the command is used. */
    private static volatile List<String> knownServers = new ArrayList<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können gewarpt werden.");
            return true;
        }
        if (args.length == 0) {
            ServerManagerUi.openWarpMenu(player);
            return true;
        }
        String target = args[0];
        PaperContext.async(() -> {
            List<Server> servers;
            try {
                servers = ServerApi.listJoinableServers();
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Die Serverliste ist nicht erreichbar."));
                return;
            }
            refreshCompletions(servers);
            Server match = null;
            for (Server server : servers) {
                if (server.name.equalsIgnoreCase(target)) match = server;
            }
            if (match == null) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Der Server '" + target + "' läuft gerade nicht."));
                return;
            }
            String name = match.name;
            PaperContext.sync(() -> ServerConnector.connect(player, name));
        });
        return true;
    }

    /**
     * @param servers the servers that are currently running
     */
    public static void refreshCompletions(List<Server> servers) {
        List<String> names = new ArrayList<>();
        for (Server server : servers) names.add(server.name);
        knownServers = names;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) return List.of();
        List<String> matches = new ArrayList<>();
        for (String name : knownServers) {
            if (name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) matches.add(name);
        }
        return matches;
    }
}
