package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.EventType;
import de.hems.types.event.PokerEventSettings;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * One event up close, with the buttons an admin needs to change it, call it off or remove it.
 * <p>
 * Laid out like the create panel: the event on top, settings and rewards in the same two slots, back in the
 * corner. Somebody who has made one event knows where everything is on the next one.
 */
public final class EventDetailUi {

    /** Where the button into the event itself sits - the round, the casino, the arena, the queue. */
    private static final int DOOR_SLOT = 10;
    /** Where the ranking sits, for the kinds that have one to show. */
    private static final int RANKING_SLOT = 16;

    private EventDetailUi() {
    }

    /**
     * @param player who is looking
     * @param event  the event to show
     * @return the panel
     */
    public static CustomInventory build(Player player, EventData event) {
        CustomInventory ui = new CustomInventory(9 * 4, ChatColor.GOLD + "Event", close -> {
        });
        ui.fillPlaceHolder();

        ui.setItem(4, EventCalendarUi.icon(event), SimpleItemAction.display());

        drawDoor(ui, player, event);

        // everybody may see the settings and what there is to win; only an admin changes them, and every
        // change is saved straight away
        boolean admin = player.isOp();
        EventEdit edit = EventEdit.live(event, back ->
                back.openInventory(build(back, fresh(event)).getInventory()));
        boolean hasSettings = EventDefinitions.of(event.getType()).hasSettings();
        ui.setItem(EventCreateUi.SETTINGS_SLOT, EventSettingsUi.icon(event, admin && hasSettings),
                admin && hasSettings
                        ? new SimpleItemAction(click -> EventSettingsUi.open(player, edit))
                        : SimpleItemAction.display());
        ui.setItem(EventCreateUi.REWARDS_SLOT, RewardUi.icon(event, admin),
                admin && event.getType().isRanked()
                        ? new SimpleItemAction(click -> RewardUi.open(player, edit))
                        : SimpleItemAction.display());

        ui.setItem(27, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click ->
                        player.openInventory(EventCalendarUi.build(player, EventCalendarUi.Filter.ALL).getInventory())));

        if (!admin) return ui;

        boolean over = event.getState() == EventState.FINISHED;
        if (!over) {
            boolean cancelled = event.isCancelled();
            ui.setItem(31, new ItemApi(cancelled ? Material.LIME_DYE : Material.RED_DYE,
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

        ui.setItem(35, new ItemApi(Material.BARRIER, ChatColor.RED + "Event löschen",
                List.of(ChatColor.GRAY + "Entfernt das Event ganz.",
                        ChatColor.DARK_RED + "Kann nicht rückgängig gemacht werden.")).build(),
                new SimpleItemAction(click -> {
                    EventService.deleteAsync(event.getId());
                    player.sendMessage(ChatColor.GREEN + "✓ " + event.getName() + " wurde gelöscht.");
                    player.closeInventory();
                }));
        return ui;
    }

    /**
     * @param event the event as it was when the panel opened
     * @return it as the network has it now, which is what a panel opened after a change has to show
     */
    private static EventData fresh(EventData event) {
        EventData current = EventService.getEvent(event.getId());
        return current == null ? event : current;
    }

    /**
     * The way into the event itself, for the kinds that have one, and the ranking beside it.
     */
    private static void drawDoor(CustomInventory ui, Player player, EventData event) {
        boolean open = event.getState() == EventState.PLANNED || event.getState() == EventState.RUNNING;

        // a race has a queue, rules and a leaderboard - far more than fits here, so it gets its own panel
        if (event.getType().isTimed()) {
            ui.setItem(DOOR_SLOT, new ItemApi(Material.NETHER_STAR, ChatColor.AQUA + "Mitmachen & Bestenliste",
                            List.of(ChatColor.GRAY + "Warteschlange, Regeln und Zeiten")).build(),
                    new SimpleItemAction(click ->
                            player.openInventory(UhcEventUi.build(player, event).getInventory())));
        }

        // the round of a bedwars event goes up minutes before the event does, and its own waiting lobby
        // is a better place to stand around in than the hub
        if (event.getType() == EventType.BEDWARS && BedwarsEventStarter.serverOf(event) != null && open) {
            ui.setItem(DOOR_SLOT, new ItemApi(Material.RED_BED, ChatColor.GREEN + "Zur Bedwars-Lobby",
                            List.of(ChatColor.GRAY + "Die Runde wartet schon.",
                                    ChatColor.GRAY + "Gestartet wird sie zur Eventzeit.")).build(),
                    new SimpleItemAction(click -> {
                        player.closeInventory();
                        player.sendMessage(ChatColor.AQUA + BedwarsEventStarter.join(player, event));
                    }));
        }

        // a poker night is two doors: one into the casino, one onto the board. The board stays open after
        // the night is over, because that is when people want to look at it
        if (event.getType() == EventType.POKER) {
            if (PokerEventStarter.serverOf(event) != null && open) {
                PokerEventSettings poker = new PokerEventSettings(event);
                ui.setItem(DOOR_SLOT, new ItemApi(Material.PLAYER_HEAD, ChatColor.GREEN + "Zum Casino",
                                List.of(ChatColor.GRAY + "Buy-in: " + ChatColor.WHITE + poker.getBuyIn() + " Bits",
                                        ChatColor.GRAY + "Blinds: " + ChatColor.WHITE + poker.getSmallBlind()
                                                + "/" + poker.getBigBlind(),
                                        ChatColor.GRAY + "Haus: " + ChatColor.WHITE + poker.getRakeText()
                                                + ChatColor.GRAY + " pro Pot",
                                        ChatColor.DARK_GRAY + "Gespielt wird um echte Bits.")).build(),
                        new SimpleItemAction(click -> {
                            player.closeInventory();
                            player.sendMessage(ChatColor.AQUA + PokerEventStarter.join(player, event));
                        }));
            }
            ui.setItem(RANKING_SLOT, new ItemApi(Material.GOLD_BLOCK, ChatColor.GOLD + "Rangliste",
                            List.of(ChatColor.GRAY + "Wer hat aus seinem Einsatz am meisten gemacht")).build(),
                    new SimpleItemAction(click ->
                            player.openInventory(PokerRankingUi.build(player, event).getInventory())));
        }

        // an event started through the general starter - hunger games and whatever comes after it
        ServerEventStarter starter = ServerEventStarter.of(event.getType());
        if (starter != null) {
            if (starter.serverOf(event) != null && open) {
                ui.setItem(DOOR_SLOT, new ItemApi(EventDefinitions.of(event.getType()).getIcon(),
                                ChatColor.GREEN + starter.getDoorTitle(),
                                List.of(ChatColor.GRAY + "Klicken zum Hingehen",
                                        ChatColor.DARK_GRAY + "Wer nach dem Start kommt, schaut zu.")).build(),
                        new SimpleItemAction(click -> {
                            player.closeInventory();
                            player.sendMessage(ChatColor.AQUA + starter.join(player, event));
                        }));
            }
            ui.setItem(RANKING_SLOT, new ItemApi(Material.GOLD_BLOCK, ChatColor.GOLD + "Ergebnis",
                            List.of(ChatColor.GRAY + "Platzierungen und Kills")).build(),
                    new SimpleItemAction(click -> EventResultUi.open(player, event)));
        }
    }
}
