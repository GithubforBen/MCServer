package de.hems.paper.commands;

import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.customInventory.types.InventoryBase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ServerManagerCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!CustomInventoryListener.hasBeenRegistered()) {
            sender.sendMessage("CustomInventory has not been registered yet!");
            return false;
        }
        if (!sender.isOp()) {
            sender.sendMessage("You are not allowed to use this command!");
            return false;
        }
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("You are not a player!");
            return false;
        }
        try {
            ((Player) sender).openInventory(InventoryBase.SERVERINVENTORY().getInventory());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}
