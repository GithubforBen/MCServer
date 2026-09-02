package de.hems.paper.commands;

import de.hems.paper.discord.AccountLinkService;
import de.hems.types.discord.AccountLink;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The in game half of linking a discord account, and the lookup it exists for.
 * <p>
 * Three things: type in the code discord gave you, ask who somebody is, and see who you are yourself. The
 * lookup is what the whole feature is for - somebody has to be told something and the only handle anybody
 * has is a minecraft name.
 */
public class VerifyCommand implements CommandExecutor, TabCompleter {

    private static final SimpleDateFormat WHEN = new SimpleDateFormat("dd.MM.yyyy");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            own(sender);
            return true;
        }
        String first = args[0].toLowerCase(Locale.ROOT);
        switch (first) {
            case "wer", "who" -> who(sender, args);
            case "loesen", "lösen", "unlink" -> sender.sendMessage(Component.text(
                    "Eine Verknüpfung löst der Besitzer im Discord mit /unlink <spieler>.",
                    NamedTextColor.GRAY));
            default -> confirm(sender, args[0]);
        }
        return true;
    }

    /**
     * Shows the sender their own link.
     */
    private void own(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("/verify wer <spieler>", NamedTextColor.GRAY));
            return;
        }
        AccountLink link = AccountLinkService.of(player.getUniqueId());
        if (link == null) {
            player.sendMessage(Component.text("Dein Account ist mit keinem Discord verknüpft.",
                            NamedTextColor.GRAY)
                    .append(Component.newline())
                    .append(Component.text("Schreib im Discord /verify " + player.getName()
                            + " - du bekommst einen Code, den du hier eingibst.", NamedTextColor.YELLOW)));
            return;
        }
        player.sendMessage(Component.text("Du bist mit " + link.describeDiscord() + " verknüpft.",
                NamedTextColor.GREEN));
    }

    /**
     * Hands in a code.
     */
    private void confirm(CommandSender sender, String code) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Einen Code kann nur ein Spieler eingeben.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Code wird geprüft ...", NamedTextColor.GRAY));
        AccountLinkService.confirmAsync(player.getUniqueId(), player.getName(), code, result -> {
            if (!player.isOnline()) return;
            player.sendMessage(Component.text(result.message() == null
                            ? (result.successful() ? "Verknüpft." : "Das hat nicht geklappt.")
                            : result.message(),
                    result.successful() ? NamedTextColor.GREEN : NamedTextColor.RED));
        });
    }

    /**
     * Answers "who is that on discord". For anybody who moderates, which is what the answer is for.
     */
    private void who(CommandSender sender, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("network.verify.lookup")) {
            sender.sendMessage(Component.text("Dafür bist du nicht berechtigt.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("/verify wer <spieler>", NamedTextColor.GRAY));
            return;
        }
        String name = args[1];
        AccountLink link = AccountLinkService.byName(name);
        if (link == null) {
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) link = AccountLinkService.of(online.getUniqueId());
        }
        if (link == null) {
            sender.sendMessage(Component.text(name + " hat keinen Discord-Account verknüpft.",
                    NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text(link.getMinecraftName() + " ist auf Discord "
                + link.describeDiscord() + ".", NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text("Verknüpft seit " + WHEN.format(new Date(link.getLinkedAt())) + ".",
                        NamedTextColor.DARK_GRAY)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length <= 1) {
            List<String> options = new ArrayList<>();
            if (sender.isOp() || sender.hasPermission("network.verify.lookup")) options.add("wer");
            String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(option -> option.startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("wer")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) names.add(online.getName());
            return names.stream().filter(name -> name.toLowerCase(Locale.ROOT)
                    .startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
