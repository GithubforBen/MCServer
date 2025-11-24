package de.schnorrenbergers.survival.commands;

import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ShopkeeperCommand implements TabCompleter, org.bukkit.command.CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command");
            return false;
        }
        if (args.length != 1) {
            sender.sendMessage("Usage: /shopkeeper <name>");
            return false;
        }
        Player player = (Player) sender;
        Shopkeeper shopkeeper = ShopkeeperManager.createShopkeeper(player, args[0]);
        if (shopkeeper == null) {
            sender.sendMessage("No Shopkeeper could be created!");
            return false;
        }
        sender.sendMessage("Successfully created a Shopkeeper! " + shopkeeper.getUuid());
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}
