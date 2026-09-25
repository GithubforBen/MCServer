package de.hems.paper.restart;

import de.hems.paper.PaperContext;
import de.hems.types.restart.RestartMode;
import de.hems.types.restart.RestartStatus;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code /neustart}: restarts the whole network at a set time.
 * <ul>
 *     <li>{@code /neustart} - what is scheduled, and how the last update went</li>
 *     <li>{@code /neustart <minuten>} - restart on the code that is there</li>
 *     <li>{@code /neustart <minuten> update} - pull the newest code, build it, restart</li>
 *     <li>{@code /neustart <minuten> aus} - shut down and stay off</li>
 *     <li>{@code /neustart abbrechen} - call it off</li>
 * </ul>
 * Not {@code /restart}: that name belongs to Spigot and restarts only the one server.
 */
public final class RestartCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Das dürfen nur Admins.");
            return true;
        }
        if (args.length == 0) {
            describe(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("abbrechen")) {
            send(sender, 0, null);
            return true;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(usage());
            return true;
        }
        RestartMode mode = args.length > 1 ? RestartMode.byWord(args[1]) : RestartMode.RESTART;
        if (mode == null) {
            sender.sendMessage(usage());
            return true;
        }
        send(sender, minutes, mode);
        return true;
    }

    private static void send(CommandSender sender, int minutes, RestartMode mode) {
        PaperContext.async(() -> {
            String error = RestartService.requestBlocking(minutes, mode, sender.getName());
            PaperContext.sync(() -> {
                if (error != null) sender.sendMessage(ChatColor.RED + error);
                else sender.sendMessage(ChatColor.GREEN + "✓ " + (mode == null ? "Abgesagt." : "Geplant."));
            });
        });
    }

    private static void describe(CommandSender sender) {
        RestartStatus status = RestartService.getStatus();
        if (status.isScheduled()) {
            sender.sendMessage(ChatColor.GOLD + status.getMode().getTitle() + " in "
                    + RestartStatus.format(status.getSecondsLeft()) + ChatColor.GRAY + " (von "
                    + status.getRequestedBy() + ")");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Kein Neustart geplant.");
        }
        if (status.getLastUpdate() != null) {
            sender.sendMessage(ChatColor.GRAY + "Letztes Update: " + ChatColor.WHITE + status.getLastUpdate());
        }
        sender.sendMessage(usage());
    }

    private static String usage() {
        return ChatColor.GRAY + "/neustart <Minuten> [update|aus] · /neustart abbrechen";
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) return List.of();
        if (args.length == 1) return List.of("1", "5", "10", "30", "abbrechen");
        if (args.length == 2 && !args[0].equalsIgnoreCase("abbrechen")) return List.of("update", "aus");
        return List.of();
    }
}
