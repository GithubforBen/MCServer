package de.schnorrenbergers.survival.antiEnd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AntiEndCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) return false;
        if (args.length != 1) return false;
        try {
            boolean b = Boolean.parseBoolean(args[0]);
            AntiEndHandler.setAllowEnd(b);
        } catch (NumberFormatException e) {
            if (args[0].equalsIgnoreCase("query")) {
                sender.sendMessage("AllowEnd: " + AntiEndHandler.allowEnd());
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return List.of("true", "false", "query");
    }
}
