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
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The event calendar as an inventory: what is running, what is coming and what is over.
 * <p>
 * Lives in CommonCode so the lobby and survival show the same thing. Admins get a button in the bottom row
 * to add one.
 */
public final class EventCalendarUi {

    /** How the start and end of an event are written out. */
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd.MM. HH:mm").withZone(ZoneId.systemDefault());

    private static final int SIZE = 9 * 6;
    private static final int CONTENT = 45;

    private EventCalendarUi() {
    }

    /**
     * Opens the calendar.
     *
     * @param player who wants to look
     */
    public static void open(Player player) {
        player.openInventory(build(player, Filter.ALL).getInventory());
    }

    /** Which events the calendar shows. */
    public enum Filter {
        ALL("Alle"), RUNNING("Nur laufende"), UPCOMING("Nur kommende");

        private final String title;

        Filter(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }

        boolean matches(EventData event) {
            return switch (this) {
                case ALL -> true;
                case RUNNING -> event.getState() == EventState.RUNNING;
                case UPCOMING -> event.getState() == EventState.PLANNED;
            };
        }
    }

    /**
     * @param player who is looking, which decides whether the admin button is there
     * @param filter which events to show
     * @return the calendar panel
     */
    public static CustomInventory build(Player player, Filter filter) {
        CustomInventory ui = new CustomInventory(SIZE, ChatColor.GOLD + "Eventkalender", close -> {
        });
        ui.fillPlaceHolder();

        List<EventData> shown = new ArrayList<>();
        for (EventData event : EventService.getEvents()) {
            if (filter.matches(event)) shown.add(event);
        }
        // running first, then what is coming, then what is over - the order somebody actually cares about
        shown.sort((left, right) -> {
            int byState = Integer.compare(rank(left), rank(right));
            return byState != 0 ? byState : Long.compare(left.getStartsAt(), right.getStartsAt());
        });

        if (!EventService.isLoaded()) {
            ui.setItem(22, new ItemApi(Material.BARRIER, ChatColor.RED + "Events werden geladen",
                    List.of(ChatColor.GRAY + "Der Hauptserver hat sich noch nicht gemeldet.")).build(),
                    SimpleItemAction.display());
        } else if (shown.isEmpty()) {
            ui.setItem(22, new ItemApi(Material.PAPER, ChatColor.GRAY + "Keine Events",
                    List.of(ChatColor.GRAY + "Hier ist gerade nichts los.")).build(),
                    SimpleItemAction.display());
        }
        for (int i = 0; i < shown.size() && i < CONTENT; i++) {
            EventData event = shown.get(i);
            ui.setItem(i, icon(event), new SimpleItemAction(click ->
                    player.openInventory(EventDetailUi.build(player, event).getInventory())));
        }

        Filter nextFilter = filter.next();
        ui.setItem(45, new ItemApi(Material.HOPPER, ChatColor.AQUA + "Filter: " + filter.getTitle(),
                List.of(ChatColor.GRAY + "Klicken für: " + nextFilter.getTitle())).build(),
                new SimpleItemAction(click -> player.openInventory(build(player, nextFilter).getInventory())));

        EventData next = EventService.getNext();
        ui.setItem(49, new ItemApi(Material.CLOCK, ChatColor.GOLD + "Nächstes Event",
                next == null
                        ? List.of(ChatColor.GRAY + "Nichts geplant.")
                        : List.of(ChatColor.WHITE + next.getName(),
                        ChatColor.GRAY + "startet in " + EventData.format(next.getTimeUntilStart()))).build(),
                SimpleItemAction.display());

        if (player.isOp()) {
            ui.setItem(53, new ItemApi(Material.WRITABLE_BOOK, ChatColor.GREEN + "Neues Event",
                    List.of(ChatColor.GRAY + "Ein Event anlegen")).build(),
                    new SimpleItemAction(click ->
                            player.openInventory(EventCreateUi.build(player).getInventory())));
        }
        return ui;
    }

    /**
     * @param event the event to weigh
     * @return its sort rank, running first
     */
    private static int rank(EventData event) {
        return switch (event.getState()) {
            case RUNNING -> 0;
            case PLANNED -> 1;
            case FINISHED -> 2;
            case CANCELLED -> 3;
        };
    }

    /**
     * @param event the event to draw
     * @return its icon, with the time frame and what it is doing right now
     */
    public static ItemStack icon(EventData event) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Typ: " + ChatColor.WHITE + event.getType().getTitle());
        lore.add(ChatColor.GRAY + "Status: " + stateColor(event) + event.getState().getTitle());
        lore.add(ChatColor.GRAY + "Von: " + ChatColor.WHITE + WHEN.format(Instant.ofEpochMilli(event.getStartsAt())));
        lore.add(ChatColor.GRAY + "Bis: " + ChatColor.WHITE + WHEN.format(Instant.ofEpochMilli(event.getEndsAt())));
        switch (event.getState()) {
            case PLANNED -> lore.add(ChatColor.YELLOW + "Startet in " + EventData.format(event.getTimeUntilStart()));
            case RUNNING -> lore.add(ChatColor.GREEN + "Noch " + EventData.format(event.getTimeUntilEnd()));
            default -> {
            }
        }
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            lore.add("");
            lore.add(ChatColor.GRAY + event.getDescription());
        }
        return new ItemApi(material(event), stateColor(event) + event.getName(), lore).build();
    }

    private static ChatColor stateColor(EventData event) {
        return switch (event.getState()) {
            case RUNNING -> ChatColor.GREEN;
            case PLANNED -> ChatColor.YELLOW;
            case FINISHED -> ChatColor.GRAY;
            case CANCELLED -> ChatColor.RED;
        };
    }

    /**
     * @param event the event to pick an icon for
     * @return a block that says at a glance what kind of event it is
     */
    private static Material material(EventData event) {
        if (event.getState() == EventState.CANCELLED) return Material.BARRIER;
        return switch (event.getType()) {
            case END -> Material.END_PORTAL_FRAME;
            case UHC_BOSSES -> Material.NETHER_STAR;
            case UHC_DRAGON -> Material.DRAGON_HEAD;
            case OTHER_WORLD -> Material.GRASS_BLOCK;
            case SIMPLE -> Material.PAPER;
        };
    }

    /**
     * Every type can be scheduled from the game. What separates them is whether code reacts to them - the
     * End event opens the End, a simple event is only an announcement.
     *
     * @return the types an admin may pick when creating an event
     */
    public static List<EventType> creatableTypes() {
        return List.of(EventType.values());
    }
}
