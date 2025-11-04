package de.hems.communication;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.MessageFactory;
import org.jgroups.Receiver;
import org.jgroups.util.MessageBatch;

import java.util.*;

public class ListenerAdapter implements Receiver {

    private static boolean isIniotialized = false;
    private static String name;
    private static JChannel jChannel;

    public ListenerAdapter(String name) throws Exception {
        if (isIniotialized) return;
        name = name;
        isIniotialized = true;
        jChannel = new JChannel();
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
            try {
                k.onEvent(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendListeners(Event event) throws Exception {
        jChannel.send(MessageFactory.create(Message.OBJ_MSG).setObject(event).create().get());
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
