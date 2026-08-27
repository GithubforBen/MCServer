package de.hems.paper.commands;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.servermanager.ServerManagerUi;
import de.hems.paper.warp.ServerConnector;
import de.hems.paper.warp.ServerStartup;
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
            List<Server> servers = new ArrayList<>();
            try {
                for (Server server : ServerApi.listServers()) if (server != null) servers.add(server);
            } catch (Exception e) {
                // the host being unreachable is exactly when an admin most needs to get off this server,
                // and the proxy knows the destination whether or not our own list lookup worked
                unreachable(player, target);
                return;
            }
            List<Server> joinable = new ArrayList<>();
            for (Server server : servers) if (server.isJoinable()) joinable.add(server);
            refreshCompletions(joinable);
            Server match = null;
            for (Server server : servers) {
                if (server.name.equalsIgnoreCase(target)) match = server;
            }
            if (match == null) {
                unreachable(player, target);
                return;
            }
            Server found = match;
            if (found.isJoinable()) {
                PaperContext.sync(() -> ServerConnector.connect(player, found.name));
                return;
            }
            if (found.isStartingUp()) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.GRAY + found.name
                        + " ist noch nicht bereit - du wirst verbunden, sobald er es ist."));
                ServerStartup.warpWhenReady(player, found.name);
                return;
            }
            PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Der Server '" + target + "' läuft gerade nicht."));
        });
        return true;
    }

    /**
     * What to do when the host cannot confirm the destination.
     * <p>
     * For a normal player that is the end of it - sending them to a server that may not exist only gets
     * them kicked by the proxy. An admin is a different case: they are usually asking precisely because
     * something is broken, and being told "no" by a broken component is how somebody ends up stuck on a
     * server they cannot leave. So the warp is sent anyway and the proxy decides.
     *
     * @param player who wants to warp
     * @param target where to
     */
    private static void unreachable(Player player, String target) {
        PaperContext.sync(() -> {
            if (!ServerConnector.mayWarpUnchecked(player)) {
                player.sendMessage(ChatColor.RED + "Der Server '" + target + "' läuft gerade nicht.");
                player.sendMessage(ChatColor.GRAY + "Mit /lobby kommst du zurück in die Lobby.");
                return;
            }
            player.sendMessage(ChatColor.YELLOW + "Der Host bestätigt '" + target
                    + "' nicht - der Warp wird trotzdem versucht.");
            ServerConnector.connect(player, target);
        });
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
