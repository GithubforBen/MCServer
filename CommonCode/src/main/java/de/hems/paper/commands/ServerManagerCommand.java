package de.hems.paper.commands;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.servermanager.ServerManagerUi;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;
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
 * {@code /servermanger} opens the server manager. It also has a command form so servers can be created from
 * command blocks or scripts:
 * <pre>
 * /servermanger create &lt;name&gt; [vorlage] [ram] [plugin,plugin,...]
 * /servermanger stop &lt;name&gt;
 * /servermanger restart &lt;name&gt;
 * /servermanger list
 * </pre>
 */
public class ServerManagerCommand implements TabCompleter, CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You are not allowed to use this command!");
            return true;
        }
        if (args.length == 0) {
            if (!CustomInventoryListener.hasBeenRegistered()) {
                sender.sendMessage(ChatColor.RED + "CustomInventory has not been registered yet!");
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Nur Spieler können das Menü öffnen - nutze /servermanger create <name>.");
                return true;
            }
            ServerManagerUi.openServerList(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(sender, args);
            case "stop" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/servermanger stop <name>");
                    return true;
                }
                run(sender, () -> ServerApi.stopServer(args[1]), args[1] + " wird gestoppt.");
            }
            case "restart" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/servermanger restart <name>");
                    return true;
                }
                run(sender, () -> ServerApi.restartServer(args[1]), args[1] + " wird neu gestartet.");
            }
            case "list" -> PaperContext.async(() -> {
                try {
                    StringBuilder builder = new StringBuilder(ChatColor.AQUA + "Server:");
                    for (de.hems.types.Server server : ServerApi.listServers()) {
                        builder.append("\n").append(ChatColor.GRAY).append(" - ").append(ChatColor.WHITE)
                                .append(server.name).append(ChatColor.GRAY).append(" (").append(server.memory)
                                .append(" MB, Port ").append(server.port).append(")");
                    }
                    String message = builder.toString();
                    PaperContext.sync(() -> sender.sendMessage(message));
                } catch (Exception e) {
                    PaperContext.sync(() -> sender.sendMessage(ChatColor.RED + "Der Host antwortet gerade nicht."));
                }
            });
            default -> sender.sendMessage(ChatColor.RED + "/servermanger [create|stop|restart|list]");
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/servermanger create <name> [vorlage] [ram] [plugin,plugin,...]");
            return;
        }
        String name = args[1];
        ServerTemplate template = args.length > 2 ? ServerTemplate.find(args[2]) : ServerTemplate.EVENT;
        if (template == null) {
            sender.sendMessage(ChatColor.RED + "Unbekannte Vorlage '" + args[2] + "'.");
            return;
        }
        Integer memory = null;
        if (args.length > 3) {
            try {
                memory = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "'" + args[3] + "' ist keine Zahl.");
                return;
            }
        }
        List<FileType.PLUGIN> plugins = new ArrayList<>();
        if (args.length > 4) {
            for (String raw : args[4].split(",")) {
                try {
                    plugins.add(FileType.PLUGIN.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Unbekanntes Plugin '" + raw + "'.");
                    return;
                }
            }
        }
        ServerTemplate finalTemplate = template;
        Integer finalMemory = memory;
        run(sender, () -> ServerApi.createServer(name, finalTemplate, finalMemory, plugins),
                name + " wird erstellt.");
    }

    private interface ApiCall {
        void run() throws Exception;
    }

    private void run(CommandSender sender, ApiCall call, String successMessage) {
        PaperContext.async(() -> {
            try {
                call.run();
            } catch (Exception e) {
                PaperContext.sync(() -> sender.sendMessage(ChatColor.RED + "Das hat nicht geklappt: " + e.getMessage()));
                return;
            }
            PaperContext.sync(() -> sender.sendMessage(ChatColor.GREEN + successMessage));
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) return filter(List.of("create", "stop", "restart", "list"), args[0]);
        if (args.length == 2 && !args[0].equalsIgnoreCase("create")) {
            List<String> names = new ArrayList<>();
            for (ListenerAdapter.ServerName serverName : ListenerAdapter.ServerName.servers()) {
                names.add(serverName.toString());
            }
            return filter(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            List<String> templates = new ArrayList<>();
            for (ServerTemplate template : ServerTemplate.values()) templates.add(template.name());
            return filter(templates, args[2]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            List<String> plugins = new ArrayList<>();
            for (FileType.PLUGIN plugin : FileType.PLUGIN.values()) plugins.add(plugin.name());
            return filter(plugins, args[4]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) matches.add(option);
        }
        return matches;
    }
}
