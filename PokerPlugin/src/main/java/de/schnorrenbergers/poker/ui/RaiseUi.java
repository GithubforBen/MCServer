package de.schnorrenbergers.poker.ui;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.util.ChatPrompt;
import de.schnorrenbergers.poker.game.Action;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * How much to raise by.
 * <p>
 * Offered as the sizes people actually use - half the pot, the pot, all of it - rather than as a number to
 * type, because a raise is the one decision at a poker table where a slipped finger costs the stack. Typing
 * an exact amount is still there for anybody who wants it, one click further in.
 */
public final class RaiseUi {

    private RaiseUi() {
    }

    /**
     * @param player who is raising
     * @param table  the table
     * @param seat   their seat
     * @return the panel
     */
    public static CustomInventory build(Player player, PokerTable table, PokerPlayer seat) {
        int minRaise = table.minRaiseTo(seat);
        int maxRaise = table.maxRaiseTo(seat);
        int pot = table.getPot();
        int toCall = table.toCall(seat);

        CustomInventory ui = new CustomInventory(9 * 3,
                ChatColor.GOLD + (table.getCurrentBet() > 0 ? "Erhöhen" : "Setzen"), close -> {
        });
        ui.fillPlaceHolder();

        ui.setItem(4, new ItemApi(Material.PAPER, ChatColor.AQUA + "Im Pot: " + pot,
                List.of(ChatColor.GRAY + "Zu zahlen: " + ChatColor.WHITE + toCall,
                        ChatColor.GRAY + "Dein Stack: " + ChatColor.WHITE + seat.getChips(),
                        ChatColor.GRAY + "Mindestens: " + ChatColor.WHITE + minRaise)).build(),
                SimpleItemAction.display());

        int[] slots = {10, 11, 12, 13, 14};
        List<Option> options = options(pot, toCall, minRaise, maxRaise);
        for (int i = 0; i < options.size() && i < slots.length; i++) {
            Option option = options.get(i);
            ui.setItem(slots[i], new ItemApi(option.icon(), ChatColor.GREEN + option.title(),
                    List.of(ChatColor.GRAY + "Auf " + ChatColor.WHITE + option.amount(),
                            ChatColor.GRAY + "Kostet dich " + ChatColor.WHITE
                                    + (option.amount() - seat.getCommitted()))).build(),
                    new SimpleItemAction(click -> {
                        player.closeInventory();
                        submit(player, table, seat, option.amount());
                    }));
        }

        ui.setItem(16, new ItemApi(Material.WRITABLE_BOOK, ChatColor.YELLOW + "Eigener Betrag",
                List.of(ChatColor.GRAY + "Zwischen " + minRaise + " und " + maxRaise)).build(),
                new SimpleItemAction(click -> {
                    player.closeInventory();
                    ChatPrompt.ask(player, "Auf welchen Betrag willst du erhöhen? ("
                            + minRaise + " bis " + maxRaise + ")", answer -> {
                        int amount;
                        try {
                            amount = Integer.parseInt(answer.trim());
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Das ist keine Zahl.");
                            return;
                        }
                        submit(player, table, seat, Math.max(minRaise, Math.min(maxRaise, amount)));
                    });
                }));

        ui.setItem(22, new ItemApi(Material.BARRIER, ChatColor.RED + "Doch nicht",
                List.of(ChatColor.GRAY + "Zurück zur Hand")).build(),
                new SimpleItemAction(click -> player.closeInventory()));
        return ui;
    }

    /**
     * Sends the raise, and only accepts it while it is still that player's turn.
     * <p>
     * A menu can be left open for half a minute, and half a minute at a poker table is three decisions by
     * other people. Acting on a stale click would put chips in on a hand that has already moved on.
     */
    private static void submit(Player player, PokerTable table, PokerPlayer seat, int amount) {
        if (table.getActing() != seat) {
            player.sendMessage(ChatColor.RED + "Du bist nicht mehr dran.");
            return;
        }
        Action action = amount >= table.maxRaiseTo(seat)
                ? Action.allIn(0)
                : table.getCurrentBet() > 0 ? Action.raise(amount) : Action.bet(amount);
        if (!table.act(seat, action)) {
            player.sendMessage(ChatColor.RED + "Das geht so nicht.");
        }
    }

    /**
     * The sizes on offer, with anything that does not fit between the minimum and the stack left out.
     */
    private static List<Option> options(int pot, int toCall, int minRaise, int maxRaise) {
        List<Option> options = new ArrayList<>();
        add(options, new Option("Minimum", minRaise, Material.IRON_NUGGET), minRaise, maxRaise);
        add(options, new Option("Halber Pot", toCall + pot / 2, Material.GOLD_NUGGET), minRaise, maxRaise);
        add(options, new Option("Pot", toCall + pot, Material.GOLD_INGOT), minRaise, maxRaise);
        add(options, new Option("Doppelter Pot", toCall + pot * 2, Material.GOLD_BLOCK), minRaise, maxRaise);
        options.add(new Option("All-In", maxRaise, Material.NETHER_STAR));
        return options;
    }

    private static void add(List<Option> options, Option option, int minRaise, int maxRaise) {
        // an option that is below the minimum or above the stack is not an option, and showing it greyed
        // out only invites the click that then does not work
        if (option.amount() < minRaise || option.amount() >= maxRaise) return;
        for (Option existing : options) {
            if (existing.amount() == option.amount()) return;
        }
        options.add(option);
    }

    private record Option(String title, int amount, Material icon) {
    }
}
