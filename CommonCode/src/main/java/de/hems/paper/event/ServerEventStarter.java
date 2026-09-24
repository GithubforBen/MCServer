package de.hems.paper.event;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.ServerTemplate;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.EventType;
import de.hems.types.event.HungerGamesSettings;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puts a server up for an event and takes everybody along - the general form of what
 * {@link BedwarsEventStarter} and {@link PokerEventStarter} each do for themselves.
 * <p>
 * A kind of event that is played on its own server needs one {@link #register} call: which template the
 * server is made from, which setting its name is written to, and how long people keep being taken along
 * after the start. The rest is the same for all of them and happens here - the server goes up a few minutes
 * before the event, its name is written onto the event before it is ordered (the server finds its event by
 * that name in the first second of its life), the lobby is invited, and at the start everybody in the lobby
 * is sent over.
 * <p>
 * Runs on the hub, because that is where the players are.
 */
public final class ServerEventStarter {

    /** How often the calendar is checked, in ticks. */
    private static final long CHECK_INTERVAL_TICKS = 20L * 15L;

    private static final Map<EventType, ServerEventStarter> starters = new EnumMap<>(EventType.class);
    private static boolean initialized;

    static {
        register(new ServerEventStarter(EventType.HUNGER_GAMES, ServerTemplate.HUNGER_GAMES, "HUNGER_GAMES",
                HungerGamesSettings.SERVER, 5, 2, "Zur Arena"));
    }

    private final EventType type;
    private final ServerTemplate template;
    private final String prefix;
    private final String serverKey;
    private final long leadMinutes;
    private final long warpMinutes;
    private final String doorTitle;

    /** The events this server has already acted on, so a slow write is not started twice. */
    private final Set<UUID> started = new HashSet<>();
    /** Who has already been sent to which event, so nobody is dragged back every fifteen seconds. */
    private final Map<UUID, Set<UUID>> sent = new HashMap<>();

    /**
     * @param type        the kind of event
     * @param template    what its server is made from
     * @param prefix      how its server names begin
     * @param serverKey   the setting the server's name is written to
     * @param leadMinutes how long before the event the server goes up. Keep it shorter than the launcher's
     *                    idle shutdown, or the empty server is switched off before the event it was for
     * @param warpMinutes how long after the start the lobby keeps sending people over, zero for as long as
     *                    the event runs
     * @param doorTitle   what the button into it is called
     */
    public ServerEventStarter(EventType type, ServerTemplate template, String prefix, String serverKey,
                              long leadMinutes, long warpMinutes, String doorTitle) {
        this.type = type;
        this.template = template;
        this.prefix = prefix;
        this.serverKey = serverKey;
        this.leadMinutes = leadMinutes;
        this.warpMinutes = warpMinutes;
        this.doorTitle = doorTitle;
    }

    /**
     * @param starter the starter of one kind of event, replacing one of the same type
     */
    public static synchronized void register(ServerEventStarter starter) {
        starters.put(starter.type, starter);
    }

    /**
     * @param type the kind of event
     * @return its starter, or {@code null} for a kind that is not started through here
     */
    public static synchronized @Nullable ServerEventStarter of(EventType type) {
        return starters.get(type);
    }

    /**
     * Starts watching the calendar for every registered kind. Only the hub calls this.
     *
     * @param plugin the plugin the background work belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        Bukkit.getScheduler().runTaskTimer(plugin, ServerEventStarter::checkAll,
                CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    private static void checkAll() {
        if (!EventService.isLoaded()) return;
        List<ServerEventStarter> all;
        synchronized (ServerEventStarter.class) {
            all = new ArrayList<>(starters.values());
        }
        for (EventData event : EventService.getEvents()) {
            for (ServerEventStarter starter : all) {
                if (starter.type == event.getType()) starter.check(event);
            }
        }
    }

    private void check(EventData event) {
        EventState state = event.getState();
        if (state != EventState.RUNNING && !startsSoon(event)) return;
        String server = serverOf(event);
        if (server == null) {
            if (started.add(event.getId())) create(event, state == EventState.RUNNING);
            return;
        }
        if (state == EventState.RUNNING && withinWarpWindow(event)) warpNewcomers(event, server);
    }

    private boolean startsSoon(EventData event) {
        if (event.getState() != EventState.PLANNED) return false;
        return event.getStartsAt() - System.currentTimeMillis() <= leadMinutes * 60_000L;
    }

    private boolean withinWarpWindow(EventData event) {
        if (warpMinutes <= 0) return true;
        return System.currentTimeMillis() - event.getStartsAt() <= warpMinutes * 60_000L;
    }

    /**
     * @param event the event
     * @return the server it is played on, or {@code null} while there is none
     */
    public @Nullable String serverOf(EventData event) {
        String server = event.getSetting(serverKey, "");
        return server == null || server.isBlank() ? null : server;
    }

    public String getDoorTitle() {
        return doorTitle;
    }

    /**
     * Sends one player over by their own choice.
     *
     * @param player who wants to go
     * @param event  the event
     * @return what to tell them
     */
    public String join(Player player, EventData event) {
        String server = serverOf(event);
        if (server == null) return "Der Server steht noch nicht bereit.";
        if (event.getState() != EventState.PLANNED && event.getState() != EventState.RUNNING) {
            return "Dieses Event läuft nicht mehr.";
        }
        sent.computeIfAbsent(event.getId(), key -> new HashSet<>()).add(player.getUniqueId());
        ServerStartup.warpWhenReady(player, server);
        return "Du wirst zu " + event.getName() + " verbunden.";
    }

    private void create(EventData event, boolean nowRunning) {
        Bukkit.getServer().sendMessage(Component.text(event.getName()
                + " - der Server wird vorbereitet.", NamedTextColor.GREEN));
        PaperContext.async(() -> {
            String name;
            try {
                name = ServerApi.freeName(prefix + "_" + event.getId().toString().substring(0, 8)
                        .toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                Bukkit.getLogger().warning("The server of " + event.getName() + " could not be named: "
                        + e.getMessage());
                started.remove(event.getId());
                return;
            }
            // written before the server is ordered: it looks for its event by its own name as it starts,
            // and writing afterwards is a race it can lose
            EventData updated = event.copy();
            updated.setSetting(serverKey, name);
            EventService.Result result = EventService.saveBlocking(updated, false);
            if (!result.successful()) {
                Bukkit.getLogger().warning("The server of " + event.getName() + " could not be written down: "
                        + result.message());
                started.remove(event.getId());
                return;
            }
            try {
                ServerApi.createServer(name, template, null, null);
            } catch (Exception e) {
                Bukkit.getLogger().warning("The server of " + event.getName() + " could not be started: "
                        + e.getMessage());
                started.remove(event.getId());
                return;
            }
            PaperContext.sync(() -> {
                if (nowRunning) {
                    Bukkit.getServer().sendMessage(Component.text(event.getName() + " - es geht los!",
                            NamedTextColor.GREEN));
                    ServerStartup.ensureAndWarp(remember(event, Bukkit.getOnlinePlayers()), name, template);
                    return;
                }
                invite(event);
            });
        });
    }

    /**
     * Sends whoever has arrived since the start after the others. Never {@code ensureAndWarp}: a server that
     * is gone by now is over, and starting it again would put up an empty one for somebody walking in late.
     */
    private void warpNewcomers(EventData event, String server) {
        List<Player> late = remember(event, Bukkit.getOnlinePlayers());
        if (late.isEmpty()) return;
        for (Player player : late) {
            player.sendMessage(Component.text(event.getName() + " läuft - du wirst verbunden.",
                    NamedTextColor.GREEN));
        }
        ServerStartup.warpWhenReady(late, server);
    }

    private static void invite(EventData event) {
        Bukkit.getServer().sendMessage(Component.text(event.getName()
                        + " startet in Kürze - du kannst schon hin.", NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Component.text("[Jetzt hingehen]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/events"))
                        .hoverEvent(HoverEvent.showText(Component.text(
                                "Öffnet den Kalender - dort steht der Knopf zum Event")))));
    }

    private List<Player> remember(EventData event, Collection<? extends Player> online) {
        Set<UUID> already = sent.computeIfAbsent(event.getId(), key -> new HashSet<>());
        List<Player> fresh = new ArrayList<>();
        for (Player player : online) {
            if (already.add(player.getUniqueId())) fresh.add(player);
        }
        return fresh;
    }
}
