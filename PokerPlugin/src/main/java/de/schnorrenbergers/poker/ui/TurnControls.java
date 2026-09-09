package de.schnorrenbergers.poker.ui;

import de.hems.api.ItemApi;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The buttons somebody plays with, as items in their hand.
 * <p>
 * A menu that opens over the table would be the easy way and the wrong one: the whole point of playing at a
 * table rather than in an inventory is seeing the cards, the chips and the other players while you decide.
 * So the decision lives on the hotbar, where it costs nothing to look at.
 * <p>
 * The buttons say the price on them. "Mitgehen 240" is a decision; "Call" is a thing you press and find out
 * about afterwards, and afterwards is too late when it is real bits.
 */
public final class TurnControls {

    /** The slots the buttons live in, left to right. */
    public static final int SLOT_FOLD = 0;
    public static final int SLOT_CHECK_CALL = 1;
    public static final int SLOT_RAISE = 2;
    public static final int SLOT_LEAVE = 8;

    private TurnControls() {
    }

    /**
     * Puts the buttons for one turn into somebody's hands.
     *
     * @param player who is deciding
     * @param table  the table
     * @param seat   their seat at it
     */
    public static void give(Player player, PokerTable table, PokerPlayer seat) {
        int toCall = table.toCall(seat);
        int minRaise = table.minRaiseTo(seat);
        int maxRaise = table.maxRaiseTo(seat);

        player.getInventory().setItem(SLOT_FOLD, new ItemApi(Material.RED_DYE,
                ChatColor.RED + "Passen",
                List.of(ChatColor.GRAY + "Du steigst aus dieser Hand aus.",
                        toCall > 0 ? ChatColor.DARK_GRAY + "Kostet dich nichts mehr."
                                : ChatColor.YELLOW + "Schieben ist gratis - passen wäre Unsinn.")).build());

        if (toCall > 0) {
            player.getInventory().setItem(SLOT_CHECK_CALL, new ItemApi(Material.LIME_DYE,
                    ChatColor.GREEN + "Mitgehen " + toCall,
                    List.of(ChatColor.GRAY + "Kostet " + toCall + " Chips",
                            ChatColor.GRAY + "Danach hast du " + (seat.getChips() - toCall) + " übrig",
                            toCall >= seat.getChips()
                                    ? ChatColor.RED + "Das ist dein ganzer Stack." : "")).build());
        } else {
            player.getInventory().setItem(SLOT_CHECK_CALL, new ItemApi(Material.LIME_DYE,
                    ChatColor.GREEN + "Schieben",
                    List.of(ChatColor.GRAY + "Kostet nichts, du bleibst dabei.")).build());
        }

        if (minRaise > 0 && minRaise <= maxRaise) {
            player.getInventory().setItem(SLOT_RAISE, new ItemApi(Material.GOLD_INGOT,
                    ChatColor.GOLD + (table.getCurrentBet() > 0 ? "Erhöhen" : "Setzen"),
                    List.of(ChatColor.GRAY + "Mindestens " + minRaise + ", höchstens " + maxRaise,
                            ChatColor.GRAY + "Klicken öffnet die Beträge")).build());
        } else {
            player.getInventory().setItem(SLOT_RAISE, null);
        }
        leaveButton(player);
        player.getInventory().setHeldItemSlot(SLOT_CHECK_CALL);
        player.updateInventory();
    }

    /**
     * The hotbar between turns: nothing to decide, but the way out stays where it was.
     *
     * @param player somebody sitting at a table
     */
    public static void giveIdle(Player player) {
        player.getInventory().setItem(SLOT_FOLD, null);
        player.getInventory().setItem(SLOT_CHECK_CALL, null);
        player.getInventory().setItem(SLOT_RAISE, null);
        leaveButton(player);
        player.updateInventory();
    }

    private static void leaveButton(Player player) {
        player.getInventory().setItem(SLOT_LEAVE, new ItemApi(Material.OAK_DOOR,
                ChatColor.YELLOW + "Aufstehen",
                List.of(ChatColor.GRAY + "Deine Chips werden wieder zu Bits.",
                        ChatColor.DARK_GRAY + "Mitten in einer Hand wartest du,",
                        ChatColor.DARK_GRAY + "bis sie vorbei ist.")).build());
    }

    /**
     * Takes the buttons away, which is what standing up and logging off both do.
     *
     * @param player who is done
     */
    public static void clear(Player player) {
        for (int slot : new int[]{SLOT_FOLD, SLOT_CHECK_CALL, SLOT_RAISE, SLOT_LEAVE}) {
            player.getInventory().setItem(slot, null);
        }
        player.updateInventory();
    }

    /**
     * @param stack what somebody clicked with
     * @return which button it is, or {@code -1} for anything else
     */
    public static int buttonOf(ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) return -1;
        String name = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        if (name == null) return -1;
        if (name.equals("Passen")) return SLOT_FOLD;
        if (name.equals("Schieben") || name.startsWith("Mitgehen")) return SLOT_CHECK_CALL;
        if (name.equals("Erhöhen") || name.equals("Setzen")) return SLOT_RAISE;
        if (name.equals("Aufstehen")) return SLOT_LEAVE;
        return -1;
    }
}
