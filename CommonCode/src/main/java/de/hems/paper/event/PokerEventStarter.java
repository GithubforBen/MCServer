package de.hems.paper.event;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.ServerTemplate;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.EventType;
import de.hems.types.event.PokerEventSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puts the casino up when a poker night starts, and takes everybody along.
 * <p>
 * Deliberately the same shape as {@link BedwarsEventStarter}, down to writing the server's name back onto
 * the event: that name is the only thing tying the two together, because a server is told what it is called
 * and nothing else. The casino reads the event by looking for its own name and finds out from it what the
 * buy-in is, what the house keeps and whether it is dealing a cash game or a tournament.
 * <p>
 * Where it differs is what happens at the event's time. A bedwars round starts, so everybody has to be
 * there; a poker night opens, and people wander in over the evening. So nobody is dragged across: the
 * casino is announced with a button, and whoever wants to play walks through the door themselves. Being
 * teleported out of what you were doing into a card table you did not ask for is not an invitation.
 */
public final class PokerEventStarter {

    /** How often the calendar is checked, in ticks. */
    private static final long CHECK_INTERVAL_TICKS = 20L * 15L;
    /**
     * How long before the night the casino is put up, in minutes.
     * <p>
     * Same reasoning as the bedwars round: shorter than the launcher's idle shutdown, or the server is
     * switched off again before the night it was put up for.
     */
    private static final long LEAD_MINUTES = 5L;
    /** How often the room is reminded that the casino is open, in minutes. */
    private static final long REMINDER_MINUTES = 15L;

    /** The events this server has already acted on, so a slow write is not started twice. */
    private static final Set<UUID> started = new HashSet<>();
    /** When the room was last told about a night, so the reminder does not become spam. */
    private static final Map<UUID, Long> reminded = new HashMap<>();
    private static boolean initialized;

    private PokerEventStarter() {
    }

    /**
     * Starts watching the calendar.
     *
     * @param plugin the plugin the background work belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        Bukkit.getScheduler().runTaskTimer(plugin, PokerEventStarter::check,
                CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    /**
     * Looks for a poker night whose casino has to go up, and for one that is open.
     */
    private static void check() {
        if (!EventService.isLoaded()) return;
        for (EventData event : EventService.getEvents()) {
            if (event.getType() != EventType.POKER) continue;
            EventState state = event.getState();
            if (state != EventState.RUNNING && !startsSoon(event)) continue;

            PokerEventSettings settings = new PokerEventSettings(event);
            if (settings.getServer() == null) {
                if (started.add(event.getId())) create(event, state == EventState.RUNNING);
                continue;
            }
            if (state == EventState.RUNNING) remind(event);
        }
    }

    /**
     * @param event an event that has not begun
     * @return whether it begins soon enough that its casino should already be open
     */
    private static boolean startsSoon(EventData event) {
        if (event.getState() != EventState.PLANNED) return false;
        long untilStart = event.getStartsAt() - System.currentTimeMillis();
        return untilStart <= LEAD_MINUTES * 60_000L;
    }

    /**
     * @param event the event to look at
     * @return the server its casino is on, or {@code null} while there is none yet
     */
    public static @Nullable String serverOf(EventData event) {
        return new PokerEventSettings(event).getServer();
    }

    /**
     * Sends one player to the casino.
     *
     * @param player who wants to go
     * @param event  the night they want to play
     * @return what to tell them
     */
    public static String join(Player player, EventData event) {
        String server = serverOf(event);
        if (server == null) {
            return "Das Casino steht noch nicht bereit.";
        }
        EventState state = event.getState();
        if (state != EventState.PLANNED && state != EventState.RUNNING) {
            return "Diese Pokernacht läuft nicht mehr.";
        }
        ServerStartup.warpWhenReady(player, server);
        return "Du wirst zum Casino verbunden.";
    }

    /**
     * Puts the casino up and writes its name onto the event.
     * <p>
     * The name is written before the server is ordered, and that order is not free: the casino reads the
     * buy-in, the blinds and the rake off the event by looking for its own name, and it does that in the
     * first second of its life. Writing afterwards is a race it can lose, and a casino that loses it deals
     * at the wrong stakes for real money.
     *
     * @param event      the event
     * @param nowRunning whether the night has already begun
     */
    private static void create(EventData event, boolean nowRunning) {
        Bukkit.getServer().sendMessage(Component.text(event.getName()
                + " - das Casino wird vorbereitet.", NamedTextColor.GREEN));
        PaperContext.async(() -> {
            String name;
            try {
                name = ServerApi.freeName("POKER_" + shortId(event.getId()));
            } catch (Exception e) {
                Bukkit.getLogger().warning("The casino of " + event.getName()
                        + " could not be named: " + e.getMessage());
                started.remove(event.getId());
                return;
            }
            EventData updated = event.copy();
            PokerEventSettings settings = new PokerEventSettings(updated);
            // an event that was created before this version, or through the website, may have no knobs at
            // all. Filling them in here means the casino always starts against a complete set rather than
            // making up its own numbers, which would be a different set than the calendar is showing
            settings.applyDefaults();
            settings.setServer(name);
            EventService.Result result = EventService.saveBlocking(updated, false);
            if (!result.successful()) {
                Bukkit.getLogger().warning("The casino of " + event.getName()
                        + " could not be written down: " + result.message());
                started.remove(event.getId());
                return;
            }
            try {
                ServerApi.createServer(name, ServerTemplate.POKER, null, null);
            } catch (Exception e) {
                Bukkit.getLogger().warning("The casino of " + event.getName()
                        + " could not be started: " + e.getMessage());
                started.remove(event.getId());
                return;
            }
            PaperContext.sync(() -> announce(event, nowRunning));
        });
    }

    /**
     * Tells the room the casino is there.
     *
     * @param event      the night
     * @param nowRunning whether it has already begun, which changes only the wording
     */
    private static void announce(EventData event, boolean nowRunning) {
        reminded.put(event.getId(), System.currentTimeMillis());
        PokerEventSettings settings = new PokerEventSettings(event);
        Component headline = Component.text(nowRunning
                        ? event.getName() + " läuft - das Casino ist offen."
                        : event.getName() + " startet gleich - das Casino ist offen.",
                NamedTextColor.GOLD);
        Bukkit.getServer().sendMessage(headline
                .append(Component.newline())
                .append(Component.text("Buy-in " + settings.getBuyIn() + " Bits · Blinds "
                                + settings.getSmallBlind() + "/" + settings.getBigBlind()
                                + " · Haus " + settings.getRakeText(), NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("[Zum Casino]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/events"))
                        .hoverEvent(HoverEvent.showText(Component.text(
                                "Öffnet den Kalender - dort steht der Knopf zum Tisch")))));
    }

    /**
     * Reminds the room now and then that there is a table running, for the people who logged in after the
     * announcement went past.
     *
     * @param event the night
     */
    private static void remind(EventData event) {
        long last = reminded.getOrDefault(event.getId(), 0L);
        if (System.currentTimeMillis() - last < REMINDER_MINUTES * 60_000L) return;
        // nobody to remind is not a reason to burn the interval - the next person to log in should still
        // hear about it within the quarter hour
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        announce(event, true);
    }

    /**
     * @param id the event
     * @return the first block of its id, short enough for a server name
     */
    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
