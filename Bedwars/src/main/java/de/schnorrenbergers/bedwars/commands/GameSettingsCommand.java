package de.schnorrenbergers.bedwars.commands;

import de.schnorrenbergers.bedwars.Bedwars;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GameSettingsCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You're not op.");
            return false;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return false;
        }
        switch(args[0].toLowerCase()) {
            case "teamsize": {
                if (args.length < 2) {
                    try {
                        int i = Integer.parseInt(args[1]);
                        Bedwars.getInstance().getConfigurationManager().getConfig().set("teamsize", i);
                        sender.sendMessage(ChatColor.GREEN + "Team size set to " + i);
                        return true;
                    } catch (NumberFormatException e) {
                        sendUsage(sender);
                        return false;
                    }
                } else {
                    sendUsage(sender);
                }
            }
        }
        return false;
    }

    public void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Usage: \n/gamesettings teamsize");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return List.of("teamSize");
        }
        switch (args[0].toLowerCase()) {
            case "teamsize": {
                return List.of("1", "2", "3", "4");
            }
        }
        return List.of();
    }
}
