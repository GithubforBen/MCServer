package de.hems.paper.lotto;

import de.hems.communication.events.lotto.LottoRequestEvent;
import de.hems.types.lotto.LottoDraw;
import de.hems.types.lotto.LottoStatus;
import de.hems.types.lotto.LottoTicket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@code /lotto}: 4 from 15, paid in bits.
 * <ul>
 *     <li>{@code /lotto} - the slip</li>
 *     <li>{@code /lotto tipp <a> <b> <c> <d>} - one tip straight away</li>
 *     <li>{@code /lotto quick [anzahl]} - random tips</li>
 *     <li>{@code /lotto info}, {@code /lotto meine}</li>
 *     <li>admins: {@code /lotto ziehen}, {@code /lotto termin <tag> <HH:mm>}, {@code /lotto preis <bits>},
 *     and whatever the server adds - the lobby adds its stand</li>
 * </ul>
 */
public final class LottoCommand implements CommandExecutor, TabCompleter {

    private static final SecureRandom random = new SecureRandom();

    /** Admin subcommands only one server has, like the lobby's stand. */
    private final Map<String, Consumer<Player>> extras;

    public LottoCommand() {
        this(Map.of());
    }

    public LottoCommand(Map<String, Consumer<Player>> extras) {
        this.extras = extras;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (args.length == 0) {
            LottoMenu.open(player);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "tipp" -> tip(player, args);
            case "quick" -> quick(player, args);
            case "info" -> info(player);
            case "meine" -> LottoMenu.showMine(player);
            case "ziehen", "termin", "preis" -> admin(player, sub, args);
            default -> {
                Consumer<Player> extra = extras.get(sub);
                if (extra != null && LottoClient.isAdmin(player)) extra.accept(player);
                else usage(player);
            }
        }
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(Component.text("/lotto · /lotto tipp <4 Zahlen von 1-15> · /lotto quick [anzahl] · /lotto info · /lotto meine",
                NamedTextColor.GRAY));
        if (LottoClient.isAdmin(player)) {
            player.sendMessage(Component.text("Admin: /lotto ziehen · /lotto termin <tag> <HH:mm> · /lotto preis <bits>"
                    + (extras.isEmpty() ? "" : " · /lotto " + String.join(" · /lotto ", extras.keySet())), NamedTextColor.GRAY));
        }
    }

    private static void tip(Player player, String[] args) {
        if (args.length != LottoTicket.NUMBERS + 1) {
            player.sendMessage(Component.text("So geht das: /lotto tipp 3 7 11 14", NamedTextColor.RED));
            return;
        }
        int[] numbers = new int[LottoTicket.NUMBERS];
        try {
            for (int i = 0; i < numbers.length; i++) numbers[i] = Integer.parseInt(args[i + 1]);
        } catch (NumberFormatException e) {
            numbers = null;
        }
        if (LottoTicket.normalize(numbers) == null) {
            player.sendMessage(Component.text("Ein Tipp sind " + LottoTicket.NUMBERS + " verschiedene Zahlen von 1 bis "
                    + LottoTicket.HIGHEST + ".", NamedTextColor.RED));
            return;
        }
        buy(player, List.of(numbers));
    }

    private static void quick(Player player, String[] args) {
        int count = 1;
        if (args.length > 1) {
            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                count = -1;
            }
        }
        if (count < 1 || count > 20) {
            player.sendMessage(Component.text("Zwischen 1 und 20 Quicktipps auf einmal.", NamedTextColor.RED));
            return;
        }
        List<int[]> tips = new ArrayList<>();
        for (int i = 0; i < count; i++) tips.add(LottoTicket.random(random));
        buy(player, tips);
    }

    private static void buy(Player player, List<int[]> tips) {
        LottoClient.ask(LottoClient.request(player, LottoRequestEvent.Action.BUY).withTips(tips), answer -> {
            if (answer.error() != null) {
                player.sendMessage(Component.text(answer.error(), NamedTextColor.RED));
                return;
            }
            for (int[] tip : tips) {
                player.sendMessage(Component.text("✓ Getippt: ", NamedTextColor.GREEN)
                        .append(Component.text(LottoTicket.format(LottoTicket.normalize(tip)), NamedTextColor.YELLOW)));
            }
            player.sendMessage(Component.text("Du hast " + answer.tickets().size() + " Tipp(s) in dieser Runde.",
                    NamedTextColor.GRAY));
        });
    }

    private static void info(Player player) {
        LottoStatus status = LottoClient.getStatus();
        if (!status.isKnown()) {
            player.sendMessage(Component.text("Das Lotto ist noch nicht geladen.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text("— Lotto · 4 aus 15 · Runde " + status.getRound() + " —", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Topf: " + status.getPot() + " Bits · " + status.getTickets()
                + " Tipps · ein Tipp kostet " + status.getPrice() + " Bits", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Ziehung in " + LottoStatus.span(status.getNextDrawAt() - System.currentTimeMillis())
                + " (" + status.getSchedule().toLowerCase() + ")", NamedTextColor.GRAY));
        LottoDraw last = status.getLastDraw();
        if (last != null) {
            player.sendMessage(Component.text("Letzte Zahlen: " + LottoTicket.format(last.getNumbers())
                    + (last.getWinners().isEmpty() ? " · kein Gewinner"
                    : " · gewonnen: " + String.join(", ", last.getWinners().stream().distinct().toList())), NamedTextColor.GRAY));
        }
    }

    private static void admin(Player player, String sub, String[] args) {
        if (!LottoClient.isAdmin(player)) {
            player.sendMessage(Component.text("Das dürfen nur Admins.", NamedTextColor.RED));
            return;
        }
        LottoRequestEvent request;
        switch (sub) {
            case "ziehen" -> request = LottoClient.request(player, LottoRequestEvent.Action.DRAW_NOW);
            case "termin" -> {
                if (args.length != 3) {
                    player.sendMessage(Component.text("So geht das: /lotto termin sonntag 20:00", NamedTextColor.RED));
                    return;
                }
                request = LottoClient.request(player, LottoRequestEvent.Action.SET_SCHEDULE).withText(args[1] + " " + args[2]);
            }
            default -> {
                int price;
                try {
                    price = args.length == 2 ? Integer.parseInt(args[1]) : -1;
                } catch (NumberFormatException e) {
                    price = -1;
                }
                if (price < 1) {
                    player.sendMessage(Component.text("So geht das: /lotto preis 100", NamedTextColor.RED));
                    return;
                }
                request = LottoClient.request(player, LottoRequestEvent.Action.SET_PRICE).withAmount(price);
            }
        }
        LottoClient.ask(request, answer -> {
            if (answer.error() != null) player.sendMessage(Component.text(answer.error(), NamedTextColor.RED));
            else if (!sub.equals("ziehen")) info(player);
        });
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        boolean admin = sender instanceof Player player && LottoClient.isAdmin(player);
        if (args.length == 1) {
            options.addAll(List.of("tipp", "quick", "info", "meine"));
            if (admin) {
                options.addAll(List.of("ziehen", "termin", "preis"));
                options.addAll(extras.keySet());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("termin") && admin) {
            options.addAll(List.of("montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag", "sonntag"));
        }
        String typed = args[args.length - 1].toLowerCase();
        options.removeIf(option -> !option.startsWith(typed));
        return options;
    }
}
