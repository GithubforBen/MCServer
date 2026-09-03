package de.hems.paper.commands;

import de.hems.paper.cosmetic.CosmeticAdminUi;
import de.hems.paper.cosmetic.CosmeticService;
import de.hems.paper.cosmetic.CosmeticsUi;
import de.hems.types.cosmetic.CosmeticType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opens the cosmetics shop, wherever the player happens to be standing.
 * <p>
 * The shop used to hang off one button in the marketplace on survival, which meant that somebody who only
 * plays bedwars had to travel to another server to put on something they had already paid for. The menu
 * itself was never survival's - it talks to the launcher and to nothing else - so the command lives here
 * and every plugin that has a network connection can register it.
 */
public class CosmeticsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cosmetics gehören zu einem Spieler.", NamedTextColor.RED));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("verwalten")) {
            CosmeticAdminUi.open(player);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("neu")) {
            // for the one case the automatic loading cannot cover: the connection was down when they
            // joined, so nothing was ever fetched for them
            CosmeticService.refreshAsync();
            CosmeticService.loadPlayerAsync(player.getUniqueId());
            player.sendMessage(Component.text("Cosmetics werden neu geladen.", NamedTextColor.GRAY));
            return true;
        }
        CosmeticType tab = args.length > 0 ? tabOf(args[0]) : null;
        if (tab == null) {
            CosmeticsUi.open(player);
        } else {
            CosmeticsUi.open(player, tab);
        }
        return true;
    }

    /**
     * @param name what was typed
     * @return the page it means, or {@code null} for the one they last had open
     */
    private static @Nullable CosmeticType tabOf(String name) {
        for (CosmeticType type : CosmeticType.values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length != 1) return List.of();
        List<String> options = new ArrayList<>();
        for (CosmeticType type : CosmeticType.values()) options.add(type.name().toLowerCase(Locale.ROOT));
        options.add("neu");
        if (sender.isOp()) options.add("verwalten");
        options.removeIf(option -> !option.startsWith(args[0].toLowerCase(Locale.ROOT)));
        return options;
    }
}
