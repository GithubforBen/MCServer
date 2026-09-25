package de.schnorrenbergers.hungergames;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code /hg}: what an admin needs in the arena.
 * <ul>
 *     <li>{@code status} - where the game stands</li>
 *     <li>{@code start} - start now, whatever the event's clock says; alone it runs as a test</li>
 *     <li>{@code stop} - end without a winner</li>
 *     <li>{@code mitte} - make where you stand the middle of the map, for the next arena too</li>
 * </ul>
 */
public final class HgCommand implements CommandExecutor, TabCompleter {

    private final Game game;

    public HgCommand(Game game) {
        this.game = game;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        if (!sub.equals("status") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Das dürfen nur Admins.");
            return true;
        }
        switch (sub) {
            case "status" -> sender.sendMessage(ChatColor.GOLD + ArenaContext.getTitle() + ": "
                    + ChatColor.WHITE + game.describe());
            case "start" -> sender.sendMessage(ChatColor.GREEN + game.forceStart());
            case "stop" -> sender.sendMessage(ChatColor.YELLOW + game.stop());
            case "mitte" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Nur im Spiel.");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + ArenaMap.saveCenter(player.getLocation()));
                sender.sendMessage(ChatColor.GRAY + "Gilt ab dem nächsten Start der Arena.");
            }
            default -> sender.sendMessage(ChatColor.GRAY + "/hg [status|start|stop|mitte]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        return List.of("status", "start", "stop", "mitte").stream()
                .filter(option -> option.startsWith(args[0].toLowerCase())).toList();
    }
}
