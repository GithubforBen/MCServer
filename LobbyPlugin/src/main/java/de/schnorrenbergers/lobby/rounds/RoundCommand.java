package de.schnorrenbergers.lobby.rounds;

import de.hems.paper.round.RoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * The way into the round list from the chat. Everything it can do, the menu can do too - this is the door,
 * not a second interface.
 */
public class RoundCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Das kann nur ein Spieler.", NamedTextColor.RED));
            return true;
        }
        String action = args.length == 0 ? "liste" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "start" -> RoundCreateUi.open(player);
            case "admin", "einstellungen" -> RoundPolicyUi.open(player);
            case "aktualisieren", "refresh" -> {
                RoundService.refreshAsync();
                player.sendMessage(Component.text("Die Rundenliste wird neu geladen.", NamedTextColor.GRAY));
            }
            default -> RoundBrowserUi.open(player);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length > 1) return List.of();
        List<String> options = new ArrayList<>(List.of("liste", "start", "aktualisieren"));
        if (sender.isOp()) options.add("admin");
        String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(typed)).toList();
    }
}
