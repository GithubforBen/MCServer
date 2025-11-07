package de.schnorrenbergers.survival.commands;

import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AdminCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0 || !sender.isOp()) {
            sender.sendMessage(NamedTextColor.RED + "You are not allowed to use this command");
            return false;
        }
        switch (args[0]) {
            case "money": {
                if (args.length < 2) {
                    sendUsage(sender);
                    return false;
                }
                UUID uuid;
                Player player = Bukkit.getPlayer(args[2]);
                if (player == null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
                    if (offlinePlayer == null) {
                        sender.sendMessage(NamedTextColor.RED + "Player not found");
                    } else {
                        uuid = offlinePlayer.getUniqueId();
                    }
                    return false;
                } else {
                    uuid = player.getUniqueId();
                }
                int amount = 0;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    if (!args[1].toLowerCase().equals("query")) {
                        sender.sendMessage(NamedTextColor.RED + "Amount must be a number");
                        return false;
                    }
                }
                switch (args[1].toLowerCase()) {
                    case "add": {
                        sender.sendMessage(NamedTextColor.GREEN + "Added " + amount + " to " + args[2]);
                        MoneyHandler.addMoney(amount, uuid);
                        break;
                    }
                    case "remove": {
                        sender.sendMessage(NamedTextColor.GREEN + "Removed " + amount + " from " + args[2]);
                        MoneyHandler.removeMoney(amount, uuid);
                        break;
                    }
                    case "query": {
                        sender.sendMessage(MoneyHandler.getMoney(uuid) + "$");
                        break;
                    }
                    default: {
                        sendUsage(sender);
                        return false;
                    }
                }
            }
            default: {
                sendUsage(sender);
                return false;
            }
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("/admin money add <player> <amount>\n" +
                "/admin money remove <player> <amount>\n" +
                "/admin money query <player>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            return List.of("money");
        }
        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "money":
                    return List.of("add", "remove", "query");
                default:
                    return List.of();
            }
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "money":
                    return Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList();
                default:
                    return List.of();
            }
        }
        return List.of();
    }
}
