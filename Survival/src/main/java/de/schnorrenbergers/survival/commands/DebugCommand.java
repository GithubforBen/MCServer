package de.schnorrenbergers.survival.commands;

import de.schnorrenbergers.survival.featrues.animations.ParticleLine;
import de.schnorrenbergers.survival.utils.customInventory.CustomInventory;
import de.schnorrenbergers.survival.utils.customInventory.types.Inventorys;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DebugCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0 || !sender.isOp()) {
            sender.sendMessage("IDIOT!");
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "animation": {
                Player player = (Player) sender;
                new ParticleLine(player.getLocation(), new Location(player.getWorld(), 0, 100,0), Particle.HAPPY_VILLAGER, 0.1).drawParticleLine();
                break;
            }
            case "inv": {
                Player player = (Player) sender;
                player.openInventory(Inventorys.ADD_MONEY_INVENTORY().getInventory());
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of("animation", "inv");
    }
}
