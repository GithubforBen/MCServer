package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.EventResultData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where everybody finished, for any event that ranks by placing.
 * <p>
 * Those still in the game come first, then the placings from the winner down. The lines are gone once the
 * event has been settled and paid out, so this is a panel for while it runs and shortly after.
 */
public final class EventResultUi {

    private static final int ROWS = 45;

    private EventResultUi() {
    }

    /**
     * Loads the lines and opens the panel once they are there.
     *
     * @param player who is looking
     * @param event  the event
     */
    public static void open(Player player, EventData event) {
        EventResultService.fetchAsync(event.getId(), rows -> {
            if (!player.isOnline()) return;
            player.openInventory(build(player, event, rows).getInventory());
        });
    }

    private static CustomInventory build(Player player, EventData event, List<EventResultData> rows) {
        CustomInventory ui = new CustomInventory(9 * 6, ChatColor.GOLD + "Ergebnis", close -> {
        });
        ui.fillPlaceHolder();

        List<EventResultData> sorted = new ArrayList<>(rows);
        // still standing first, then from the winner down; kills settle a tie among the living
        sorted.sort(Comparator.<EventResultData>comparingInt(row -> row.getPlace() == 0 ? 0 : 1)
                .thenComparingInt(EventResultData::getPlace)
                .thenComparing(Comparator.comparingInt(EventResultData::getKills).reversed()));

        if (sorted.isEmpty()) {
            ui.setItem(22, new ItemApi(Material.PAPER, ChatColor.GRAY + "Noch keine Ergebnisse",
                    List.of(ChatColor.GRAY + "Nach der Abrechnung sind sie weg -",
                            ChatColor.GRAY + "die Belohnungen sind dann verteilt.")).build(),
                    SimpleItemAction.display());
        }
        for (int i = 0; i < sorted.size() && i < ROWS; i++) {
            ui.setItem(i, head(sorted.get(i), player), SimpleItemAction.display());
        }

        ui.setItem(45, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click -> player.openInventory(EventDetailUi.build(player, event).getInventory())));
        ui.setItem(49, RewardUi.icon(event, false), SimpleItemAction.display());
        return ui;
    }

    private static ItemStack head(EventResultData row, Player viewer) {
        String place = row.getPlace() == 0 ? ChatColor.GREEN + "lebt" : ChatColor.GOLD + "#" + row.getPlace();
        String title = place + ChatColor.WHITE + " " + row.getPlayerName()
                + (viewer.getUniqueId().equals(row.getPlayerId()) ? ChatColor.GRAY + " (du)" : "");
        ItemStack head = new ItemApi(Material.PLAYER_HEAD, title,
                List.of(ChatColor.GRAY + "Kills: " + ChatColor.WHITE + row.getKills())).build();
        if (head.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(row.getPlayerId()));
            head.setItemMeta(skull);
        }
        return head;
    }
}
