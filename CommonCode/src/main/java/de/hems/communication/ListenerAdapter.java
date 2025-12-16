package de.hems.communication;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.communication.events.types.EventHandler;
import de.hems.communication.events.types.RespondDataEvent;
import org.jgroups.*;

import java.util.*;

public class ListenerAdapter implements Receiver {

    private final static Multimap<Class<? extends Event>, EventHandler<? extends Event>> listeners = ArrayListMultimap.create();
    private final static List<RespondDataEvent> respondDataEvents = new LinkedList<>();
    private static boolean isInitialized = false;
    private static ServerName name;
    private static JChannel jChannel;

    public ListenerAdapter(ServerName name) throws Exception {
        if (isInitialized) return;
        ListenerAdapter.name = name;
        isInitialized = true;
        jChannel = new JChannel();
        jChannel.setName(name.toString());
        jChannel.setReceiver(this);
        jChannel.connect("MCServer");
        System.out.println("[JGroups] Connected as '" + name + "' to cluster MCServer. View=" + jChannel.getView());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> jChannel.close()));
    }

    public static <T extends Event> void register(Class<T> eventType, EventHandler<T> listener) {
        listeners.put(eventType, listener);
    }

    public static void executeListeners(Event event) {
        Collection<EventHandler<? extends Event>> eventHandlers = listeners.get(event.getClass());
        System.out.println(eventHandlers.size() + ":" + Arrays.toString(eventHandlers.toArray()) + ":" + event.getClass().toString() + ":" + RequestServerStartEvent.class);
        if (eventHandlers.isEmpty()) {
            return;
        }
        eventHandlers.forEach((k) -> {
            try {
                System.out.println("running listener: " + k.getClass().toString());
                k.onEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendListeners(Event event) throws Exception {
        Message message = new ObjectMessage(null, event);
        System.out.println(message.getObject().toString());
        jChannel.send(message);
    }

    public static RespondDataEvent waitForEvent(UUID requestId) throws InterruptedException {
        while (respondDataEvents.isEmpty()) {
            Thread.sleep(100);
        }
        RespondDataEvent event = respondDataEvents.stream().filter(e -> e.getRequestId().equals(requestId)).findFirst().orElse(null);
        if (event == null) {
            return waitForEvent(requestId);
        }
        respondDataEvents.remove(event);
        return event;
    }

    public static ServerName getName() {
        return name;
    }

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view + " **");
    }

    @Override
    public void receive(Message msg) {
        System.out.println(name + " received message");
        Object object = msg.getObject();
        System.out.println(object);
        if (object instanceof Event) {
            EventFoundationData event = (EventFoundationData) object;
            if (event.getReceiver() != ServerName.ALL && (event.getReceiver() == null || !event.getReceiver().equals(name))) {
                System.out.println(name + " Event not for me");
                return;
            }
            if (event instanceof RespondDataEvent) {
                respondDataEvents.add((RespondDataEvent) event);
            }
            executeListeners((Event) object);
        }
    }

    public enum ServerName {
        LOBBY(3001),
        ALL(-1),
        SURVIVAL(3000),
        EVENT(3002),
        HOST(-1),
        VELOCITY(-1);//you can't connect from velocity to velocity

        private final int port;

        ServerName(int i) {
            port = i;
        }

        public int getPort() {
            return port;
        }
    }
}
