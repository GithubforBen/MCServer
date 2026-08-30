package de.hems.paper.event;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.ServerTemplate;
import de.hems.types.event.BedwarsEventSettings;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.EventType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puts a bedwars round up when a bedwars event starts, and takes everybody along.
 * <p>
 * Runs on the hub, because that is where the players are: an event is a time on a calendar, and what has
 * to happen at that time is that a server exists and that the people standing around get moved onto it.
 * <p>
 * The server's name is written back into the event. That is what ties the two together afterwards - the
 * round itself reads it to find out which event it is playing and how big its teams are, and a second
 * check of the calendar sees the name and knows the round has already been started.
 */
public final class BedwarsEventStarter {

    /** How often the calendar is checked for a round that should be running, in ticks. */
    private static final long CHECK_INTERVAL_TICKS = 20L * 15L;
    /**
     * How long before an event its round server is put up, in minutes.
     * <p>
     * Deliberately shorter than the launcher's {@code idle-shutdown-minutes}, which defaults to ten: an
     * empty server that is put up too early is switched off again before the event it was put up for. If
     * it is switched off anyway, the start warps everybody at the event's time and brings it back.
     */
    private static final long LEAD_MINUTES = 5L;

    /** The events this server has already acted on, so a slow write is not started twice. */
    private static final Set<UUID> started = new HashSet<>();
    /** Who has already been sent to which round, so nobody is dragged back every fifteen seconds. */
    private static final Map<UUID, Set<UUID>> sent = new HashMap<>();
    private static boolean initialized;

    private BedwarsEventStarter() {
    }

    /**
     * Starts watching the calendar.
     *
     * @param plugin the plugin the background work belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        Bukkit.getScheduler().runTaskTimer(plugin, BedwarsEventStarter::check,
                CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    /**
     * Looks for a bedwars event whose round has to be put up, and for one whose time has come.
     */
    private static void check() {
        if (!EventService.isLoaded()) return;
        for (EventData event : EventService.getEvents()) {
            if (event.getType() != EventType.BEDWARS) continue;
            EventState state = event.getState();
            if (state != EventState.RUNNING && !startsSoon(event)) continue;

            BedwarsEventSettings settings = new BedwarsEventSettings(event);
            if (settings.getServer() == null) {
                if (started.add(event.getId())) create(event, settings, state == EventState.RUNNING);
                continue;
            }
            // the round is up. Before the event it is an open door; at the event it is where everybody
            // goes, whether they walked through it early or not
            if (state == EventState.RUNNING) warpNewcomers(event, settings.getServer());
        }
    }

    /**
     * @param event an event that has not begun
     * @return whether it begins soon enough that its round should already be waiting
     */
    private static boolean startsSoon(EventData event) {
        if (event.getState() != EventState.PLANNED) return false;
        long untilStart = event.getStartsAt() - System.currentTimeMillis();
        return untilStart <= LEAD_MINUTES * 60_000L;
    }

    /**
     * @param event the event to look at
     * @return the server its round is on, or {@code null} while there is none yet
     */
    public static @Nullable String serverOf(EventData event) {
        return new BedwarsEventSettings(event).getServer();
    }

    /**
     * Sends one player to a round that is already waiting.
     * <p>
     * This is the early door: the round server is up minutes before the event, its own waiting lobby is
     * open, and standing in that is a better place to wait than standing in the hub.
     *
     * @param player who wants to go
     * @param event  the event whose round they want
     * @return what to tell them
     */
    public static String join(Player player, EventData event) {
        String server = serverOf(event);
        if (server == null) {
            return "Die Runde steht noch nicht bereit.";
        }
        if (event.getState() != EventState.PLANNED && event.getState() != EventState.RUNNING) {
            return "Dieses Event läuft nicht mehr.";
        }
        sent.computeIfAbsent(event.getId(), key -> new HashSet<>()).add(player.getUniqueId());
        ServerStartup.warpWhenReady(player, server);
        return "Du wirst zur Bedwars-Lobby verbunden.";
    }

    /**
     * Creates the round of one event and writes its name back onto the event.
     *
     * @param event    the event
     * @param settings its knobs
     * @param nowRunning whether the event has already begun, so everybody goes at once rather than being
     *                   invited
     */
    private static void create(EventData event, BedwarsEventSettings settings, boolean nowRunning) {
        Bukkit.getServer().sendMessage(Component.text(event.getName()
                + " - die Runde wird vorbereitet.", NamedTextColor.GREEN));
        PaperContext.async(() -> {
            String name;
            try {
                name = ServerApi.freeName("BEDWARS_" + shortId(event.getId()));
            } catch (Exception e) {
                Bukkit.getLogger().warning("The bedwars round of " + event.getName()
                        + " could not be named: " + e.getMessage());
                started.remove(event.getId());
                return;
            }
            // the name is written onto the event before the server is ordered, and that order matters:
            // the round reads its team size off the event by looking for its own name, and it does that
            // in the first second of its life. Writing afterwards is a race it can lose
            EventData updated = event.copy();
            new BedwarsEventSettings(updated).setServer(name);
            EventService.Result result = EventService.saveBlocking(updated, false);
            if (!result.successful()) {
                Bukkit.getLogger().warning("The bedwars round of " + event.getName()
                        + " could not be written down: " + result.message());
                started.remove(event.getId());
                return;
            }
            try {
                ServerApi.createServer(name, ServerTemplate.BEDWARS, null, null);
            } catch (Exception e) {
                Bukkit.getLogger().warning("The bedwars round of " + event.getName()
                        + " could not be started: " + e.getMessage());
                started.remove(event.getId());
                return;
            }
            PaperContext.sync(() -> {
                if (nowRunning) {
                    announce(event);
                    ServerStartup.ensureAndWarp(remember(event, Bukkit.getOnlinePlayers()), name,
                            ServerTemplate.BEDWARS);
                    return;
                }
                invite(event);
            });
        });
    }

    /**
     * Sends whoever has arrived since the round started after the others.
     * <p>
     * Never {@code ensureAndWarp}: by the time somebody logs in late the round may be over and its server
     * gone, and starting it again would put a fresh empty round up for one person who happened to walk in.
     *
     * @param event  the event, for the line they are told
     * @param server the round's server
     */
    private static void warpNewcomers(EventData event, String server) {
        List<Player> late = remember(event, Bukkit.getOnlinePlayers());
        if (late.isEmpty()) return;
        for (Player player : late) {
            player.sendMessage(Component.text(event.getName() + " läuft - du wirst verbunden.",
                    NamedTextColor.GREEN));
        }
        ServerStartup.warpWhenReady(late, server);
    }

    /**
     * Tells everybody who is here that the round is starting.
     */
    private static void announce(EventData event) {
        Bukkit.getServer().sendMessage(Component.text(event.getName() + " - es geht los!",
                NamedTextColor.GREEN));
    }

    /**
     * Opens the door: the round is waiting, and anybody who wants to can go and stand in it already.
     */
    private static void invite(EventData event) {
        Bukkit.getServer().sendMessage(Component.text(event.getName()
                        + " startet in Kürze - die Bedwars-Lobby ist offen.", NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text("[Jetzt hingehen]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/events"))
                        .hoverEvent(HoverEvent.showText(Component.text(
                                "Öffnet den Kalender - dort steht der Knopf zur Runde")))));
    }

    /**
     * Writes down who is being sent to a round.
     *
     * @param event  the event
     * @param online everybody who is here
     * @return the ones that had not been sent yet
     */
    private static List<Player> remember(EventData event, Collection<? extends Player> online) {
        Set<UUID> already = sent.computeIfAbsent(event.getId(), key -> new HashSet<>());
        List<Player> fresh = new ArrayList<>();
        for (Player player : online) {
            if (already.add(player.getUniqueId())) fresh.add(player);
        }
        return fresh;
    }

    /**
     * @param id the event
     * @return the first block of its id, short enough for a server name
     */
    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
