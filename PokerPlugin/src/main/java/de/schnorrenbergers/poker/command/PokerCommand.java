package de.schnorrenbergers.poker.command;

import de.schnorrenbergers.poker.Casino;
import de.schnorrenbergers.poker.CasinoContext;
import de.schnorrenbergers.poker.CasinoTable;
import de.schnorrenbergers.poker.bank.Bank;
import de.schnorrenbergers.poker.bot.BotShop;
import de.schnorrenbergers.poker.world.CasinoBuilder;
import de.schnorrenbergers.poker.world.CasinoLayout;
import de.schnorrenbergers.poker.world.CasinoWorld;
import de.schnorrenbergers.poker.world.TableSpot;
import de.hems.types.event.PokerEventSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Everything about a poker night that is worth a command.
 * <p>
 * Sitting down is not one of them - that is done by walking up to a chair and right-clicking it, because a
 * table is a place and not a menu. What is here is the rest: the stakes, buying more chips, standing up,
 * the bots, and the two things an admin needs after they have rebuilt the room by hand.
 */
public final class PokerCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;

    public PokerCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (args.length == 0) {
            info(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info", "regeln" -> info(player);
            case "nachkaufen", "rebuy" -> Casino.rebuy(player);
            case "aufstehen", "leave" -> Casino.leave(player);
            case "bot" -> bot(player, args);
            case "setup" -> setup(player, args);
            case "karte", "map" -> map(player, args);
            default -> help(player);
        }
        return true;
    }

    /**
     * What this evening costs and what it pays, which is the one thing everybody wants before they sit.
     */
    private void info(Player player) {
        PokerEventSettings settings = Casino.getSettings();
        player.sendMessage(Component.text("── " + CasinoContext.getTitle() + " ──", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Format: ", NamedTextColor.GRAY)
                .append(Component.text(settings.getFormat().getTitle(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Buy-in: ", NamedTextColor.GRAY)
                .append(Component.text(settings.getBuyIn() + " Bits", NamedTextColor.WHITE))
                .append(Component.text(" - so viele Chips bekommst du dafür", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("Blinds: ", NamedTextColor.GRAY)
                .append(Component.text(settings.getSmallBlind() + "/" + settings.getBigBlind(),
                        NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Haus: ", NamedTextColor.GRAY)
                .append(Component.text(settings.getRakeText() + " pro Pot", NamedTextColor.WHITE))
                .append(Component.text(settings.getRakeCapBigBlinds() > 0
                        ? ", höchstens " + (settings.getRakeCapBigBlinds() * settings.getBigBlind())
                        + " Bits" : "", NamedTextColor.DARK_GRAY)));
        player.sendMessage(Component.text("Nur aus Pots, um die wirklich gespielt wurde.",
                NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("Gewertet wird: ", NamedTextColor.GRAY)
                .append(Component.text("was rausgeht geteilt durch was reingeht", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Dafür brauchst du " + settings.getMinHands()
                + " Hände und " + settings.getMinVolume() + " Bits Einsatz.", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("Auf deinem Konto: ", NamedTextColor.GRAY)
                .append(Component.text(Bank.balanceOf(player) + " Bits", NamedTextColor.GREEN)));
        int onTable = Casino.stackOf(player.getUniqueId());
        if (onTable > 0) {
            player.sendMessage(Component.text("Am Tisch liegen: ", NamedTextColor.GRAY)
                    .append(Component.text(onTable + " Chips", NamedTextColor.GOLD)));
        }
        player.sendMessage(Component.text("Setz dich mit Rechtsklick auf einen Stuhl.",
                NamedTextColor.AQUA));
    }

    private void bot(Player player, String[] args) {
        CasinoTable table = Casino.tableOf(player);
        if (table == null) table = Casino.tableNear(player.getLocation());
        if (table == null) {
            player.sendMessage(Component.text("Stell dich an einen Tisch.", NamedTextColor.RED));
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("weg")) {
            BotShop.removeAll(player, table);
            return;
        }
        BotShop.spawn(player, table);
    }

    /**
     * Tells the plugin where a table is after somebody has moved it by hand.
     */
    private void setup(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(Component.text("Dafür brauchst du Operator.", NamedTextColor.RED));
            return;
        }
        CasinoLayout layout = Casino.getLayout();
        if (args.length < 2) {
            player.sendMessage(Component.text("/poker setup tisch <nummer> - setzt den Tisch dorthin, "
                    + "wo du stehst", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("/poker setup spawn - setzt den Eingang hierher",
                    NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Danach /poker karte speichern, sonst ist es beim "
                    + "nächsten Abend wieder weg.", NamedTextColor.GRAY));
            return;
        }
        if (args[1].equalsIgnoreCase("spawn")) {
            layout.setSpawn(player.getLocation());
            layout.save();
            player.sendMessage(Component.text("Der Eingang ist jetzt hier.", NamedTextColor.GREEN));
            return;
        }
        if (!args[1].equalsIgnoreCase("tisch") || args.length < 3) {
            player.sendMessage(Component.text("/poker setup tisch <nummer>", NamedTextColor.RED));
            return;
        }
        int number;
        try {
            number = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Das ist keine Zahl.", NamedTextColor.RED));
            return;
        }
        List<TableSpot> spots = new ArrayList<>(layout.getTables());
        if (number < 0 || number >= spots.size()) {
            player.sendMessage(Component.text("Es gibt nur " + spots.size() + " Tische.",
                    NamedTextColor.RED));
            return;
        }
        TableSpot old = spots.get(number);
        // where the player stands is the middle of the table, and the felt is one block above the floor
        TableSpot moved = new TableSpot(old.getIndex(), player.getLocation().getX(),
                player.getLocation().getY() + 1, player.getLocation().getZ(), old.getSeatRadius());
        moved.layOutSeats(old.getSeatCount());
        spots.set(number, moved);
        layout.setTables(spots);
        layout.save();
        player.sendMessage(Component.text("Tisch " + (number + 1) + " steht jetzt hier. "
                + "Ein Neustart des Servers zeichnet ihn neu.", NamedTextColor.GREEN));
    }

    /**
     * The one command that makes building on the casino worth anything.
     */
    private void map(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage(Component.text("Dafür brauchst du Operator.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2 || !args[1].toLowerCase(Locale.ROOT).startsWith("speich")) {
            player.sendMessage(Component.text("/poker karte speichern - legt die Welt so, wie sie "
                    + "jetzt ist, neben den Launcher.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Die nächste Pokernacht spielt dann darin. Ohne das "
                    + "geht der Umbau mit diesem Server verloren.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text(CasinoWorld.export(plugin), NamedTextColor.GREEN));
    }

    private void help(Player player) {
        player.sendMessage(Component.text("/poker", NamedTextColor.GOLD)
                .append(Component.text(" - Einsätze, Regeln, dein Konto", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/poker nachkaufen", NamedTextColor.GOLD)
                .append(Component.text(" - noch ein Buy-in", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/poker aufstehen", NamedTextColor.GOLD)
                .append(Component.text(" - Chips wieder zu Bits", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/poker bot", NamedTextColor.GOLD)
                .append(Component.text(" - einen Bot setzen (" + BotShop.costOf() + " Bits)",
                        NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/poker bot weg", NamedTextColor.GOLD)
                .append(Component.text(" - eigene Bots abräumen", NamedTextColor.GRAY)));
        if (player.isOp()) {
            player.sendMessage(Component.text("/poker setup", NamedTextColor.GOLD)
                    .append(Component.text(" - Tische und Eingang neu festlegen", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/poker karte speichern", NamedTextColor.GOLD)
                    .append(Component.text(" - die Welt für die nächste Nacht sichern",
                            NamedTextColor.GRAY)));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("info", "nachkaufen", "aufstehen", "bot"));
            if (sender.isOp()) options.addAll(List.of("setup", "karte"));
            return options.stream().filter(o -> o.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("bot")) return List.of("weg");
        if (args.length == 2 && args[0].equalsIgnoreCase("setup")) return List.of("tisch", "spawn");
        if (args.length == 2 && args[0].equalsIgnoreCase("karte")) return List.of("speichern");
        return List.of();
    }
}
