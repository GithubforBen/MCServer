package de.schnorrenbergers.lobby.rounds;

import de.hems.paper.round.RoundService;
import de.hems.types.round.RoundData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
 * The way into the round list from the chat. Everything it can do, the menu can do too - this is the door,
 * not a second interface.
 */
public class RoundCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Das kann nur ein Spieler.", NamedTextColor.RED));
            return true;
        }
        String action = args.length == 0 ? "liste" : args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "start" -> RoundCreateUi.open(player);
            case "einladen", "invite" -> invite(player, args);
            case "admin", "einstellungen" -> RoundPolicyUi.open(player);
            case "aktualisieren", "refresh" -> {
                RoundService.refreshAsync();
                player.sendMessage(Component.text("Die Rundenliste wird neu geladen.", NamedTextColor.GRAY));
            }
            default -> RoundBrowserUi.open(player);
        }
        return true;
    }

    /**
     * Lets somebody into a round that is closed.
     * <p>
     * Only the owner can, because it is their round, and only into one of theirs. An admin is not given a
     * shortcut here on purpose: they can see and join every round anyway, so an admin inviting somebody
     * into another player's round would only be a way to fill it against its owner's wishes.
     *
     * @param player who is inviting
     * @param args   the command as it was typed
     */
    private void invite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("/runde einladen <spieler>", NamedTextColor.GRAY));
            return;
        }
        RoundData round = null;
        for (RoundData candidate : RoundService.getRounds()) {
            if (candidate.isOwner(player.getUniqueId()) && candidate.getState().isAlive()) {
                round = candidate;
                break;
            }
        }
        if (round == null) {
            player.sendMessage(Component.text("Du hast gerade keine eigene Runde offen.", NamedTextColor.RED));
            return;
        }
        Player guest = Bukkit.getPlayerExact(args[1]);
        if (guest == null) {
            player.sendMessage(Component.text("'" + args[1] + "' ist gerade nicht hier in der Lobby.",
                    NamedTextColor.RED));
            return;
        }
        RoundData updated = round.copy();
        if (!updated.invite(guest.getUniqueId())) {
            player.sendMessage(Component.text(guest.getName() + " darf schon rein.", NamedTextColor.GRAY));
            return;
        }
        RoundService.saveAsync(updated, stored -> {
            if (!Boolean.TRUE.equals(stored)) {
                player.sendMessage(Component.text("Die Einladung konnte nicht gespeichert werden.",
                        NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text(guest.getName() + " darf jetzt in deine Runde.",
                    NamedTextColor.GREEN));
            if (!guest.isOnline()) return;
            guest.sendMessage(Component.text(player.getName() + " lädt dich in seine Runde ein.",
                            NamedTextColor.AQUA)
                    .append(Component.newline())
                    .append(Component.text("[Runden öffnen]", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/runde"))
                            .hoverEvent(HoverEvent.showText(Component.text("Zeigt die Runden, in die du darfst")))));
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 2 && args[0].toLowerCase(Locale.ROOT).startsWith("ein")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) names.add(online.getName());
            return names.stream().filter(name -> name.toLowerCase(Locale.ROOT)
                    .startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length > 1) return List.of();
        List<String> options = new ArrayList<>(List.of("liste", "start", "einladen", "aktualisieren"));
        if (sender.isOp()) options.add("admin");
        String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(typed)).toList();
    }
}
