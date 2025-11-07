package de.schnorrenbergers.survival.commands;

import de.hems.FileType;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.schnorrenbergers.survival.featrues.animations.ParticleLine;
import de.schnorrenbergers.survival.utils.customInventory.types.Inventorys;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
                new ParticleLine(player.getLocation(), new Location(player.getWorld(), 0, 100, 0), Particle.HAPPY_VILLAGER, 0.1).drawParticleLine();
                break;
            }
            case "inv": {
                Player player = (Player) sender;
                Inventory inv = null;
                switch (args[1].toLowerCase()) {
                    case "atm":
                        try {
                            player.openInventory(Inventorys.ATM_INVENTORY().getInventory());
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    default:
                        sender.sendMessage("gibt es nicht du idiot!");
                        break;
                }
                break;
            }
            case "start": {
                try {
                    ListenerAdapter.sendListeners(new RequestServerStartEvent(
                            ListenerAdapter.ServerName.SURVIVAL,
                            ListenerAdapter.ServerName.HOST,
                            UUID.randomUUID(),
                            "debug",
                            FileType.SERVER.PAPER,
                            4000,
                            2000,
                            new FileType.PLUGIN[]{FileType.PLUGIN.WORLDEDIT}
                    ));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(args.length == 1) {
            String parentCommand = args[0].toLowerCase();
            if (parentCommand.equals("inv ")) {
                return List.of("atm");
            }
        }

        return List.of("animation", "inv", "start");
    }
}
