package de.hems.api;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerRestartEvent;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.hems.communication.events.server.RequestServerStopEvent;
import de.hems.communication.events.server.RequestCapacityEvent;
import de.hems.communication.events.server.RequestServerSlotEvent;
import de.hems.communication.events.server.RequestServersEvent;
import de.hems.communication.events.server.RespondServerSlotEvent;
import de.hems.communication.events.server.RespondServersEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.FileType;
import de.hems.types.Server;
import de.hems.types.ServerTemplate;
import de.hems.types.server.CapacityData;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The programmatic way to run the network: create, start, stop and list servers from anywhere in the
 * cluster without touching events, ports or jars.
 * <p>
 * Creating an event server is a single call:
 * <pre>{@code
 * ServerApi.createEventServer("SOMMERFEST");                       // plain paper event server
 * ServerApi.createServer("BEDWARS_1", ServerTemplate.BEDWARS);     // a bedwars round
 * ServerApi.createServer("KREATIV", ServerTemplate.EVENT, 4096,
 *         List.of(FileType.PLUGIN.WORLDEDIT));                     // with extra plugins
 * }</pre>
 * The host assigns a free port, installs the plugins and announces the server, so players can be warped to
 * it right away. Names are normalised, so {@code "sommerfest"} and {@code "Sommerfest"} are the same server.
 * <p>
 * The listing methods talk to the host and block until it answers - never call them on the main server
 * thread, use {@link #listServersAsync()} there.
 */
public final class ServerApi {

    /** How long the api waits for the host to answer a request. */
    public static final Duration TIMEOUT = Duration.ofSeconds(10);

    private ServerApi() {
    }

    /**
     * Creates and starts a plain event server.
     *
     * @param name the name of the server, e.g. {@code "SOMMERFEST"}
     * @return the name the server was registered under
     */
    public static ListenerAdapter.ServerName createEventServer(String name) throws Exception {
        return createServer(name, ServerTemplate.EVENT, null, null);
    }

    /**
     * Creates and starts a server from a template.
     *
     * @param name     the name of the server
     * @param template the blueprint to use
     * @return the name the server was registered under
     */
    public static ListenerAdapter.ServerName createServer(String name, ServerTemplate template) throws Exception {
        return createServer(name, template, null, null);
    }

    /**
     * Creates and starts a server from a template.
     *
     * @param name         the name of the server
     * @param template     the blueprint to use
     * @param memoryMB     the memory in MB, or {@code null} for the default of the template
     * @param extraPlugins plugins installed on top of the template, may be {@code null}
     * @return the name the server was registered under
     */
    public static ListenerAdapter.ServerName createServer(String name, ServerTemplate template, Integer memoryMB,
                                                          Collection<FileType.PLUGIN> extraPlugins) throws Exception {
        if (template == null) template = ServerTemplate.EVENT;
        ListenerAdapter.ServerName serverName = ListenerAdapter.ServerName.valueOf(name);
        if (serverName.isReserved()) {
            throw new IllegalArgumentException("'" + serverName + "' is reserved and can not be used as a server name");
        }
        ListenerAdapter.sendListeners(new RequestServerStartEvent(
                ListenerAdapter.ServerName.HOST, serverName, template, memoryMB, extraPlugins));
        return serverName;
    }

    /**
     * Starts a server with an explicit plugin selection, bypassing the template defaults.
     *
     * @param name     the name of the server
     * @param software the server software to run
     * @param memoryMB the memory in MB
     * @param plugins  every plugin that should be installed
     * @return the name the server was registered under
     */
    public static ListenerAdapter.ServerName startServer(String name, FileType.SERVER software, int memoryMB,
                                                         Collection<FileType.PLUGIN> plugins) throws Exception {
        ListenerAdapter.ServerName serverName = ListenerAdapter.ServerName.valueOf(name);
        if (serverName.isReserved()) {
            throw new IllegalArgumentException("'" + serverName + "' is reserved and can not be used as a server name");
        }
        List<FileType.PLUGIN> selected = new ArrayList<>();
        if (plugins != null) {
            for (FileType.PLUGIN plugin : plugins) {
                if (plugin != null && plugin.supports(software)) selected.add(plugin);
            }
        }
        ListenerAdapter.sendListeners(new RequestServerStartEvent(
                ListenerAdapter.ServerName.HOST, serverName, software, memoryMB,
                selected.toArray(new FileType.PLUGIN[0]), ServerTemplate.forServerName(serverName.toString())));
        return serverName;
    }

    /**
     * Stops a running server.
     *
     * @param name the name of the server
     */
    public static void stopServer(String name) throws Exception {
        ListenerAdapter.sendListeners(new RequestServerStopEvent(
                ListenerAdapter.ServerName.HOST, ListenerAdapter.ServerName.valueOf(name)));
    }

    /**
     * Restarts a running server with the settings it was started with.
     *
     * @param name the name of the server
     */
    public static void restartServer(String name) throws Exception {
        ListenerAdapter.sendListeners(new RequestServerRestartEvent(
                ListenerAdapter.ServerName.HOST, ListenerAdapter.ServerName.valueOf(name)));
    }

    /**
     * Asks the host which servers are running. Blocks until the answer arrives.
     *
     * @return every running server, empty if the host did not answer in time
     */
    public static Server[] listServers() throws Exception {
        RequestServersEvent request = new RequestServersEvent(ListenerAdapter.ServerName.HOST);
        ListenerAdapter.sendListeners(request);
        RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
        if (!(response instanceof RespondServersEvent)) return new Server[0];
        Server[] servers = ((RespondServersEvent) response).getData();
        for (Server server : servers) {
            // keep the local registry in sync so warping knows the ports of servers created elsewhere
            ListenerAdapter.ServerName.of(server.name, server.port);
        }
        return servers;
    }

    /**
     * Asks the host which servers are running, off the calling thread.
     *
     * @return a future that completes with every running server
     */
    public static CompletableFuture<Server[]> listServersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return listServers();
            } catch (Exception e) {
                throw new IllegalStateException("Could not load the server list", e);
            }
        });
    }

    /**
     * Asks the host which servers players can be warped to.
     *
     * @return every running server that accepts players
     */
    public static List<Server> listJoinableServers() throws Exception {
        List<Server> joinable = new ArrayList<>();
        for (Server server : listServers()) {
            if (server.isJoinable()) joinable.add(server);
        }
        return joinable;
    }

    /**
     * @param name the name of a server
     * @return whether a server with that name is running
     */
    public static boolean isRunning(String name) throws Exception {
        String normalized = ListenerAdapter.ServerName.normalize(name);
        for (Server server : listServers()) {
            if (normalized.equals(server.name)) return true;
        }
        return false;
    }

    /**
     * Builds a name that is not taken yet by appending a counter, e.g. {@code BEDWARS_1}, {@code BEDWARS_2}.
     * Useful to start any number of servers of the same kind.
     *
     * @param prefix the base name
     * @return a free server name
     */
    public static String freeName(String prefix) throws Exception {
        String base = ListenerAdapter.ServerName.normalize(prefix);
        List<String> taken = new ArrayList<>();
        for (Server server : listServers()) taken.add(server.name);
        if (!taken.contains(base)) return base;
        for (int i = 1; i < 1000; i++) {
            String candidate = base + "_" + i;
            if (!taken.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("No free name left for '" + base + "'");
    }

    /**
     * @param template the blueprint of a server
     * @return the plugins that template always installs
     */
    public static Set<FileType.PLUGIN> requiredPlugins(ServerTemplate template) {
        return template.getRequiredPlugins();
    }

    /* ------------------------------------------------------------------ how full the machine is */

    /**
     * Asks the host how much room is left on the machine. Blocks until the answer arrives.
     *
     * @return what the host reports, or {@code null} when it did not answer in time
     */
    public static CapacityData capacity() throws Exception {
        RequestCapacityEvent request = new RequestCapacityEvent();
        ListenerAdapter.sendListeners(request);
        RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
        if (response == null || !(response.getData() instanceof CapacityData capacity)) return null;
        return capacity;
    }

    /**
     * Asks whether one more server of this size may be started, before anything is created.
     * <p>
     * The host answers, not the caller: two players asking in the same second are two questions, and the
     * host sees them one after the other. A refusal is counted there as well, which is what later turns
     * into the memory recommendation in the server manager.
     *
     * @param memoryMB   the heap the new server would want
     * @param playerId   who is asking, may be {@code null}
     * @param playerName their name, for the host's log
     * @param purpose    what the server is for
     * @return the answer, never {@code null} - a host that stays silent counts as a no
     */
    public static Slot requestSlot(int memoryMB, UUID playerId, String playerName, String purpose)
            throws Exception {
        RequestServerSlotEvent request = new RequestServerSlotEvent(memoryMB, playerId, playerName, purpose);
        ListenerAdapter.sendListeners(request);
        RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
        if (!(response instanceof RespondServerSlotEvent slot)) {
            return new Slot(false, null);
        }
        return new Slot(slot.isGranted(),
                response.getData() instanceof CapacityData capacity ? capacity : null);
    }

    /**
     * Whether one more server may be started, and what the machine looked like when that was decided.
     *
     * @param granted  whether it may be started
     * @param capacity the state of the machine, {@code null} when the host did not answer
     */
    public record Slot(boolean granted, CapacityData capacity) {
    }
}
