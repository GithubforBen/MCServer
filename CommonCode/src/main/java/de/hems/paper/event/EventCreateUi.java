package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.util.ChatPrompt;
import de.hems.types.event.BedwarsEventSettings;
import de.hems.types.event.EventData;
import de.hems.types.event.EventType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lets an admin put an event into the calendar without touching a config file.
 * <p>
 * Start and length are picked from presets rather than typed, because a date entered into chat is the one
 * thing that reliably goes wrong. Anything finer can still be corrected on the website.
 */
public final class EventCreateUi {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd.MM. HH:mm").withZone(ZoneId.systemDefault());

    /** How long until the event starts, offered as buttons. */
    private static final long[] START_OFFSETS_MINUTES = {0, 60, 60 * 6, 60 * 24, 60 * 24 * 3};
    /** How long the event runs, offered as buttons. */
    private static final long[] DURATIONS_MINUTES = {60, 60 * 3, 60 * 24, 60 * 24 * 5, 60 * 24 * 7};

    private EventCreateUi() {
    }

    /**
     * @param player the admin creating the event
     * @return the panel, working on a fresh draft
     */
    public static CustomInventory build(Player player) {
        EventData draft = new EventData("Neues Event", EventType.SIMPLE,
                System.currentTimeMillis() + Duration.ofHours(1).toMillis(),
                System.currentTimeMillis() + Duration.ofHours(2).toMillis());
        return build(player, draft, 60, 60);
    }

    /**
     * @param player         the admin creating the event
     * @param draft          the event as it stands
     * @param startOffsetMin how far out the start currently sits
     * @param durationMin    how long it currently runs
     * @return the panel
     */
    private static CustomInventory build(Player player, EventData draft, long startOffsetMin, long durationMin) {
        CustomInventory ui = new CustomInventory(9 * 4, ChatColor.GREEN + "Event anlegen", close -> {
        });
        ui.fillPlaceHolder();

        ui.setItem(4, EventCalendarUi.icon(draft), SimpleItemAction.display());

        ui.setItem(10, new ItemApi(Material.NAME_TAG, ChatColor.GOLD + "Name",
                List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + draft.getName())).build(),
                new SimpleItemAction(click -> ChatPrompt.ask(player, "Wie soll das Event heißen?", answer -> {
                    draft.setName(answer);
                    player.openInventory(build(player, draft, startOffsetMin, durationMin).getInventory());
                })));

        ui.setItem(11, new ItemApi(Material.BOOK, ChatColor.GOLD + "Beschreibung",
                List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE
                        + (draft.getDescription() == null ? "-" : draft.getDescription()))).build(),
                new SimpleItemAction(click -> ChatPrompt.ask(player, "Was soll dabei stehen?", answer -> {
                    draft.setDescription(answer);
                    player.openInventory(build(player, draft, startOffsetMin, durationMin).getInventory());
                })));

        List<EventType> types = EventCalendarUi.creatableTypes();
        EventType nextType = types.get((types.indexOf(draft.getType()) + 1) % types.size());
        ui.setItem(12, new ItemApi(Material.COMPARATOR, ChatColor.GOLD + "Typ",
                List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + draft.getType().getTitle(),
                        draft.getType().hasMechanics()
                                ? ChatColor.AQUA + "Mit eigener Mechanik"
                                : ChatColor.GRAY + "Nur eine Ankündigung",
                        ChatColor.GRAY + "Klicken für: " + nextType.getTitle())).build(),
                new SimpleItemAction(click -> {
                    draft.setType(nextType);
                    player.openInventory(build(player, draft, startOffsetMin, durationMin).getInventory());
                }));

        // only bedwars has anything to set here so far, and a button that does nothing on five of six
        // types is worse than one that appears when it means something
        if (draft.getType() == EventType.BEDWARS) {
            BedwarsEventSettings bedwars = new BedwarsEventSettings(draft);
            int size = bedwars.getTeamSize();
            int nextSize = size >= BedwarsEventSettings.MAX_TEAM_SIZE ? 1 : size + 1;
            ui.setItem(13, new ItemApi(Material.RED_BED, ChatColor.GOLD + "Teamgröße",
                    List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + size + " pro Team",
                            ChatColor.GRAY + "Modus: " + ChatColor.WHITE + bedwars.getMode(),
                            ChatColor.GRAY + "Klicken für: " + nextSize + " pro Team")).build(),
                    new SimpleItemAction(click -> {
                        new BedwarsEventSettings(draft).setTeamSize(nextSize);
                        player.openInventory(build(player, draft, startOffsetMin, durationMin).getInventory());
                    }));
        }

        long nextStart = nextValue(START_OFFSETS_MINUTES, startOffsetMin);
        ui.setItem(14, new ItemApi(Material.CLOCK, ChatColor.GOLD + "Start",
                List.of(ChatColor.GRAY + "Beginnt: " + ChatColor.WHITE
                                + WHEN.format(Instant.ofEpochMilli(draft.getStartsAt())),
                        ChatColor.GRAY + "In: " + ChatColor.WHITE + describe(startOffsetMin),
                        ChatColor.GRAY + "Klicken für: " + describe(nextStart))).build(),
                new SimpleItemAction(click -> {
                    retime(draft, nextStart, durationMin);
                    player.openInventory(build(player, draft, nextStart, durationMin).getInventory());
                }));

        long nextDuration = nextValue(DURATIONS_MINUTES, durationMin);
        ui.setItem(15, new ItemApi(Material.REPEATER, ChatColor.GOLD + "Dauer",
                List.of(ChatColor.GRAY + "Läuft: " + ChatColor.WHITE + describe(durationMin),
                        ChatColor.GRAY + "Endet: " + ChatColor.WHITE
                                + WHEN.format(Instant.ofEpochMilli(draft.getEndsAt())),
                        ChatColor.GRAY + "Klicken für: " + describe(nextDuration))).build(),
                new SimpleItemAction(click -> {
                    retime(draft, startOffsetMin, nextDuration);
                    player.openInventory(build(player, draft, startOffsetMin, nextDuration).getInventory());
                }));

        ui.setItem(27, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Abbrechen").build(),
                new SimpleItemAction(click -> player.openInventory(
                        EventCalendarUi.build(player, EventCalendarUi.Filter.ALL).getInventory())));

        ui.setItem(35, new ItemApi(Material.LIME_DYE, ChatColor.GREEN + "Anlegen",
                List.of(ChatColor.GRAY + "Legt das Event für alle Server an")).build(),
                new SimpleItemAction(click -> EventService.saveAsync(draft, true, result -> {
                    if (!result.successful()) {
                        player.sendMessage(ChatColor.RED + "❌ " + result.message());
                        return;
                    }
                    player.sendMessage(ChatColor.GREEN + "✓ " + draft.getName() + " wurde angelegt.");
                    player.openInventory(EventCalendarUi.build(player,
                            EventCalendarUi.Filter.ALL).getInventory());
                })));
        return ui;
    }

    /**
     * Moves the draft to a new start and length.
     *
     * @param draft          the event being built
     * @param startOffsetMin how far out it starts
     * @param durationMin    how long it runs
     */
    private static void retime(EventData draft, long startOffsetMin, long durationMin) {
        long start = System.currentTimeMillis() + Duration.ofMinutes(startOffsetMin).toMillis();
        draft.setStartsAt(start);
        draft.setEndsAt(start + Duration.ofMinutes(durationMin).toMillis());
    }

    /**
     * @param values  the presets to cycle through
     * @param current where we are now
     * @return the next preset, wrapping around
     */
    private static long nextValue(long[] values, long current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return values[0];
    }

    /**
     * @param minutes a span in minutes
     * @return it written out for a button
     */
    private static String describe(long minutes) {
        if (minutes == 0) return "sofort";
        if (minutes < 60) return minutes + " Min";
        if (minutes < 60 * 24) return (minutes / 60) + " Std";
        return (minutes / (60 * 24)) + " Tage";
    }
}
