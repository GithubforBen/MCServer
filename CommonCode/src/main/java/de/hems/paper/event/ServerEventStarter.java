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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Puts a server up for an event and brings the players to it - for every kind of event that is played on a
 * server of its own.
 * <p>
 * A kind of event needs one {@link #define} chain. The rest is the same for all of them and happens here:
 * the server goes up a few minutes before the event, its name is written onto the event before it is
 * ordered (the server finds its event by that name in the first second of its life, and writing afterwards
 * is a race it can lose), the lobby is invited, and at the start the players are brought over.
 * <p>
 * How they are brought over is the one real difference between the kinds, and there are two:
 * <ul>
 *     <li><b>Everybody goes</b> - a bedwars round or hunger games <em>starts</em>, so whoever is in the
 *     lobby is sent over, and people who log in later follow for as long as {@link Builder#warpFor} says.</li>
 *     <li><b>Walk in</b> - a poker night <em>opens</em>. Nobody is dragged across; the room is told the
 *     door is open and reminded now and then, and whoever wants to play goes.</li>
 * </ul>
 * Runs on the hub, because that is where the players are.
 */
public final class ServerEventStarter {

    /** How often the calendar is checked, in ticks. */
    private static final long CHECK_INTERVAL_TICKS = 20L * 15L;

    private static final Map<EventType, ServerEventStarter> starters = new EnumMap<>(EventType.class);
    private static boolean initialized;

    static {
        define(EventType.BEDWARS, ServerTemplate.BEDWARS, "BEDWARS")
                .door("Zur Bedwars-Lobby", event -> List.of("Die Runde wartet schon.",
                        "Gestartet wird sie zur Eventzeit."))
                .invite(event -> Component.text(event.getName()
                        + " startet in Kürze - die Bedwars-Lobby ist offen.", NamedTextColor.AQUA))
                .joinText("Du wirst zur Bedwars-Lobby verbunden.")
                // late comers join the round for as long as the event runs; the round itself decides
                // whether they play or watch
                .warpFor(0)
                .register();
        define(EventType.POKER, ServerTemplate.POKER, "POKER")
                .door("Zum Casino", event -> {
                    PokerEventSettings poker = new PokerEventSettings(event);
                    return List.of("Buy-in: " + poker.getBuyIn() + " Bits",
                            "Blinds: " + poker.getSmallBlind() + "/" + poker.getBigBlind(),
                            "Haus: " + poker.getRakeText() + " pro Pot",
                            "Gespielt wird um echte Bits.");
                })
                // an event from before this version, or from the website, may have no knobs at all -
                // the casino must start against a complete set rather than making up its own numbers
                .prepare(event -> new PokerEventSettings(event).applyDefaults())
                .walkIn(15, ServerEventStarter::pokerOpen)
                .joinText("Du wirst zum Casino verbunden.")
                .register();
        define(EventType.HUNGER_GAMES, ServerTemplate.HUNGER_GAMES, "HUNGER_GAMES")
                .door("Zur Arena", event -> List.of("Wer nach dem Start kommt, schaut zu."))
                .warpFor(2)
                .register();
    }

    private final EventType type;
    private final ServerTemplate template;
    private final String prefix;
    private final String serverKey;
    private final long leadMinutes;
    private final long warpMinutes;
    private final boolean walkIn;
    private final long reminderMinutes;
    private final Function<EventData, Component> openAnnouncement;
    private final Function<EventData, Component> invite;
    private final Consumer<EventData> prepare;
    private final String doorTitle;
    private final Function<EventData, List<String>> doorLore;
    private final String joinText;

    /** The events this server has already acted on, so a slow write is not started twice. */
    private final Set<UUID> started = new HashSet<>();
    /** Who has already been sent to which event, so nobody is dragged back every fifteen seconds. */
    private final Map<UUID, Set<UUID>> sent = new HashMap<>();
    /** When the room was last told about a walk-in event, so the reminder does not become spam. */
    private final Map<UUID, Long> reminded = new HashMap<>();

    private ServerEventStarter(Builder builder) {
        this.type = builder.type;
        this.template = builder.template;
        this.prefix = builder.prefix;
        this.serverKey = builder.type.getServerKey();
        this.leadMinutes = builder.leadMinutes;
        this.warpMinutes = builder.warpMinutes;
        this.walkIn = builder.walkIn;
        this.reminderMinutes = builder.reminderMinutes;
        this.openAnnouncement = builder.openAnnouncement;
        this.invite = builder.invite;
        this.prepare = builder.prepare;
        this.doorTitle = builder.doorTitle;
        this.doorLore = builder.doorLore;
        this.joinText = builder.joinText;
    }

    /**
     * Starts describing how one kind of event is put up. The type must name the setting its server goes
     * into ({@link EventType#getServerKey()}).
     *
     * @param type     the kind of event
     * @param template what its server is made from
     * @param prefix   how its server names begin - keep it the template's name, so a restarted server is
     *                 recognised as what it is
     * @return a builder, finished with {@link Builder#register()}
     */
    public static Builder define(EventType type, ServerTemplate template, String prefix) {
        if (type.getServerKey() == null) {
            throw new IllegalArgumentException(type + " has no server setting in EventType");
        }
        return new Builder(type, template, prefix);
    }

    /** How one kind of event is put up. */
    public static final class Builder {
        private final EventType type;
        private final ServerTemplate template;
        private final String prefix;
        private long leadMinutes = 5;
        private long warpMinutes = 0;
        private boolean walkIn;
        private long reminderMinutes;
        private Function<EventData, Component> openAnnouncement;
        private Function<EventData, Component> invite = event -> Component.text(event.getName()
                + " startet in Kürze - du kannst schon hin.", NamedTextColor.AQUA);
        private Consumer<EventData> prepare = event -> {
        };
        private String doorTitle = "Zum Event";
        private Function<EventData, List<String>> doorLore = event -> List.of();
        private String joinText;

        private Builder(EventType type, ServerTemplate template, String prefix) {
            this.type = type;
            this.template = template;
            this.prefix = prefix;
        }

        /**
         * @param minutes how long before the event the server goes up. Keep it shorter than the launcher's
         *                idle shutdown, or the empty server is switched off before the event it was for
         * @return this builder
         */
        public Builder lead(long minutes) {
            this.leadMinutes = minutes;
            return this;
        }

        /**
         * @param minutes how long after the start the lobby keeps sending newcomers over, zero for as long
         *                as the event runs
         * @return this builder
         */
        public Builder warpFor(long minutes) {
            this.warpMinutes = minutes;
            return this;
        }

        /**
         * Nobody is sent over; the room is told the door is open instead, and reminded.
         *
         * @param reminderMinutes how often the room is reminded while the event runs
         * @param announcement    what the room is told
         * @return this builder
         */
        public Builder walkIn(long reminderMinutes, Function<EventData, Component> announcement) {
            this.walkIn = true;
            this.reminderMinutes = reminderMinutes;
            this.openAnnouncement = announcement;
            return this;
        }

        /**
         * @param invite what the room is told when the server goes up before the event
         * @return this builder
         */
        public Builder invite(Function<EventData, Component> invite) {
            this.invite = invite;
            return this;
        }

        /**
         * @param prepare what to fill in on the event right before its server is ordered
         * @return this builder
         */
        public Builder prepare(Consumer<EventData> prepare) {
            this.prepare = prepare;
            return this;
        }

        /**
         * @param title what the button into the event is called
         * @param lore  what it says underneath, uncoloured
         * @return this builder
         */
        public Builder door(String title, Function<EventData, List<String>> lore) {
            this.doorTitle = title;
            this.doorLore = lore;
            return this;
        }

        /**
         * @param text what a player is told when they go through the door
         * @return this builder
         */
        public Builder joinText(String text) {
            this.joinText = text;
            return this;
        }

        /**
         * @return the starter, now known to the calendar and the event panel
         */
        public ServerEventStarter register() {
            ServerEventStarter starter = new ServerEventStarter(this);
            synchronized (ServerEventStarter.class) {
                starters.put(type, starter);
            }
            return starter;
        }
    }

    /**
     * @param type the kind of event
     * @return its starter, or {@code null} for a kind that is not played on a server of its own
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
        if (state != EventState.RUNNING) return;
        if (walkIn) {
            remind(event);
        } else if (withinWarpWindow(event)) {
            warpNewcomers(event, server);
        }
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
     * @param event the event
     * @return the lines under the door button, uncoloured
     */
    public List<String> describeDoor(EventData event) {
        return doorLore.apply(event);
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
        return joinText != null ? joinText : "Du wirst zu " + event.getName() + " verbunden.";
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
            EventData updated = event.copy();
            prepare.accept(updated);
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
                if (walkIn) {
                    announceOpen(result.event());
                    return;
                }
                if (nowRunning) {
                    Bukkit.getServer().sendMessage(Component.text(event.getName() + " - es geht los!",
                            NamedTextColor.GREEN));
                    ServerStartup.ensureAndWarp(remember(event, Bukkit.getOnlinePlayers()), name, template);
                    return;
                }
                Bukkit.getServer().sendMessage(invite.apply(event)
                        .append(Component.newline())
                        .append(calendarButton("[Jetzt hingehen]")));
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

    /**
     * Reminds the room now and then that a walk-in event is open, for the people who logged in after the
     * announcement went past.
     */
    private void remind(EventData event) {
        long last = reminded.getOrDefault(event.getId(), 0L);
        if (System.currentTimeMillis() - last < reminderMinutes * 60_000L) return;
        // nobody to remind is not a reason to burn the interval - the next person to log in should still
        // hear about it in time
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        announceOpen(event);
    }

    private void announceOpen(EventData event) {
        reminded.put(event.getId(), System.currentTimeMillis());
        Bukkit.getServer().sendMessage(openAnnouncement.apply(event)
                .append(Component.newline())
                .append(calendarButton("[" + doorTitle + "]")));
    }

    private static Component calendarButton(String text) {
        return Component.text(text, NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/events"))
                .hoverEvent(HoverEvent.showText(Component.text(
                        "Öffnet den Kalender - dort steht der Knopf zum Event")));
    }

    /**
     * What the room is told about an open poker night.
     */
    private static Component pokerOpen(EventData event) {
        PokerEventSettings settings = new PokerEventSettings(event);
        boolean running = event.getState() == EventState.RUNNING;
        return Component.text(running
                        ? event.getName() + " läuft - das Casino ist offen."
                        : event.getName() + " startet gleich - das Casino ist offen.", NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text("Buy-in " + settings.getBuyIn() + " Bits · Blinds "
                        + settings.getSmallBlind() + "/" + settings.getBigBlind()
                        + " · Haus " + settings.getRakeText(), NamedTextColor.GRAY));
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
