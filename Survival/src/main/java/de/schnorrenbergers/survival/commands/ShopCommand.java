package de.schnorrenbergers.survival.commands;

import de.schnorrenbergers.survival.featrues.Shopkeeper.Shopkeeper;
import de.schnorrenbergers.survival.featrues.Shopkeeper.ShopkeeperManager;
import de.schnorrenbergers.survival.featrues.Shopkeeper.market.MarketplaceUi;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * {@code /shop} opens the marketplace, {@code /shop create <name>} puts a shopkeeper down.
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Diesen Befehl kann nur ein Spieler benutzen.");
            return true;
        }
        if (args.length == 0) {
            MarketplaceUi.open(player);
            return true;
        }
        if (!args[0].equalsIgnoreCase("create")) {
            player.sendMessage("Benutzung: /shop  oder  /shop create <name>");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("Benutzung: /shop create <name>");
            return true;
        }
        String name = String.join(" ", List.of(args).subList(1, args.length));
        Shopkeeper shopkeeper = ShopkeeperManager.createShopkeeper(player, name);
        if (shopkeeper == null) return true;
        player.sendMessage("Shop \"" + name + "\" wurde erstellt.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        return args.length == 1 ? List.of("create") : List.of();
    }
}
