package de.hems.paper.event;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.ClaimGhostRunEvent;
import de.hems.communication.events.event.RespondClaimGhostRunEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.ServerTemplate;
import de.hems.types.event.EventData;
import de.hems.types.event.RunData;
import de.hems.types.event.UhcSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is waiting to start a run, per event.
 * <p>
 * Deliberately not stored anywhere. A queue only means anything while the people in it are online, so it
 * lives in memory on the server the players are standing on and is gone when that server stops - which is
 * exactly what should happen to it.
 */
public final class RunQueue {

    /** How long the launcher is given to hand out the ghost server before one is built instead. */
    private static final java.time.Duration GHOST_TIMEOUT = java.time.Duration.ofSeconds(5);

    /** Everyone waiting, per event, in the order they clicked. */
    private static final Map<UUID, Set<UUID>> waiting = new ConcurrentHashMap<>();

    private RunQueue() {
    }

    /**
     * @param event the event to look at
     * @return who is waiting, in the order they joined
     */
    public static List<UUID> getWaiting(EventData event) {
        return new ArrayList<>(waiting.getOrDefault(event.getId(), Set.of()));
    }

    /**
     * @param event  the event to look at
     * @param player the player to check
     * @return whether they are already queued
     */
    public static boolean isWaiting(EventData event, UUID player) {
        return waiting.getOrDefault(event.getId(), Set.of()).contains(player);
    }

    /**
     * Puts a player into the queue, or takes them back out if they were already in it.
     *
     * @param event  the event to queue for
     * @param player who is queueing
     * @return what to tell them
     */
    public static String toggle(EventData event, Player player) {
        UUID id = player.getUniqueId();
        Set<UUID> queue = waiting.computeIfAbsent(event.getId(), key -> new LinkedHashSet<>());
        if (queue.remove(id)) {
            if (queue.isEmpty()) waiting.remove(event.getId());
            return "Du bist aus der Warteschlange raus.";
        }
        if (!event.isRunning()) {
            return "Dieses Event läuft gerade nicht.";
        }
        if (RunService.getActiveRunOf(event.getId(), id) != null) {
            return "Du bist schon in einem laufenden Versuch.";
        }
        if (!RunService.hasRunsLeft(event, id)) {
            return "Du hast keine Versuche mehr übrig.";
        }
        UhcSettings settings = new UhcSettings(event);
        if (queue.size() >= settings.getTeamSize()) {
            return "Die Warteschlange ist schon voll.";
        }
        queue.add(id);
        announce(event, queue);
        // a full queue starts by itself, that is what the size is for
        if (queue.size() >= settings.getTeamSize()) {
            start(event, player);
            return "Die Gruppe ist voll - es geht los!";
        }
        return "Du wartest jetzt mit " + queue.size() + "/" + settings.getTeamSize() + " Leuten.";
    }

    /**
     * Tells everyone in a queue who else is in it.
     *
     * @param event the event
     * @param queue who is waiting
     */
    private static void announce(EventData event, Set<UUID> queue) {
        UhcSettings settings = new UhcSettings(event);
        for (UUID member : queue) {
            Player online = Bukkit.getPlayer(member);
            if (online == null) continue;
            online.sendMessage(Component.text(event.getName() + ": " + queue.size() + "/"
                    + settings.getTeamSize() + " bereit", NamedTextColor.AQUA));
        }
    }

    /**
     * Starts the run for everyone waiting.
     * <p>
     * The group may be smaller than the team size when the event allows it - the run is marked as
     * undermanned so the leaderboard can tell later that it was the harder way round.
     *
     * @param event  the event to run
     * @param source who pressed start, for the error messages
     * @return what to tell them
     */
    public static String start(EventData event, Player source) {
        Set<UUID> queue = waiting.getOrDefault(event.getId(), Set.of());
        if (queue.isEmpty()) {
            return "Es wartet niemand.";
        }
        UhcSettings settings = new UhcSettings(event);
        if (queue.size() < settings.getTeamSize() && !settings.isAllowUndermanned()) {
            return "Ihr seid noch nicht genug (" + queue.size() + "/" + settings.getTeamSize() + ").";
        }
        Set<UUID> participants = new LinkedHashSet<>(queue);
        waiting.remove(event.getId());
        launch(event, participants);
        return "Der Lauf wird vorbereitet.";
    }

    /**
     * Starts the next run of a team straight away, without going through the queue - what {@code /reset}
     * does once the old run is closed.
     * <p>
     * The same rules as in the queue: the event has to be running, everybody needs an attempt left and
     * must not be on another run, and a smaller team only goes when the event allows it. Whoever has no
     * attempt left stays behind rather than holding the others up.
     *
     * @param event   the event to run
     * @param players who wants to go again
     * @param left    filled with the players that can not come along, with why
     * @return why nobody goes, or {@code null} when the run is on its way
     */
    public static String restart(EventData event, Set<UUID> players, Map<UUID, String> left) {
        if (event == null || !event.isRunning()) {
            return "Das Event läuft nicht mehr - es gibt keinen neuen Lauf.";
        }
        Set<UUID> participants = new LinkedHashSet<>();
        for (UUID player : players) {
            if (RunService.getActiveRunOf(event.getId(), player) != null) {
                left.put(player, "Du bist schon in einem laufenden Versuch.");
            } else if (!RunService.hasRunsLeft(event, player)) {
                left.put(player, "Du hast keine Versuche mehr übrig.");
            } else {
                participants.add(player);
            }
        }
        if (participants.isEmpty()) {
            return "Niemand von euch hat noch einen Versuch übrig.";
        }
        UhcSettings settings = new UhcSettings(event);
        if (participants.size() < settings.getTeamSize() && !settings.isAllowUndermanned()) {
            return "Ihr seid nicht mehr genug für einen neuen Lauf (" + participants.size() + "/"
                    + settings.getTeamSize() + ").";
        }
        launch(event, participants);
        return null;
    }

    /**
     * Opens a run and gets its team onto a server.
     * <p>
     * The ghost server of the event comes first: it was built while nobody needed it, so whoever claims
     * it skips the wait for a world to be generated. Only when another team claimed it first - or there is
     * none - a server of its own is built, the way it always was.
     *
     * @param event        the event to run
     * @param participants who runs
     */
    private static void launch(EventData event, Set<UUID> participants) {
        UhcSettings settings = new UhcSettings(event);
        RunData run = new RunData(event.getId(), participants);
        run.setIntendedTeamSize(settings.getTeamSize());

        // the server is found in the background: asking the launcher takes a round trip, building one takes
        // seconds, and neither may freeze the tick
        PaperContext.async(() -> {
            String ghost = claimGhost(run);
            String serverName = ghost;
            if (serverName == null) {
                try {
                    serverName = ListenerAdapter.ServerName.valueOf(ServerApi.freeName(
                            RunData.serverNameFor(event.getId(), run.getId()))).toString();
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not name a run server: " + e.getMessage());
                    PaperContext.sync(() -> tell(participants,
                            "Der Server für den Lauf konnte nicht gestartet werden.", NamedTextColor.RED));
                    return;
                }
            }
            run.setServerName(serverName);
            // a claimed ghost is already written into the run on the launcher, this only says it again
            RunService.save(run);
            String target = serverName;
            PaperContext.sync(() -> {
                tell(participants, ghost != null
                                ? "Euer Lauf startet auf einem vorbereiteten Server - gleich geht's los."
                                : "Euer Lauf startet - ihr werdet verbunden, sobald der Server bereit ist.",
                        NamedTextColor.GREEN);
                if (run.isUndermanned()) {
                    tell(participants, "Ihr startet zu " + participants.size() + " statt zu "
                            + settings.getTeamSize() + " - das wird schwerer.", NamedTextColor.YELLOW);
                }
                // the warp is not sent now: a run server needs the better part of a minute to build its
                // world, and everybody thrown at it before that is bounced straight back by the proxy. A
                // ghost may still be on its way up as well, so it is waited for the same way
                if (ghost != null) {
                    ServerStartup.ensureAndWarp(onlineOf(participants), target, ServerTemplate.EVENT);
                } else {
                    ServerStartup.createAndWarp(onlineOf(participants), target, ServerTemplate.EVENT, null, null);
                }
            });
        });
    }

    /**
     * Asks the launcher for the ghost server of the run's event. Blocks, so never on the main thread.
     *
     * @param run the new run
     * @return the server it got, or {@code null} when there was none to have
     */
    private static String claimGhost(RunData run) {
        RespondDataEvent answer = ListenerAdapter.ask(new ClaimGhostRunEvent(run), GHOST_TIMEOUT);
        if (answer instanceof RespondClaimGhostRunEvent claimed) return claimed.getServerName();
        // no answer in time: the launcher may still have said yes, and then the run it announced carries
        // the ghost - building a second server for it would leave the ghost standing for nobody
        RunData known = RunService.getRun(run.getId());
        return known == null ? null : known.getServerName();
    }

    /**
     * Picks an open run back up.
     * <p>
     * The server keeps its name, and with it its directory, so starting it again brings back the same
     * world with the same progress. The clock starts moving again the moment somebody who belongs to the
     * run is standing on it. A run that still counts as running is picked up the same way: its server may
     * have gone without saying so, and then this is what brings it back.
     *
     * @param event  the event the run belongs to
     * @param player who wants to carry on
     * @return what to tell them
     */
    public static String resume(EventData event, Player player) {
        RunData run = RunService.getActiveRunOf(event.getId(), player.getUniqueId());
        if (run == null) {
            return "Du hast keinen offenen Lauf.";
        }
        if (run.getServerName() == null) {
            return "Zu diesem Lauf gehört kein Server mehr.";
        }
        Set<UUID> participants = new LinkedHashSet<>(run.getParticipants());
        tell(participants, "Euer Lauf geht weiter - ihr werdet verbunden, sobald der Server bereit ist.",
                NamedTextColor.GREEN);
        // the launcher remembers the server, so this reuses its port and its world rather than building a
        // new one - and a server that is already running is simply waited for and warped to
        ServerStartup.ensureAndWarp(onlineOf(participants), run.getServerName(), ServerTemplate.EVENT);
        return "Der Server wird gestartet.";
    }

    /**
     * Calls an open run off, for everybody on it.
     * <p>
     * This is the way out of a run nobody wants to finish - a bad start, a world that is no fun, a server
     * that never came up. Without it the run stays open, and an open run is what keeps its team from
     * starting the next one. The run server notices by itself, sends whoever is still on it back to the
     * lobby and switches itself off.
     *
     * @param event  the event the run belongs to
     * @param player who is calling it off
     * @return what to tell them
     */
    public static String abort(EventData event, Player player) {
        RunData run = RunService.getActiveRunOf(event.getId(), player.getUniqueId());
        if (run == null) {
            return "Du hast keinen offenen Lauf.";
        }
        boolean counts = run.getElapsedTicksRaw() > 0;
        run.finish(RunData.State.ABANDONED);
        RunService.save(run);
        Set<UUID> others = new LinkedHashSet<>(run.getParticipants());
        others.remove(player.getUniqueId());
        tell(others, player.getName() + " hat euren Lauf abgebrochen.", NamedTextColor.YELLOW);
        return counts ? "Der Lauf ist abgebrochen. Er zählt als Versuch."
                : "Der Lauf ist abgebrochen. Er hatte noch nicht begonnen und kostet keinen Versuch.";
    }

    /**
     * Takes a player out of every queue, used when they log off.
     *
     * @param player who left
     */
    public static void forget(UUID player) {
        for (Map.Entry<UUID, Set<UUID>> entry : waiting.entrySet()) {
            entry.getValue().remove(player);
        }
        waiting.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * @param players the participants of a run
     * @return the ones that are online here right now
     */
    private static List<Player> onlineOf(Set<UUID> players) {
        List<Player> online = new ArrayList<>();
        for (UUID member : players) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) online.add(player);
        }
        return online;
    }

    private static void tell(Set<UUID> players, String message, NamedTextColor color) {
        for (UUID member : players) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) online.sendMessage(Component.text(message, color));
        }
    }
}
