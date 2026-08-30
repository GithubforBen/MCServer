package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.EventType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One event up close, with the buttons an admin needs to call it off or remove it.
 */
public final class EventDetailUi {

    private EventDetailUi() {
    }

    /**
     * @param player who is looking
     * @param event  the event to show
     * @return the panel
     */
    public static CustomInventory build(Player player, EventData event) {
        CustomInventory ui = new CustomInventory(9 * 3, ChatColor.GOLD + "Event", close -> {
        });
        ui.fillPlaceHolder();

        ui.setItem(13, EventCalendarUi.icon(event), SimpleItemAction.display());

        // a race has a queue, rules and a leaderboard - far more than fits here, so it gets its own panel
        if (event.getType().isTimed()) {
            ui.setItem(9, new ItemApi(Material.NETHER_STAR, ChatColor.AQUA + "Mitmachen & Bestenliste",
                    List.of(ChatColor.GRAY + "Warteschlange, Regeln und Zeiten")).build(),
                    new SimpleItemAction(click ->
                            player.openInventory(UhcEventUi.build(player, event).getInventory())));
        }

        // the round of a bedwars event goes up minutes before the event does, and its own waiting lobby
        // is a better place to stand around in than the hub
        if (event.getType() == EventType.BEDWARS && BedwarsEventStarter.serverOf(event) != null
                && (event.getState() == EventState.PLANNED || event.getState() == EventState.RUNNING)) {
            ui.setItem(9, new ItemApi(Material.RED_BED, ChatColor.GREEN + "Zur Bedwars-Lobby",
                    List.of(ChatColor.GRAY + "Die Runde wartet schon.",
                            ChatColor.GRAY + "Gestartet wird sie zur Eventzeit.")).build(),
                    new SimpleItemAction(click -> {
                        player.closeInventory();
                        player.sendMessage(ChatColor.AQUA + BedwarsEventStarter.join(player, event));
                    }));
        }

        if (!event.getSettings().isEmpty()) {
            List<String> lore = new ArrayList<>();
            for (Map.Entry<String, String> setting : event.getSettings().entrySet()) {
                lore.add(ChatColor.GRAY + setting.getKey() + ": " + ChatColor.WHITE + setting.getValue());
            }
            ui.setItem(11, new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Einstellungen", lore).build(),
                    SimpleItemAction.display());
        }

        ui.setItem(18, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click ->
                        player.openInventory(EventCalendarUi.build(player, EventCalendarUi.Filter.ALL).getInventory())));

        if (!player.isOp()) return ui;

        boolean over = event.getState() == EventState.FINISHED;
        if (!over) {
            boolean cancelled = event.isCancelled();
            ui.setItem(15, new ItemApi(cancelled ? Material.LIME_DYE : Material.RED_DYE,
                    cancelled ? ChatColor.GREEN + "Wieder aktivieren" : ChatColor.RED + "Event absagen",
                    List.of(ChatColor.GRAY + (cancelled
                            ? "Das Event läuft wieder nach Plan."
                            : "Das Event findet nicht statt."))).build(),
                    new SimpleItemAction(click -> {
                        EventData edited = event.copy();
                        edited.setCancelled(!cancelled);
                        EventService.saveAsync(edited, false, result -> {
                            player.sendMessage(result.successful()
                                    ? ChatColor.GREEN + "✓ " + edited.getName() + " aktualisiert."
                                    : ChatColor.RED + "❌ " + result.message());
                            player.openInventory(EventCalendarUi.build(player,
                                    EventCalendarUi.Filter.ALL).getInventory());
                        });
                    }));
        }

        ui.setItem(26, new ItemApi(Material.BARRIER, ChatColor.RED + "Event löschen",
                List.of(ChatColor.GRAY + "Entfernt das Event ganz.",
                        ChatColor.DARK_RED + "Kann nicht rückgängig gemacht werden.")).build(),
                new SimpleItemAction(click -> {
                    EventService.deleteAsync(event.getId());
                    player.sendMessage(ChatColor.GREEN + "✓ " + event.getName() + " wurde gelöscht.");
                    player.closeInventory();
                }));
        return ui;
    }
}
