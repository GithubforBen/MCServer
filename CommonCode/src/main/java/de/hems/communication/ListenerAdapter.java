package de.hems.communication;

import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.communication.events.types.EventHandler;
import de.hems.communication.events.types.RespondDataEvent;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ObjectMessage;
import org.jgroups.Receiver;
import org.jgroups.View;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListenerAdapter implements Receiver {

    private final static Map<Class<? extends Event>, List<EventHandler<? extends Event>>> listeners = new ConcurrentHashMap<>();
    private final static List<RespondDataEvent> respondDataEvents = Collections.synchronizedList(new LinkedList<>());
    private static boolean isInitialized = false;
    private static ServerName name;
    private static JChannel jChannel;
    private static Thread shutdownHook;

    public ListenerAdapter(ServerName name) throws Exception {
        if (isInitialized) return;
        ListenerAdapter.name = name;
        isInitialized = true;
        jChannel = new JChannel();
        jChannel.setName(name.toString());
        jChannel.setReceiver(this);
        jChannel.connect("MCServer");
        System.out.println("[JGroups] Connected as '" + name + "' to cluster MCServer. View=" + jChannel.getView());
        shutdownHook = new Thread(ListenerAdapter::disconnect, "jgroups-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Leaves the cluster.
     * <p>
     * Every plugin that opened a connection calls this while it is being disabled, and that is the point:
     * closing the channel from a jvm shutdown hook is too late on a paper server. By then the plugin's
     * class loader is closed, and jgroups - which loads a protocol class on the way out - dies with
     * "The plugin classloader for X has thrown a zip file error" on a perfectly healthy shutdown. Doing it
     * during onDisable happens while the jar is still open, and the hook that is left behind is only there
     * for a jvm that goes down without one.
     */
    public static synchronized void disconnect() {
        JChannel closing = jChannel;
        if (closing == null) return;
        jChannel = null;
        isInitialized = false;
        try {
            closing.close();
        } catch (Throwable ignored) {
            // going down anyway: there is nobody left to tell
        }
        Thread hook = shutdownHook;
        shutdownHook = null;
        if (hook != null && hook != Thread.currentThread()) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException ignored) {
                // the jvm is already on its way down, so the hook is running or gone
            }
        }
    }

    public static <T extends Event> void register(Class<T> eventType, EventHandler<T> listener) {
        listeners.computeIfAbsent(eventType, (k) -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static void executeListeners(Event event) {
        List<EventHandler<? extends Event>> eventHandlers = listeners.get(event.getClass());
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            return;
        }
        for (EventHandler<? extends Event> handler : eventHandlers) {
            try {
                handler.onEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendListeners(Event event) throws Exception {
        if (jChannel == null) {
            throw new IllegalStateException("The ListenerAdapter has not been initialized yet.");
        }
        jChannel.send(new ObjectMessage(null, event));
    }

    /**
     * Waits until the response belonging to the given request arrives.
     * <p>
     * This blocks the calling thread, so it must never be called from the main server thread.
     * Use {@link #waitForEvent(UUID, Duration)} to bound the waiting time.
     *
     * @param requestId the id of the request the response belongs to
     * @return the response that was sent for the given request
     */
    public static RespondDataEvent waitForEvent(UUID requestId) throws InterruptedException {
        return waitForEvent(requestId, Duration.ofSeconds(10));
    }

    /**
     * Waits at most {@code timeout} for the response belonging to the given request.
     *
     * @param requestId the id of the request the response belongs to
     * @param timeout   how long the caller is willing to wait
     * @return the response, or {@code null} if none arrived in time
     */
    public static RespondDataEvent waitForEvent(UUID requestId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            synchronized (respondDataEvents) {
                for (RespondDataEvent event : respondDataEvents) {
                    if (requestId.equals(event.getRequestId())) {
                        respondDataEvents.remove(event);
                        return event;
                    }
                }
            }
            Thread.sleep(25);
        }
        return null;
    }

    /**
     * Collects every response to a broadcast request.
     * <p>
     * {@link #waitForEvent(UUID)} returns the first answer, which is all a request to a single node needs.
     * A request that went to the whole network gets one answer per node, and the caller usually wants them
     * all - or wants to stop as soon as one of them is the useful one.
     * <p>
     * Blocks the calling thread, so it must never be called from the main server thread.
     *
     * @param requestId the id of the request the responses belong to
     * @param window    how long to keep collecting
     * @param stopEarly checked on every response as it arrives; when it holds, collecting ends right away
     * @return the responses that arrived, in the order they came in
     */
    public static List<RespondDataEvent> waitForEvents(UUID requestId, Duration window,
                                                       Predicate<RespondDataEvent> stopEarly)
            throws InterruptedException {
        List<RespondDataEvent> collected = new ArrayList<>();
        long deadline = System.currentTimeMillis() + window.toMillis();
        while (System.currentTimeMillis() < deadline) {
            boolean done = false;
            synchronized (respondDataEvents) {
                Iterator<RespondDataEvent> iterator = respondDataEvents.iterator();
                while (iterator.hasNext()) {
                    RespondDataEvent event = iterator.next();
                    if (!requestId.equals(event.getRequestId())) continue;
                    iterator.remove();
                    collected.add(event);
                    if (stopEarly != null && stopEarly.test(event)) {
                        done = true;
                        break;
                    }
                }
            }
            if (done) return collected;
            Thread.sleep(25);
        }
        return collected;
    }

    /**
     * @param requestId the id of the request the responses belong to
     * @param window    how long to keep collecting
     * @return every response that arrived inside the window
     */
    public static List<RespondDataEvent> waitForEvents(UUID requestId, Duration window)
            throws InterruptedException {
        return waitForEvents(requestId, window, null);
    }

    public static ServerName getName() {
        return name;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view + " **");
    }

    @Override
    public void receive(Message msg) {
        Object object = msg.getObject();
        if (object instanceof Event) {
            EventFoundationData event = (EventFoundationData) object;
            if (!ServerName.ALL.equals(event.getReceiver())
                    && (event.getReceiver() == null || !event.getReceiver().equals(name))) {
                return;
            }
            if (event instanceof RespondDataEvent) {
                respondDataEvents.add((RespondDataEvent) event);
                // responses pile up if nobody ever collects them (e.g. the requester died meanwhile)
                if (respondDataEvents.size() > 256) respondDataEvents.remove(0);
            }
            executeListeners((Event) object);
        }
    }

    /**
     * The identity of a node inside the network.
     * <p>
     * This used to be an enum, which capped the network at a handful of hard coded servers. It is now a
     * registry backed value type so that an unlimited amount of servers can join: every name that is used
     * anywhere - be it through the in-game UI, the {@link de.hems.api.ServerApi} or an incoming event - is
     * registered on the fly. The well known names stay available as constants so existing code keeps working.
     * <p>
     * Instances are canonical: for one name there is exactly one object per JVM, also after deserialization,
     * which means identity comparisons behave just like they did with the enum.
     */
    public static final class ServerName implements Serializable, Comparable<ServerName> {
        private static final long serialVersionUID = 200L;

        /** Port used by names that do not belong to a joinable minecraft server. */
        public static final int NO_PORT = -1;
        /** First port handed out to dynamically created servers. */
        public static final int DYNAMIC_PORT_START = 3100;
        /** Last port handed out to dynamically created servers. */
        public static final int DYNAMIC_PORT_END = 3999;

        private static final Map<String, ServerName> REGISTRY = new ConcurrentHashMap<>();

        /** Broadcast target: every node in the network. */
        public static final ServerName ALL = register("ALL", NO_PORT);
        /** The server launcher itself. */
        public static final ServerName HOST = register("HOST", NO_PORT);
        /** The proxy. You can't connect from velocity to velocity. */
        public static final ServerName VELOCITY = register("VELOCITY", NO_PORT);
        public static final ServerName SURVIVAL = register("SURVIVAL", 3000);
        public static final ServerName LOBBY = register("LOBBY", 3001);
        public static final ServerName EVENT = register("EVENT", 3002);

        private final String name;
        private volatile int port;

        private ServerName(String name, int port) {
            this.name = name;
            this.port = port;
        }

        private static ServerName register(String name, int port) {
            ServerName serverName = new ServerName(name, port);
            REGISTRY.put(name, serverName);
            return serverName;
        }

        /**
         * Normalises user input into a name that is safe to use as a directory, tmux session and
         * velocity server entry.
         *
         * @param raw the name as it was typed
         * @return the normalised name
         */
        public static String normalize(String raw) {
            if (raw == null) throw new IllegalArgumentException("A server name is required");
            String normalized = raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_");
            while (normalized.startsWith("_")) normalized = normalized.substring(1);
            while (normalized.endsWith("_")) normalized = normalized.substring(0, normalized.length() - 1);
            if (normalized.isEmpty()) throw new IllegalArgumentException("'" + raw + "' is not a usable server name");
            if (normalized.length() > 32) normalized = normalized.substring(0, 32);
            return normalized;
        }

        /**
         * Looks the name up, registering it if it is unknown so far. Never returns {@code null}, which is
         * what makes an unlimited amount of servers possible.
         *
         * @param name the name to look up
         * @return the canonical instance for that name
         */
        public static ServerName valueOf(String name) {
            String normalized = normalize(name);
            return REGISTRY.computeIfAbsent(normalized, (key) -> new ServerName(key, NO_PORT));
        }

        /**
         * Looks the name up and remembers the given port for it.
         *
         * @param name the name to look up
         * @param port the port the server listens on, or {@link #NO_PORT} if it has none
         * @return the canonical instance for that name
         */
        public static ServerName of(String name, int port) {
            ServerName serverName = valueOf(name);
            if (port != NO_PORT) serverName.port = port;
            return serverName;
        }

        /**
         * @param name the name to check
         * @return whether that name is known already
         */
        public static boolean isRegistered(String name) {
            try {
                return REGISTRY.containsKey(normalize(name));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        /**
         * @return every known name, including the ones that are not joinable servers
         */
        public static ServerName[] values() {
            List<ServerName> values = new ArrayList<>(REGISTRY.values());
            Collections.sort(values);
            return values.toArray(new ServerName[0]);
        }

        /**
         * @return every known name that belongs to a joinable minecraft server
         */
        public static List<ServerName> servers() {
            List<ServerName> values = new ArrayList<>();
            for (ServerName serverName : REGISTRY.values()) {
                if (serverName.isJoinable()) values.add(serverName);
            }
            Collections.sort(values);
            return values;
        }

        /**
         * Reserves the lowest free port of the dynamic range for this name.
         *
         * @return the port that was assigned
         */
        public int assignPort() {
            if (port != NO_PORT) return port;
            Collection<ServerName> known = REGISTRY.values();
            for (int candidate = DYNAMIC_PORT_START; candidate <= DYNAMIC_PORT_END; candidate++) {
                boolean taken = false;
                for (ServerName serverName : known) {
                    if (serverName.port == candidate) {
                        taken = true;
                        break;
                    }
                }
                if (!taken) {
                    port = candidate;
                    return port;
                }
            }
            throw new IllegalStateException("No free port left between "
                    + DYNAMIC_PORT_START + " and " + DYNAMIC_PORT_END);
        }

        public String name() {
            return name;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        /**
         * @return whether players can be sent to this server
         */
        public boolean isJoinable() {
            return port != NO_PORT;
        }

        /**
         * @return whether the name is reserved for the infrastructure and can not be used for a server
         */
        public boolean isReserved() {
            return this == ALL || this == HOST || this == VELOCITY;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ServerName)) return false;
            return name.equals(((ServerName) o).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public int compareTo(ServerName other) {
            return name.compareTo(other.name);
        }

        /**
         * Keeps one instance per name after deserialization and adopts the port the sender knew about.
         */
        private Object readResolve() {
            ServerName existing = REGISTRY.putIfAbsent(name, this);
            if (existing == null) return this;
            if (port != NO_PORT) existing.port = port;
            return existing;
        }
    }
}
