package de.schnorrenbergers.survival.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class BanCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if (!commandSender.isOp()) {
            sendUsage(commandSender);
            return false;
        }
        if (args.length != 4) {
            sendUsage(commandSender);
            return false;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
        if (offlinePlayer == null) {
            commandSender.sendMessage("Player not found!");
            return false;
        }
        BanReason banReason = BanReason.valueOf(args[1]);
        int time = Integer.parseInt(args[2]);
        String timeFrame = args[3];
        Duration duration;
        switch (timeFrame) {
            case "m":
                duration = Duration.ofMinutes(time);
                break;
            case "h":
                duration = Duration.ofHours(time);
                break;
            case "d":
                duration = Duration.ofDays(time);
                break;
            case "w":
                duration = Duration.ofDays(time * 7L);
                break;
            default:
                sendUsage(commandSender);
                return false;
        }
        offlinePlayer.ban("You have been banned: " + banReason.toString(), duration, "ADMIN");
        return false;
    }

    public void sendUsage(CommandSender sender) {
        StringBuilder options = new StringBuilder();
        options.append("<");
        Arrays.stream(BanReason.values()).forEach(banReason -> options.append(banReason.name()).append("|"));
        options.deleteCharAt(options.length() - 1);
        options.append(">");
        sender.sendMessage("/banane <player> <" + options + "> time <m|h|d|w>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        switch (args.length) {
            case 1:
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            case 2:
                return Arrays.stream(BanReason.values()).map(Enum::name).toList();
            case 3:
                return List.of("1", "2", "3", "4", "5");
            case 4:
                return List.of("m", "h", "d", "w");
        }
        return List.of("---------------------------");
    }

    enum BanReason {
        HACKING,
        GRIEFING,
        OTHER;
    }
}
