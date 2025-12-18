package de.schnorrenbergers.survival.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class BanCommand implements CommandExecutor, TabCompleter {
    enum BanReason {
        HACKING,
        GRIEFING,
        OTHER;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (args.length != 4) {
            sendUsage(commandSender);
            return false;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
        if (offlinePlayer == null) {
            commandSender.sendMessage("Player not found!");
            return false;
        }
        offlinePlayer.ban("You have been banned", Duration.ZERO, "");
        return false;//TODO:
    }

    public void sendUsage(CommandSender sender) {
        sender.sendMessage("/banane <player> <"+ "OTHER" +"> time <m|h|d|w>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        return List.of();
    }
}
