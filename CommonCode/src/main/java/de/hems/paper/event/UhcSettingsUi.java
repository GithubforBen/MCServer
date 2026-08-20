package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.UhcSettings;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * The rules of a run event, as buttons. Everything here is a toggle or a step, because these are the knobs
 * that get changed between rounds and nobody wants to edit a config for that.
 */
public final class UhcSettingsUi {

    /** The team sizes on offer. */
    private static final int[] TEAM_SIZES = {1, 2, 3, 4};
    /** How many attempts a player gets, zero meaning no limit. */
    private static final int[] RUN_LIMITS = {1, 3, 5, 10, 0};

    private UhcSettingsUi() {
    }

    /**
     * @param player the admin changing the rules
     * @param event  the event being changed
     * @return the panel
     */
    public static CustomInventory build(Player player, EventData event) {
        UhcSettings settings = new UhcSettings(event);
        CustomInventory ui = new CustomInventory(9 * 3, ChatColor.GREEN + "Regeln", close -> {
        });
        ui.fillPlaceHolder();

        ui.setItem(10, new ItemApi(settings.isHardcore() ? Material.RED_DYE : Material.GRAY_DYE,
                ChatColor.GOLD + "Hardcore: " + (settings.isHardcore() ? "an" : "aus"),
                List.of(ChatColor.GRAY + "Ein Tod beendet den Lauf.")).build(),
                new SimpleItemAction(click -> apply(player, event,
                        edited -> new UhcSettings(edited).setHardcore(!settings.isHardcore()))));

        int nextSize = next(TEAM_SIZES, settings.getTeamSize());
        ui.setItem(11, new ItemApi(Material.PLAYER_HEAD,
                ChatColor.GOLD + "Teamgröße: " + settings.getTeamSize(),
                List.of(ChatColor.GRAY + "Klicken für: " + nextSize)).build(),
                new SimpleItemAction(click -> apply(player, event,
                        edited -> new UhcSettings(edited).setTeamSize(nextSize))));

        int currentLimit = settings.getMaxRuns() == Integer.MAX_VALUE ? 0 : settings.getMaxRuns();
        int nextLimit = next(RUN_LIMITS, currentLimit);
        ui.setItem(12, new ItemApi(Material.CLOCK,
                ChatColor.GOLD + "Versuche: " + (currentLimit == 0 ? "unbegrenzt" : currentLimit),
                List.of(ChatColor.GRAY + "Pro Person.",
                        ChatColor.GRAY + "Klicken für: " + (nextLimit == 0 ? "unbegrenzt" : nextLimit))).build(),
                new SimpleItemAction(click -> apply(player, event,
                        edited -> new UhcSettings(edited).setMaxRuns(nextLimit))));

        ui.setItem(13, new ItemApi(settings.isAllowUndermanned() ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.GOLD + "Unterbesetzt starten: " + (settings.isAllowUndermanned() ? "erlaubt" : "nein"),
                List.of(ChatColor.GRAY + "Eine kleinere Gruppe darf trotzdem los.",
                        ChatColor.GRAY + "Sie hat es dann schwerer.")).build(),
                new SimpleItemAction(click -> apply(player, event,
                        edited -> new UhcSettings(edited).setAllowUndermanned(!settings.isAllowUndermanned()))));

        ui.setItem(18, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click ->
                        player.openInventory(UhcEventUi.build(player, event).getInventory())));
        return ui;
    }

    /**
     * Changes one rule and stores the event.
     *
     * @param player who is changing it
     * @param event  the event
     * @param change what to change on a copy
     */
    private static void apply(Player player, EventData event, java.util.function.Consumer<EventData> change) {
        EventData edited = event.copy();
        change.accept(edited);
        EventService.saveAsync(edited, false, result -> {
            if (!result.successful()) {
                player.sendMessage(ChatColor.RED + "❌ " + result.message());
                return;
            }
            player.openInventory(build(player, result.event()).getInventory());
        });
    }

    /**
     * @param values  the presets to cycle through
     * @param current where we are now
     * @return the next preset, wrapping around
     */
    private static int next(int[] values, int current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return values[0];
    }
}
