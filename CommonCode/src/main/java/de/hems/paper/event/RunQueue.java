package de.hems.paper.event;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
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

        RunData run = new RunData(event.getId(), participants);
        run.setIntendedTeamSize(settings.getTeamSize());

        // the server is created in the background: it takes seconds to boot and must not freeze the tick
        PaperContext.async(() -> {
            String serverName;
            try {
                serverName = ListenerAdapter.ServerName.valueOf(
                        ServerApi.freeName("RUN_" + shortId(event.getId()))).toString();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not name a run server: " + e.getMessage());
                PaperContext.sync(() -> tell(participants,
                        "Der Server für den Lauf konnte nicht gestartet werden.", NamedTextColor.RED));
                return;
            }
            run.setServerName(serverName);
            RunService.save(run);
            PaperContext.sync(() -> {
                tell(participants, "Euer Lauf startet - ihr werdet verbunden, sobald der Server bereit ist.",
                        NamedTextColor.GREEN);
                if (run.isUndermanned()) {
                    tell(participants, "Ihr startet zu " + participants.size() + " statt zu "
                            + settings.getTeamSize() + " - das wird schwerer.", NamedTextColor.YELLOW);
                }
                // the warp is not sent now: a run server needs the better part of a minute to build its
                // world, and everybody thrown at it before that is bounced straight back by the proxy
                ServerStartup.createAndWarp(onlineOf(participants), serverName, ServerTemplate.EVENT, null, null);
            });
        });
        return "Der Lauf wird vorbereitet.";
    }

    /**
     * Picks a paused run back up.
     * <p>
     * The server keeps its name, and with it its directory, so starting it again brings back the same
     * world with the same progress. The clock starts moving again the moment somebody who belongs to the
     * run is standing on it.
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

    /**
     * @param id the event
     * @return the first block of its id, short enough for a server name
     */
    private static String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }
}
