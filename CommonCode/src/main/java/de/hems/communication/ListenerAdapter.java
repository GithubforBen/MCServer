package de.hems.communication;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import org.jgroups.*;
import org.jgroups.util.MessageBatch;

import java.util.*;

public class ListenerAdapter implements Receiver {

    public enum ServerName {
        SURVIVAL,
        LOBBY,
        EVENT,
        HOST;
    }

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
    }

    private final static Multimap<Class<? extends Event>, EventHandler<? extends Event>> listeners = ArrayListMultimap.create();

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
                System.out.println("running listener: " +  k.getClass().toString());
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

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view + " **");
    }

    @Override
    public void receive(Message msg) {
        System.out.println(name + "received message");
        Object object = msg.getObject();
        System.out.println(object);
        if (object instanceof Event) {
            System.out.println(name +"sending event");
            executeListeners((Event) object);
        }
    }

    public ServerName getName() {
        return name;
    }
}
