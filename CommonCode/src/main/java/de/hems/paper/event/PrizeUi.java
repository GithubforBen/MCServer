package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.util.ChatPrompt;
import de.hems.types.event.AwardData;
import de.hems.types.event.EventData;
import de.hems.types.event.PrizeData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the placings of an event are worth, as buttons.
 * <p>
 * Items are set by holding them: whatever is in the admin's hand becomes the prize. Typing material names
 * would be the other way, and the other way is where the typos live.
 */
public final class PrizeUi {

    /** The money steps a click walks through. */
    private static final int[] MONEY_STEPS = {0, 100, 250, 500, 1000, 2500, 5000};

    private PrizeUi() {
    }

    /**
     * @param player the admin setting the prizes
     * @param event  the event they belong to
     * @return the panel
     */
    public static CustomInventory build(Player player, EventData event) {
        CustomInventory ui = new CustomInventory(9 * 4, ChatColor.GOLD + "Preise", close -> {
        });
        ui.fillPlaceHolder();

        for (int place = 1; place <= PrizeData.PLACES; place++) {
            draw(ui, player, event, 10 + (place - 1) * 2, place, PrizeData.ofPlace(event, place),
                    medal(place), place + ". Platz");
        }
        draw(ui, player, event, 16, AwardData.PARTICIPATION, PrizeData.ofParticipation(event),
                Material.PAPER, "Teilnahme");

        ui.setItem(27, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click ->
                        player.openInventory(UhcEventUi.build(player, event).getInventory())));
        return ui;
    }

    /**
     * One placing, with its money on the left click and its items on the right.
     */
    private static void draw(CustomInventory ui, Player player, EventData event, int slot, int place,
                             PrizeData prize, Material icon, String title) {
        List<String> lore = new ArrayList<>();
        for (String line : prize.describe()) lore.add(ChatColor.GRAY + line);
        lore.add("");
        lore.add(ChatColor.AQUA + "Linksklick: Geld ändern");
        lore.add(ChatColor.AQUA + "Rechtsklick: Item in der Hand hinzufügen");
        lore.add(ChatColor.RED + "Shift-Rechtsklick: Items leeren");

        ui.setItem(slot, new ItemApi(icon, ChatColor.GOLD + title, lore).build(),
                new SimpleItemAction(click -> {
                    PrizeData edited = place == AwardData.PARTICIPATION
                            ? PrizeData.ofParticipation(event) : PrizeData.ofPlace(event, place);
                    if (click.isRightClick() && click.isShiftClick()) {
                        edited.getItems().clear();
                    } else if (click.isRightClick()) {
                        ItemStack hand = player.getInventory().getItemInMainHand();
                        if (hand.getType().isAir()) {
                            player.sendMessage(ChatColor.RED + "Du hast nichts in der Hand.");
                            return;
                        }
                        edited.withItem(hand.getType().name(), hand.getAmount());
                    } else {
                        edited.setMoney(nextMoney(edited.getMoney()));
                    }
                    store(player, event, place, edited);
                }));
    }

    /**
     * Writes one placing back onto the event.
     */
    private static void store(Player player, EventData event, int place, PrizeData prize) {
        EventData edited = event.copy();
        if (place == AwardData.PARTICIPATION) {
            PrizeData.setParticipation(edited, prize);
        } else {
            PrizeData.setPlace(edited, place, prize);
        }
        EventService.saveAsync(edited, false, result -> {
            if (!result.successful()) {
                player.sendMessage(ChatColor.RED + "❌ " + result.message());
                return;
            }
            player.openInventory(build(player, result.event()).getInventory());
        });
    }

    private static int nextMoney(int current) {
        for (int i = 0; i < MONEY_STEPS.length; i++) {
            if (MONEY_STEPS[i] == current) return MONEY_STEPS[(i + 1) % MONEY_STEPS.length];
        }
        return MONEY_STEPS[0];
    }

    private static Material medal(int place) {
        return switch (place) {
            case 1 -> Material.GOLD_INGOT;
            case 2 -> Material.IRON_INGOT;
            default -> Material.COPPER_INGOT;
        };
    }
}
