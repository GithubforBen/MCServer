package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.poker.PokerStatsService;
import de.hems.types.event.EventData;
import de.hems.types.event.PokerEventSettings;
import de.hems.types.event.PrizeData;
import de.hems.types.poker.PokerStatsData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The ranking of a poker night.
 * <p>
 * It shows two groups, and the second one is the important half. The top is who is winning; underneath it
 * stand the people who have not cleared the bars yet, with what they are still short of. A ranking that
 * silently leaves somebody out looks broken to the person left out, and they are the one most likely to be
 * looking at it.
 */
public final class PokerRankingUi {

    private PokerRankingUi() {
    }

    /**
     * @param player who is looking
     * @param event  the poker night
     * @return the panel
     */
    public static CustomInventory build(Player player, EventData event) {
        PokerEventSettings settings = new PokerEventSettings(event);
        CustomInventory ui = new CustomInventory(9 * 6,
                ChatColor.GOLD + "Rangliste: " + strip(event.getName()), close -> {
        });
        ui.fillPlaceHolder();

        List<PokerStatsData> everybody = PokerStatsService.getEveryone(event);
        List<PokerStatsData> ranked = new ArrayList<>();
        List<PokerStatsData> short_ = new ArrayList<>();
        for (PokerStatsData row : everybody) {
            if (row.qualifies(settings.getMinHands(), settings.getMinVolume())) {
                ranked.add(row);
            } else {
                short_.add(row);
            }
        }

        ui.setItem(4, header(event, settings, ranked.size(), short_.size()), SimpleItemAction.display());

        // the ranking itself, best first, over the two rows that are the board
        for (int i = 0; i < ranked.size() && i < 18; i++) {
            ui.setItem(9 + i, row(ranked.get(i), i + 1, player, settings), SimpleItemAction.display());
        }

        if (!short_.isEmpty()) {
            ui.setItem(28, new ItemApi(Material.PAPER, ChatColor.YELLOW + "Noch nicht in der Wertung",
                    List.of(ChatColor.GRAY + "Gewertet wird ab " + ChatColor.WHITE
                                    + settings.getMinHands() + ChatColor.GRAY + " Händen"
                                    + (settings.getMinVolume() > 0
                                    ? " und " + ChatColor.WHITE + settings.getMinVolume()
                                    + ChatColor.GRAY + " Bits Einsatz" : ""),
                            ChatColor.DARK_GRAY + "Ein großer Pot ist ein echter Gewinn und",
                            ChatColor.DARK_GRAY + "trotzdem noch keine Pokernacht")).build(),
                    SimpleItemAction.display());
            for (int i = 0; i < short_.size() && i < 17; i++) {
                ui.setItem(29 + i, row(short_.get(i), 0, player, settings), SimpleItemAction.display());
            }
        }

        ui.setItem(45, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click ->
                        player.openInventory(EventDetailUi.build(player, event).getInventory())));

        ui.setItem(49, prizes(event), SimpleItemAction.display());
        return ui;
    }

    /**
     * @return the item at the top that explains what is being ranked
     */
    private static ItemStack header(EventData event, PokerEventSettings settings, int ranked, int waiting) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Gewertet wird: " + ChatColor.WHITE + "was du unterm Strich gewonnen hast");
        lore.add(ChatColor.DARK_GRAY + "Alles was vom Tisch kam, minus alles was drauf ging.");
        lore.add(ChatColor.DARK_GRAY + "Chips, die noch vor dir liegen, zählen mit.");
        lore.add("");
        lore.add(ChatColor.GRAY + "In der Wertung: " + ChatColor.WHITE + ranked);
        lore.add(ChatColor.GRAY + "Noch nicht drin: " + ChatColor.WHITE + waiting);
        lore.add(ChatColor.GRAY + "Buy-in: " + ChatColor.WHITE + settings.getBuyIn() + " Bits");
        lore.add(ChatColor.GRAY + "Haus: " + ChatColor.WHITE + settings.getRakeText());
        if (!PokerStatsService.isLoaded()) {
            lore.add("");
            lore.add(ChatColor.RED + "Die Zahlen sind noch nicht geladen.");
        }
        return new ItemApi(Material.PLAYER_HEAD, ChatColor.GOLD + strip(event.getName()), lore).build();
    }

    /**
     * One line of the board.
     *
     * @param row      the player's numbers
     * @param place    their placing, or {@code 0} for somebody who has not qualified
     * @param viewer   who is looking, so their own line stands out
     * @param settings the knobs, for what is still missing
     * @return the item
     */
    private static ItemStack row(PokerStatsData row, int place, Player viewer, PokerEventSettings settings) {
        List<String> lore = new ArrayList<>();
        int profit = row.getProfit();
        lore.add(ChatColor.GRAY + "Gewonnen: " + profitColor(row) + row.getProfitText() + " Bits");
        lore.add(ChatColor.GRAY + "Eingezahlt: " + ChatColor.WHITE + row.getBoughtIn() + " Bits");
        lore.add(ChatColor.GRAY + "Rausgegangen: " + ChatColor.WHITE + row.getCashedOut() + " Bits");
        if (row.getOpenStack() > 0) {
            lore.add(ChatColor.GRAY + "Noch am Tisch: " + ChatColor.WHITE + row.getOpenStack() + " Bits");
        }
        // the ratio is not what the board is ordered by any more, but it is the other half of the story:
        // the same win off a tenth of the stake was the better evening
        lore.add(ChatColor.GRAY + "Verhältnis: " + ratioColor(row) + row.getRatioText());
        lore.add("");
        lore.add(ChatColor.GRAY + "Hände: " + ChatColor.WHITE + row.getHands()
                + ChatColor.GRAY + ", davon gewonnen: " + ChatColor.WHITE + row.getHandsWon());
        lore.add(ChatColor.GRAY + "Größter Pot: " + ChatColor.WHITE + row.getBiggestPot() + " Bits");

        if (place == 0) {
            int handsShort = Math.max(0, settings.getMinHands() - row.getHands());
            int volumeShort = Math.max(0, settings.getMinVolume() - row.getBoughtIn());
            lore.add("");
            if (handsShort > 0) lore.add(ChatColor.YELLOW + "Noch " + handsShort + " Hände");
            if (volumeShort > 0) lore.add(ChatColor.YELLOW + "Noch " + volumeShort + " Bits Einsatz");
            if (handsShort == 0 && volumeShort == 0) {
                lore.add(ChatColor.YELLOW + "Zählt ab der nächsten Hand");
            }
        }

        String title = (place > 0 ? placeColor(place) + "#" + place + " " : ChatColor.GRAY + "")
                + ChatColor.WHITE + row.getPlayerName();
        if (viewer.getUniqueId().equals(row.getPlayerId())) {
            title = title + ChatColor.GRAY + " (du)";
        }

        ItemStack head = new ItemApi(Material.PLAYER_HEAD, title, lore).build();
        // the face belongs on the line: a board of identical steve heads is a list of names with extra steps
        if (head.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(row.getPlayerId()));
            head.setItemMeta(skull);
        }
        return head;
    }

    /**
     * @return what the places pay out, so people know what they are playing for
     */
    private static ItemStack prizes(EventData event) {
        List<String> lore = new ArrayList<>();
        boolean any = false;
        for (int place = 1; place <= PrizeData.PLACES; place++) {
            PrizeData prize = PrizeData.ofPlace(event, place);
            if (prize.isEmpty()) continue;
            any = true;
            lore.add(placeColor(place) + "#" + place + ChatColor.GRAY + ": "
                    + ChatColor.WHITE + String.join(", ", prize.describe()));
        }
        PrizeData participation = PrizeData.ofParticipation(event);
        if (!participation.isEmpty()) {
            any = true;
            lore.add(ChatColor.GRAY + "Teilnahme: " + ChatColor.WHITE
                    + String.join(", ", participation.describe()));
        }
        if (!any) {
            lore.add(ChatColor.GRAY + "Für diese Nacht sind keine Preise hinterlegt.");
            lore.add(ChatColor.DARK_GRAY + "Gespielt wird trotzdem um echte Bits -");
            lore.add(ChatColor.DARK_GRAY + "die liegen auf dem Tisch.");
        }
        return new ItemApi(Material.CHEST, ChatColor.GOLD + "Preise", lore).build();
    }

    private static ChatColor profitColor(PokerStatsData row) {
        int profit = row.getProfit();
        if (profit > 0) return ChatColor.GREEN;
        if (profit < 0) return ChatColor.RED;
        return ChatColor.WHITE;
    }

    private static ChatColor ratioColor(PokerStatsData row) {
        double ratio = row.getRatio();
        if (ratio > 1.0d) return ChatColor.GREEN;
        if (ratio < 1.0d) return ChatColor.RED;
        return ChatColor.WHITE;
    }

    private static ChatColor placeColor(int place) {
        return switch (place) {
            case 1 -> ChatColor.GOLD;
            case 2 -> ChatColor.GRAY;
            case 3 -> ChatColor.DARK_RED;
            default -> ChatColor.WHITE;
        };
    }

    /**
     * @param name an event name
     * @return it short enough for an inventory title, which is limited and cuts silently
     */
    private static String strip(String name) {
        if (name == null) return "Pokernacht";
        return name.length() <= 20 ? name : name.substring(0, 20);
    }
}
