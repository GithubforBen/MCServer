package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.EventSetting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The settings of any event, in one panel.
 * <p>
 * Reached through the same button from the create panel, the event panel and the race panel, so the
 * settings of an event are always in the same place. It draws whatever the event's {@link EventDefinition}
 * lists - a switch per toggle, a stepping button per number - and hands a kind with a panel of its own over
 * to that panel.
 */
public final class EventSettingsUi {

    /** Where the settings go, two rows of seven with a margin. */
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private EventSettingsUi() {
    }

    /**
     * @param player who is editing
     * @param edit   the event and where changes go
     */
    public static void open(Player player, EventEdit edit) {
        EventDefinition definition = EventDefinitions.of(edit.current().getType());
        if (definition.getPanel() != null) {
            openOwnPanel(player, edit, definition);
            return;
        }
        player.openInventory(build(player, edit, definition).getInventory());
    }

    /**
     * Hands over to a panel of its own. Those panels change the event in place and give it back when they
     * are left, so a live event gets a copy and is saved in one go on the way out.
     */
    private static void openOwnPanel(Player player, EventEdit edit, EventDefinition definition) {
        EventData working = edit.isDraft() ? edit.current() : edit.current().copy();
        definition.getPanel().open(player, working, edited -> {
            if (edit.isDraft()) {
                edit.back(player);
                return;
            }
            if (edited.getSettings().equals(edit.current().getSettings())) {
                edit.back(player);
                return;
            }
            edit.change(player, event -> event.setSettings(new LinkedHashMap<>(edited.getSettings())),
                    () -> edit.back(player));
        });
    }

    private static CustomInventory build(Player player, EventEdit edit, EventDefinition definition) {
        EventData event = edit.current();
        CustomInventory ui = new CustomInventory(9 * 4, ChatColor.GREEN + "Einstellungen", close -> {
        });
        ui.fillPlaceHolder();
        ui.setItem(4, EventCalendarUi.icon(event), SimpleItemAction.display());

        List<EventSetting> settings = definition.getSettings();
        if (settings.isEmpty()) {
            ui.setItem(13, new ItemApi(Material.PAPER, ChatColor.GRAY + "Nichts einzustellen",
                    List.of(ChatColor.GRAY + event.getType().getTitle() + " hat keine eigenen Regeln.")).build(),
                    SimpleItemAction.display());
        }
        for (int i = 0; i < settings.size() && i < SLOTS.length; i++) {
            EventSetting setting = settings.get(i);
            ui.setItem(SLOTS[i], button(setting, event), new SimpleItemAction(click ->
                    edit.change(player, changed -> setting.step(changed, !click.isRightClick()),
                            () -> player.openInventory(build(player, edit, definition).getInventory()))));
        }

        ui.setItem(27, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück",
                List.of(ChatColor.GRAY + (edit.isDraft()
                        ? "Übernommen wird beim Anlegen."
                        : "Jede Änderung ist schon gespeichert."))).build(),
                new SimpleItemAction(click -> edit.back(player)));
        return ui;
    }

    private static org.bukkit.inventory.ItemStack button(EventSetting setting, EventData event) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + setting.display(event));
        for (String line : setting.getDescription()) lore.add(ChatColor.DARK_GRAY + line);
        lore.add("");
        lore.add(setting.getKind() == EventSetting.Kind.TOGGLE
                ? ChatColor.GRAY + "Klicken zum Umschalten"
                : ChatColor.GRAY + "Links: weiter · Rechts: zurück");
        Material icon = Material.matchMaterial(setting.getIcon());
        if (setting.getKind() == EventSetting.Kind.TOGGLE) {
            icon = setting.read(event) == 1 ? Material.LIME_DYE : Material.GRAY_DYE;
        }
        return new ItemApi(icon == null ? Material.COMPARATOR : icon, ChatColor.GOLD + setting.getTitle(), lore)
                .build();
    }

    /**
     * The button that leads here, the same on every panel it appears on.
     *
     * @param event     the event
     * @param clickable whether the viewer can open it
     * @return its icon, with the current settings as lore
     */
    public static org.bukkit.inventory.ItemStack icon(EventData event, boolean clickable) {
        EventDefinition definition = EventDefinitions.of(event.getType());
        List<String> lore = new ArrayList<>();
        if (!definition.hasSettings()) {
            lore.add(ChatColor.GRAY + "Dieser Eventtyp hat keine Einstellungen.");
        } else {
            for (String line : definition.describe(event)) lore.add(ChatColor.GRAY + line);
            if (clickable) {
                lore.add("");
                lore.add(ChatColor.AQUA + "Klicken zum Bearbeiten");
            }
        }
        return new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Einstellungen", lore).build();
    }
}
