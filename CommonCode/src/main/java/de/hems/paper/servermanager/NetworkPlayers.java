package de.hems.paper.servermanager;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.admin.RequestPlayersEvent;
import de.hems.communication.events.admin.RespondPlayersEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.admin.PlayerSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Who is online where, as seen from a game server.
 * <p>
 * The launcher knows which servers run, but not who is on them - only the servers themselves do. One
 * broadcast collects that: every server answers with its own players, and the answers together are the
 * player list of the whole network. Servers that are still booting simply do not answer yet, which is
 * exactly right, since nobody can be on them.
 */
public final class NetworkPlayers {

    /** How long to keep collecting answers before showing what came in. */
    private static final Duration COLLECT_WINDOW = Duration.ofSeconds(2);

    private NetworkPlayers() {
    }

    /**
     * Asks every server who is on it. Blocks, so it must not run on the main thread.
     *
     * @return the players per server, keyed by the normalised server name
     */
    public static Map<String, List<PlayerSnapshot>> byServer() {
        Map<String, List<PlayerSnapshot>> found = new LinkedHashMap<>();
        try {
            RequestPlayersEvent request = new RequestPlayersEvent();
            ListenerAdapter.sendListeners(request);
            for (RespondDataEvent response : ListenerAdapter.waitForEvents(request.getEventId(), COLLECT_WINDOW)) {
                if (!(response instanceof RespondPlayersEvent)) continue;
                String server = String.valueOf(response.getSender()).toUpperCase(Locale.ROOT);
                List<PlayerSnapshot> players = found.computeIfAbsent(server, (key) -> new ArrayList<>());
                if (!(response.getData() instanceof List<?> list)) continue;
                for (Object entry : list) {
                    if (entry instanceof PlayerSnapshot snapshot) players.add(snapshot);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // no answers means no overview - the caller shows the servers without their players
        }
        return found;
    }

    /**
     * @param players what {@link #byServer()} returned
     * @param server  the server to look at
     * @return its players, empty when it did not answer
     */
    public static List<PlayerSnapshot> of(Map<String, List<PlayerSnapshot>> players, String server) {
        if (server == null) return List.of();
        List<PlayerSnapshot> found = players.get(server.toUpperCase(Locale.ROOT));
        return found == null ? List.of() : found;
    }

    /**
     * Whether a server reported at all, which is what tells "nobody is on it" from "it did not answer".
     *
     * @param players what {@link #byServer()} returned
     * @param server  the server to look at
     * @return whether that server answered the broadcast
     */
    public static boolean answered(Map<String, List<PlayerSnapshot>> players, String server) {
        return server != null && players.containsKey(server.toUpperCase(Locale.ROOT));
    }
}
