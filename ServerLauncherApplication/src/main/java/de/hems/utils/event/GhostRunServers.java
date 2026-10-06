package de.hems.utils.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.ClaimGhostRunEvent;
import de.hems.communication.events.event.RespondClaimGhostRunEvent;
import de.hems.communication.events.event.RunUpdatedEvent;
import de.hems.communication.events.server.RequestProxyPlayersEvent;
import de.hems.communication.events.server.RespondProxyPlayersEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;
import de.hems.types.event.EventData;
import de.hems.types.event.RunData;
import de.hems.utils.server.MemoryWatch;
import de.hems.utils.server.ServerHandler;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Keeps a run server ready for the next attempt at a speedrun event: its ghost server.
 * <p>
 * A new run server spends the better part of a minute generating its world, and a reset is exactly the
 * moment nobody wants to wait. So while at least two players are on the run servers of an event, one more
 * server of that event is built in the background with nobody on it. Whoever starts the next run first -
 * by {@code /reset} or from the queue - claims it and is warped over without the wait; everybody after them
 * builds a server of their own as before, and a new ghost is built behind the one that was taken.
 * <p>
 * The claim is decided here and nowhere else, so two teams resetting in the same second can not both get
 * the same world. A ghost nobody needs any more - the event is over, or fewer than two players are left on
 * it for a few minutes - is switched off and thrown away, since it holds a whole server's memory.
 */
public class GhostRunServers {

    /** How many players have to be on the run servers of an event before it gets a ghost. */
    private static final int MIN_PLAYERS = 2;
    /** How often the proxy is asked who is where. */
    private static final long CHECK_INTERVAL_SECONDS = 30L;
    /** How long to wait for the proxy to answer. */
    private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(5);
    /**
     * How long an event has to stay below the player count before its ghost goes. A reset sends a team
     * from one server to the next, and for those seconds nobody is on a run server at all - that must not
     * throw away the ghost the next reset is waiting for.
     */
    private static final long TEARDOWN_GRACE_MS = 3L * 60L * 1000L;
    /** How long a stopping server is given to let go of its files before its directory is removed. */
    private static final long SHUTDOWN_GRACE_SECONDS = 30L;

    private final EventStore events;
    private final RunStore runs;
    private final EventSettlement settlement;
    private final ServerHandler servers;
    /** What the machine has left, or {@code null} to start ghosts without asking. */
    private final MemoryWatch memory;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor((runnable) -> {
                Thread thread = new Thread(runnable, "ghost-run-servers");
                thread.setDaemon(true);
                return thread;
            });

    /** The ghost of each event that has one, by event. Taken out of here the moment it is claimed. */
    private final Map<UUID, String> ghosts = new ConcurrentHashMap<>();
    /** Since when an event with a ghost has been below the player count, by event. */
    private final Map<UUID, Long> unneededSince = new HashMap<>();
    /** Ghosts that were switched off and whose directory still has to go once the process is gone. */
    private final Set<String> leftovers = new LinkedHashSet<>();
    /** Events whose ghost did not fit into memory, so the log says it once and not every half minute. */
    private final Set<UUID> noRoomReported = new HashSet<>();

    public GhostRunServers(EventStore events, RunStore runs, EventSettlement settlement, ServerHandler servers,
                           MemoryWatch memory) {
        this.events = events;
        this.runs = runs;
        this.settlement = settlement;
        this.servers = servers;
        this.memory = memory;
        ListenerAdapter.register(ClaimGhostRunEvent.class, event -> onClaim((ClaimGhostRunEvent) event));
        // ghosts live only in memory, so the ones of the last time the launcher ran are still on the disk
        collectOrphans();
        scheduler.scheduleWithFixedDelay(this::check,
                CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * @param serverName the name of a server
     * @return whether it is a ghost nobody has claimed yet - a server that is empty on purpose
     */
    public boolean isGhost(String serverName) {
        return serverName != null && ghosts.containsValue(serverName);
    }

    /**
     * Hands the ghost of an event to the run that asks first, and writes it into that run.
     * <p>
     * The run is stored here and not only by whoever asked: if the answer gets lost on its way back, the
     * run announced from here still carries the server, and the asking side finds it there.
     *
     * @param request the new run
     */
    private void onClaim(ClaimGhostRunEvent request) throws Exception {
        RunData run = request.getRun();
        String ghost = null;
        if (run != null && run.getId() != null && run.getEventId() != null) {
            synchronized (this) {
                ghost = take(run.getEventId());
                if (ghost != null) {
                    run.setServerName(ghost);
                    if (runs.put(run) == null) {
                        // not a run the store takes - the ghost stays where it was
                        ghosts.put(run.getEventId(), ghost);
                        ghost = null;
                    }
                }
            }
        }
        if (ghost != null) {
            System.out.println("Ghost server " + ghost + " was claimed by run " + run.getId() + ".");
            try {
                ListenerAdapter.sendListeners(new RunUpdatedEvent(run.getId(), run));
            } catch (Exception e) {
                System.out.println("Could not announce the run " + run.getId() + ": " + e.getMessage());
            }
            // the next reset should find a ghost again, not wait for the next round of the check
            scheduler.execute(this::check);
        }
        ListenerAdapter.sendListeners(new RespondClaimGhostRunEvent(request.getSender(), ghost,
                request.getEventId()));
    }

    /**
     * Takes the ghost of an event out of the pool, if there is one that is still running.
     *
     * @param eventId the event
     * @return the ghost, or {@code null}
     */
    private String take(UUID eventId) {
        String ghost = ghosts.get(eventId);
        if (ghost == null) return null;
        ghosts.remove(eventId);
        if (!isUp(ghost)) {
            // it crashed or was switched off by hand - a run must not be sent to a server that is gone
            leftovers.add(ghost);
            return null;
        }
        return ghost;
    }

    /**
     * One round: ask the proxy who is where, then build the ghosts that are missing and throw away the
     * ones nobody needs.
     */
    private void check() {
        try {
            if (!ListenerAdapter.isInitialized()) return;
            Map<String, List<String>> connected = askProxy();
            synchronized (this) {
                discardLeftovers();
                if (connected == null) return;
                long now = System.currentTimeMillis();
                Set<UUID> seen = new HashSet<>();
                for (EventData event : events.getEvents()) {
                    if (!event.getType().isTimed()) continue;
                    seen.add(event.getId());
                    String ghost = ghosts.get(event.getId());
                    if (ghost != null && !isUp(ghost)) {
                        System.out.println("Ghost server " + ghost + " is gone - building another one if needed.");
                        ghosts.remove(event.getId());
                        leftovers.add(ghost);
                        ghost = null;
                    }
                    boolean running = event.isRunning() && !event.isApplied();
                    if (running && playersOn(event, connected) >= MIN_PLAYERS) {
                        unneededSince.remove(event.getId());
                        if (ghost == null) build(event);
                        continue;
                    }
                    if (ghost == null) {
                        unneededSince.remove(event.getId());
                        continue;
                    }
                    Long since = unneededSince.putIfAbsent(event.getId(), now);
                    // an event that is over needs no grace - nobody can start a run on it any more
                    if (running && (since == null || now - since < TEARDOWN_GRACE_MS)) continue;
                    tearDown(event.getId(), running ? "fewer than " + MIN_PLAYERS + " players are left"
                            : "the event is over");
                }
                // the ghost of an event that was deleted outright
                for (UUID eventId : new HashSet<>(ghosts.keySet())) {
                    if (!seen.contains(eventId)) tearDown(eventId, "the event is gone");
                }
                unneededSince.keySet().retainAll(ghosts.keySet());
            }
        } catch (Exception e) {
            System.out.println("The ghost server check failed: " + e.getMessage());
        }
    }

    /**
     * How many players are on the run servers of an event right now, as the proxy sees it.
     *
     * @param event     the event
     * @param connected who is on which server
     * @return the number of players on the servers of its open runs
     */
    private int playersOn(EventData event, Map<String, List<String>> connected) {
        Set<String> serversOfEvent = new HashSet<>();
        for (RunData run : runs.getRunsOf(event.getId())) {
            if (run.isOpen() && run.getServerName() != null) {
                serversOfEvent.add(run.getServerName().toUpperCase(Locale.ROOT));
            }
        }
        Set<String> players = new HashSet<>();
        for (String server : serversOfEvent) {
            List<String> on = connected.get(server);
            if (on != null) players.addAll(on);
        }
        return players.size();
    }

    /**
     * Sets a ghost aside for an event, if the machine has the room for one more server. The server itself
     * is started right after this round, outside the lock: building its directory takes a while, and a
     * claim must not wait for that.
     *
     * @param event the event
     */
    private void build(EventData event) {
        int memoryMB = ServerTemplate.EVENT.getDefaultMemoryMB();
        if (memory != null && !memory.fits(memoryMB)) {
            // a ghost is a convenience - it must never take the room a real run or round needs
            if (noRoomReported.add(event.getId())) {
                System.out.println("No room for a ghost server of " + event.getName() + " - runs start the slow way.");
            }
            return;
        }
        noRoomReported.remove(event.getId());
        String name = freeGhostName(event.getId());
        if (name == null) return;
        // the handler gives a held slot back once the server exists, and without one it would give back
        // somebody else's
        if (memory != null) memory.hold(memoryMB);
        ghosts.put(event.getId(), name);
        UUID eventId = event.getId();
        String eventName = event.getName();
        scheduler.execute(() -> start(eventId, eventName, name, memoryMB));
    }

    /**
     * Starts a ghost that was set aside, unless it was given up on in the meantime.
     *
     * @param eventId   the event
     * @param eventName its name, for the log
     * @param name      the ghost
     * @param memoryMB  its heap
     */
    private void start(UUID eventId, String eventName, String name, int memoryMB) {
        synchronized (this) {
            if (!name.equals(ghosts.get(eventId))) return;
        }
        try {
            servers.startNewInstance(ListenerAdapter.ServerName.valueOf(name), ServerTemplate.EVENT, memoryMB,
                    new FileType.PLUGIN[0]);
            System.out.println("Building ghost server " + name + " for " + eventName + ".");
        } catch (Exception e) {
            synchronized (this) {
                ghosts.remove(eventId, name);
                leftovers.add(name);
            }
            System.out.println("Could not build a ghost server for " + eventName + ": " + e.getMessage());
        }
    }

    /**
     * @param eventId the event
     * @return a ghost name that neither a running server nor a directory on the disk has, or {@code null}
     */
    private String freeGhostName(UUID eventId) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String name = ListenerAdapter.ServerName.normalize(RunData.ghostServerNameFor(eventId));
            if (!isUp(name) && !new File("./servers/" + name + "/").exists()) return name;
        }
        return null;
    }

    /**
     * Switches the ghost of an event off; its directory goes once the process has let go of it.
     *
     * @param eventId the event
     * @param reason  why, for the log
     */
    private void tearDown(UUID eventId, String reason) {
        String ghost = ghosts.remove(eventId);
        unneededSince.remove(eventId);
        if (ghost == null) return;
        System.out.println("Throwing away ghost server " + ghost + " - " + reason + ".");
        try {
            ListenerAdapter.ServerName name = ListenerAdapter.ServerName.valueOf(ghost);
            if (servers.doesInstanceExist(name)) servers.stop(name);
        } catch (RuntimeException e) {
            System.out.println("Could not stop the ghost server " + ghost + ": " + e.getMessage());
        }
        leftovers.add(ghost);
        scheduler.schedule(() -> {
            synchronized (this) {
                discardLeftovers();
            }
        }, SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Removes the directories of ghosts that are switched off. One whose process is still up is tried again
     * on the next round.
     */
    private void discardLeftovers() {
        for (String ghost : new LinkedHashSet<>(leftovers)) {
            if (isGhost(ghost) || isClaimed(ghost)) {
                leftovers.remove(ghost);
                continue;
            }
            if (isUp(ghost)) continue;
            settlement.discardServer(ghost);
            leftovers.remove(ghost);
        }
    }

    /**
     * Finds the ghosts the launcher built the last time it ran. Nothing remembers them across a restart -
     * every server is stopped with the launcher - so whatever ghost directory no run claimed is waste.
     */
    private synchronized void collectOrphans() {
        File[] directories = new File("./servers/").listFiles(File::isDirectory);
        if (directories == null) return;
        for (File directory : directories) {
            String name = directory.getName();
            if (RunData.isGhostServerName(name) && !isClaimed(name)) leftovers.add(name);
        }
        if (!leftovers.isEmpty()) {
            System.out.println(leftovers.size() + " ghost servers of the last run of the launcher will be removed.");
        }
    }

    /**
     * @param serverName the name of a server
     * @return whether a run lives on it, so it is a run's server and no ghost any more
     */
    private boolean isClaimed(String serverName) {
        for (RunData run : runs.getRuns()) {
            if (serverName.equalsIgnoreCase(run.getServerName())) return true;
        }
        return false;
    }

    /**
     * @param serverName the name of a server
     * @return whether it is running
     */
    private boolean isUp(String serverName) {
        try {
            return servers.doesInstanceExist(ListenerAdapter.ServerName.valueOf(serverName));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * @return who is on which server, or {@code null} when the proxy did not answer
     */
    private Map<String, List<String>> askProxy() {
        RespondDataEvent response = ListenerAdapter.ask(new RequestProxyPlayersEvent(), ANSWER_TIMEOUT);
        if (!(response instanceof RespondProxyPlayersEvent players)) return null;
        return players.getPlayersPerServer();
    }
}
