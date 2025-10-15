package de.hems.communication;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.hems.communication.events.Event;
import de.hems.communication.events.EventHandler;
import de.hems.communication.events.RequestDataFromConfigEvent;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.util.MessageBatch;

import java.util.*;

public class ListenerAdapter implements Receiver {

    private static boolean isIniotialized = false;
    private String name;

    public ListenerAdapter(String name) throws Exception {
        if (isIniotialized) return;
        this.name = name;
        isIniotialized = true;
        JChannel jChannel = new JChannel();
        jChannel.setReceiver(this);
        jChannel.connect("MCServer");
    }

    private final static Multimap<Class<? extends Event>, EventHandler<? extends Event>> listeners = ArrayListMultimap.create();

    public static <T extends Event> void register(Class<T> eventType, EventHandler<T> listener) {
        listeners.put(eventType, listener);
    }

    public static void excecuteListeners(Event event) {
        Collection<EventHandler<? extends Event>> eventHandlers = listeners.get(event.getClass());
        if (eventHandlers.isEmpty()) {
            return;
        }
        eventHandlers.forEach((k) -> {
            k.onEvent(event);
        });
    }

    public void receive(Message msg) {
        Object object = msg.getObject();
        if (object instanceof Event) {
            excecuteListeners((Event) object);
        }
    }

    @Override
    public void receive(MessageBatch batch) {
        Receiver.super.receive(batch);
        batch.forEach(this::receive);
    }

    public String getName() {
        return name;
    }
}
