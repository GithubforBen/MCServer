package de.schnorrenbergers.survival.commands;

import de.hems.paper.admin.AdminStash;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * {@code /admin} - the admin stash and the money tools.
 * <p>
 * Called on its own it opens the stash: the chest the web interface drops items into when an admin pulls
 * something out of a player's inventory there.
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "❌ Dieser Befehl ist nur für Admins.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("stash") || args[0].equalsIgnoreCase("ablage")) {
            openStash(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("money")) {
            money(sender, args);
            return true;
        }
        sendUsage(sender);
        return true;
    }

    /**
     * Opens the shared admin stash.
     *
     * @param sender who asked
     */
    private void openStash(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Die Ablage kann nur ein Spieler öffnen.");
            return;
        }
        AdminStash stash = AdminStash.getInstance();
        if (stash == null) {
            player.sendMessage(ChatColor.RED + "❌ Die Ablage ist auf diesem Server nicht eingerichtet.");
            return;
        }
        stash.open(player);
    }

    /**
     * Adds, removes or reads a player's money.
     *
     * @param sender who asked
     * @param args   the whole command, starting at {@code money}
     */
    private void money(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsage(sender);
            return;
        }
        String operation = args[1].toLowerCase();
        String targetName = args[2];

        UUID uuid;
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            uuid = online.getUniqueId();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (offline.getUniqueId() == null) {
                sender.sendMessage(ChatColor.RED + "❌ Spieler nicht gefunden.");
                return;
            }
            uuid = offline.getUniqueId();
        }

        if (operation.equals("query")) {
            sender.sendMessage(MoneyHandler.getMoney(uuid) + "$");
            return;
        }
        if (args.length < 4) {
            sendUsage(sender);
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "❌ Der Betrag muss eine Zahl sein.");
            return;
        }
        switch (operation) {
            case "add" -> {
                MoneyHandler.addMoney(amount, uuid);
                sender.sendMessage(ChatColor.GREEN + "✓ " + amount + " zu " + targetName + " hinzugefügt.");
            }
            case "remove" -> {
                MoneyHandler.removeMoney(amount, uuid);
                sender.sendMessage(ChatColor.GREEN + "✓ " + amount + " von " + targetName + " abgezogen.");
            }
            default -> sendUsage(sender);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/admin" + ChatColor.GRAY + " öffnet die Admin-Ablage");
        sender.sendMessage(ChatColor.GRAY + "/admin money add|remove <spieler> <betrag>");
        sender.sendMessage(ChatColor.GRAY + "/admin money query <spieler>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) return List.of();
        if (args.length <= 1) return List.of("stash", "money");
        if (args.length == 2 && args[0].equalsIgnoreCase("money")) {
            return List.of("add", "remove", "query");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("money")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
