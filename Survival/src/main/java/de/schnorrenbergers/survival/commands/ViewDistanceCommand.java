package de.schnorrenbergers.survival.commands;

import de.schnorrenbergers.survival.featrues.chunklimiter.ChunkLimiter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * {@code /sichtweite [<chunks>|aus]}: a limit of a player's own for their view distance.
 * <p>
 * The chunk limiter moves the distance up and down with the lag. Whoever would rather have a steady picture
 * sets a limit at or below what they get on a bad evening, and from then on nothing changes for them at all.
 */
public class ViewDistanceCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUGGESTIONS = List.of("aus", "4", "6", "8", "10", "12");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler haben eine Sichtweite.");
            return true;
        }
        ChunkLimiter limiter = ChunkLimiter.getInstance();
        if (limiter == null) {
            player.sendMessage(ChatColor.RED + "Die Sichtweite lässt sich gerade nicht einstellen.");
            return true;
        }
        if (args.length == 0) {
            status(player, limiter);
            return true;
        }
        String arg = args[0].toLowerCase();
        if (arg.equals("aus") || arg.equals("off") || arg.equals("reset")) {
            limiter.setPersonalLimit(player, null);
            player.sendMessage(ChatColor.GREEN + "✓ Dein Limit ist weg. Du bekommst wieder, so viel der Server hergibt"
                    + ChatColor.GRAY + " (gerade " + player.getViewDistance() + " Chunks).");
            return true;
        }
        int limit;
        try {
            limit = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Benutzung: /sichtweite [<" + ChunkLimiter.MIN_DISTANCE + "-"
                    + ChunkLimiter.MAX_DISTANCE + ">|aus]");
            return true;
        }
        if (limit < ChunkLimiter.MIN_DISTANCE || limit > ChunkLimiter.MAX_DISTANCE) {
            player.sendMessage(ChatColor.RED + "Das Limit muss zwischen " + ChunkLimiter.MIN_DISTANCE + " und "
                    + ChunkLimiter.MAX_DISTANCE + " Chunks liegen.");
            return true;
        }
        limiter.setPersonalLimit(player, limit);
        player.sendMessage(ChatColor.GREEN + "✓ Deine Sichtweite geht ab jetzt nie über " + limit + " Chunks"
                + ChatColor.GRAY + " (gerade " + player.getViewDistance() + ").");
        int automatic = limiter.automaticViewDistance(player);
        if (limit >= automatic) {
            player.sendMessage(ChatColor.GRAY + "Der Server gibt dir gerade nur " + automatic
                    + " Chunks. Solange er laggt, ändert dein Limit also nichts.");
        }
        return true;
    }

    /**
     * Tells a player what they have, what they chose, and how to change it.
     */
    private void status(Player player, ChunkLimiter limiter) {
        Integer limit = ChunkLimiter.getPersonalLimit(player);
        player.sendMessage(ChatColor.AQUA + "Deine Sichtweite: " + ChatColor.WHITE + player.getViewDistance()
                + " Chunks");
        player.sendMessage(ChatColor.GRAY + "Eigenes Limit: " + ChatColor.WHITE
                + (limit == null ? "keins" : limit + " Chunks"));
        player.sendMessage(ChatColor.GRAY + "Ohne Limit gäbe dir der Server gerade "
                + limiter.automaticViewDistance(player) + " Chunks.");
        player.sendMessage(ChatColor.GRAY + "/sichtweite <" + ChunkLimiter.MIN_DISTANCE + "-"
                + ChunkLimiter.MAX_DISTANCE + "> setzt ein Limit, damit sich deine Sichtweite bei Lag nicht "
                + "dauernd ändert. /sichtweite aus nimmt es wieder weg.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) return List.of();
        String typed = args[0].toLowerCase();
        return SUGGESTIONS.stream().filter(option -> option.startsWith(typed)).toList();
    }
}
