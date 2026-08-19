package de.hems.utils.webconsole;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.admin.ApplyInventoryEvent;
import de.hems.communication.events.admin.RequestCoreProtectEvent;
import de.hems.communication.events.admin.RequestInventoryEvent;
import de.hems.communication.events.admin.RequestMaterialsEvent;
import de.hems.communication.events.admin.RequestPlayerActionEvent;
import de.hems.communication.events.admin.RequestPlayersEvent;
import de.hems.communication.events.admin.RespondPlayersEvent;
import de.hems.communication.events.admin.RespondActionEvent;
import de.hems.communication.events.admin.RespondCoreProtectEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.admin.CoreProtectEntry;
import de.hems.types.admin.InventoryData;
import de.hems.types.admin.LookupQuery;
import de.hems.types.admin.PlayerSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * How the website reaches the game servers.
 * <p>
 * The launcher has no bukkit and knows nothing about players, so every question is broadcast to the whole
 * network and the servers that can answer do. That keeps the launcher out of the business of tracking who
 * is where - a player moving from the lobby to survival needs no bookkeeping here.
 * <p>
 * Everything in here blocks while it waits for answers. That is fine on a web request thread and would not
 * be anywhere near a game server's main thread.
 */
public final class AdminNetwork {

    /** How long to collect answers to a broadcast that everybody replies to. */
    private static final Duration COLLECT_WINDOW = Duration.ofMillis(900);
    /** How long to wait for the one server that has the player in question. */
    private static final Duration SINGLE_TIMEOUT = Duration.ofSeconds(3);
    /** A lookup goes to a database, so it gets noticeably longer. */
    private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(15);

    private AdminNetwork() {
    }

    /**
     * @return whether the network is reachable at all
     */
    public static boolean isAvailable() {
        return ListenerAdapter.isInitialized();
    }

    /**
     * Asks every server who is on it and how well it is running.
     * <p>
     * One broadcast answers both questions, because the servers that report their players are exactly the
     * ones whose load is worth showing - a server that is still booting or already gone simply does not
     * answer and is correctly absent from the overview.
     *
     * @return the players of the whole network and the servers that reported them
     */
    public static Network network() throws Exception {
        List<PlayerSnapshot> players = new ArrayList<>();
        List<ServerLoad> servers = new ArrayList<>();
        RequestPlayersEvent request = new RequestPlayersEvent();
        ListenerAdapter.sendListeners(request);
        for (RespondDataEvent response : ListenerAdapter.waitForEvents(request.getEventId(), COLLECT_WINDOW)) {
            int count = 0;
            if (response.getData() instanceof List<?> list) {
                for (Object entry : list) {
                    if (!(entry instanceof PlayerSnapshot snapshot)) continue;
                    players.add(snapshot);
                    count++;
                }
            }
            double tps = response instanceof RespondPlayersEvent reported ? reported.getTps() : 20.0d;
            servers.add(new ServerLoad(String.valueOf(response.getSender()), tps, count));
        }
        servers.sort(Comparator.comparing(ServerLoad::name));
        return new Network(players, servers);
    }

    /**
     * Asks every server who is online there.
     *
     * @return the players of the whole network
     */
    public static List<PlayerSnapshot> players() throws Exception {
        return network().players();
    }

    /**
     * Which servers are actually on the bus right now.
     *
     * @return the names of the servers that answered
     */
    public static List<String> respondingServers() throws Exception {
        List<String> names = new ArrayList<>();
        for (ServerLoad server : network().servers()) names.add(server.name());
        return names;
    }

    /**
     * What one server reported about itself.
     *
     * @param name    the server
     * @param tps     how well it is keeping up
     * @param players how many players are on it
     */
    public record ServerLoad(String name, double tps, int players) {
    }

    /**
     * The state of the whole network in one answer.
     *
     * @param players every player, across all servers
     * @param servers the servers that answered
     */
    public record Network(List<PlayerSnapshot> players, List<ServerLoad> servers) {
    }

    /**
     * Fetches one container of a player from whichever server has them.
     *
     * @param playerId    the player
     * @param kind        which container
     * @param containerId which backpack, when a backpack is meant
     * @return its contents, or {@code null} if the player is not online anywhere
     */
    public static InventoryData inventory(UUID playerId, InventoryData.Kind kind, String containerId)
            throws Exception {
        RequestInventoryEvent request = new RequestInventoryEvent(playerId, kind, containerId);
        ListenerAdapter.sendListeners(request);
        // the servers without that player answer with nothing, so the first real answer ends the wait
        List<RespondDataEvent> responses = ListenerAdapter.waitForEvents(
                request.getEventId(), SINGLE_TIMEOUT, event -> event.getData() != null);
        for (RespondDataEvent response : responses) {
            if (response.getData() instanceof InventoryData inventory) return inventory;
        }
        return null;
    }

    /**
     * Writes an edited container back.
     *
     * @param inventory the container as the browser sent it
     * @param editor    who made the change
     * @return what to report, or {@code null} if no server took it
     */
    public static String applyInventory(InventoryData inventory, String editor) throws Exception {
        ApplyInventoryEvent request = new ApplyInventoryEvent(inventory, editor);
        ListenerAdapter.sendListeners(request);
        return firstSuccessfulMessage(request.getEventId());
    }

    /**
     * Does something to a player.
     *
     * @param playerId the player
     * @param action   what to do
     * @param argument what the action needs, may be {@code null}
     * @param editor   who asked for it
     * @return what to report, or {@code null} if the player was not found
     */
    public static String action(UUID playerId, RequestPlayerActionEvent.Action action, String argument,
                                String editor) throws Exception {
        RequestPlayerActionEvent request = new RequestPlayerActionEvent(playerId, action, argument, editor);
        ListenerAdapter.sendListeners(request);
        return firstSuccessfulMessage(request.getEventId());
    }

    /**
     * @param requestId the request the answers belong to
     * @return the message of the first server that reported success, or {@code null}
     */
    private static String firstSuccessfulMessage(UUID requestId) throws InterruptedException {
        List<RespondDataEvent> responses = ListenerAdapter.waitForEvents(requestId, SINGLE_TIMEOUT,
                event -> event instanceof RespondActionEvent action && action.isSuccessful());
        for (RespondDataEvent response : responses) {
            if (response instanceof RespondActionEvent action && action.isSuccessful()) {
                return String.valueOf(action.getData());
            }
        }
        return null;
    }

    /** The material list, kept for the life of the process - it cannot change while the servers run. */
    private static volatile List<String> materialCache;

    /**
     * The materials the item editor offers.
     * <p>
     * They come from a game server rather than from the launcher's own classpath: bukkit's registry is only
     * populated inside a running server, so asking {@code Material.values()} here would fail.
     *
     * @return every material that can be an item, sorted
     */
    public static List<String> materials() throws Exception {
        List<String> cached = materialCache;
        if (cached != null) return cached;
        RequestMaterialsEvent request = new RequestMaterialsEvent();
        ListenerAdapter.sendListeners(request);
        List<RespondDataEvent> responses = ListenerAdapter.waitForEvents(
                request.getEventId(), SINGLE_TIMEOUT, event -> event.getData() != null);
        for (RespondDataEvent response : responses) {
            if (!(response.getData() instanceof List<?> list) || list.isEmpty()) continue;
            List<String> materials = new ArrayList<>();
            for (Object entry : list) {
                if (entry instanceof String name) materials.add(name);
            }
            java.util.Collections.sort(materials);
            materialCache = materials;
            return materials;
        }
        return List.of();
    }

    /**
     * Runs a CoreProtect lookup on one server.
     *
     * @param server the server to ask - each keeps its own CoreProtect database
     * @param query  what to look up
     * @return the rows that were found
     * @throws IllegalStateException if the server could not run the lookup
     */
    public static List<CoreProtectEntry> lookup(ListenerAdapter.ServerName server, LookupQuery query)
            throws Exception {
        RequestCoreProtectEvent request = new RequestCoreProtectEvent(server, query);
        ListenerAdapter.sendListeners(request);
        List<RespondDataEvent> responses = ListenerAdapter.waitForEvents(
                request.getEventId(), LOOKUP_TIMEOUT, event -> event instanceof RespondCoreProtectEvent);
        for (RespondDataEvent response : responses) {
            if (!(response instanceof RespondCoreProtectEvent lookup)) continue;
            if (lookup.getError() != null) throw new IllegalStateException(lookup.getError());
            List<CoreProtectEntry> entries = new ArrayList<>();
            if (lookup.getData() instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof CoreProtectEntry row) entries.add(row);
                }
            }
            return entries;
        }
        throw new IllegalStateException(server + " hat nicht geantwortet. Läuft der Server?");
    }
}
