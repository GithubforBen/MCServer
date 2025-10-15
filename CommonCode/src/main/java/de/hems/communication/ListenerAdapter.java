package de.hems.communication;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.blocks.cs.ReceiverAdapter;
import org.jgroups.util.MessageBatch;

import java.util.ArrayList;
import java.util.List;

public class ListenerAdapter implements Receiver {

    private static boolean isIniotialized = false;

    public ListenerAdapter() throws Exception {
        if (isIniotialized) return;
        isIniotialized = true;
        JChannel jChannel = new JChannel();
        jChannel.setReceiver(this);
        jChannel.connect("MCServer");
    }



    private static List<CommunicationListener<Event>> listeners = new ArrayList<>();
    public static void register(CommunicationListener<Event> listener) {
        listeners.add(listener);
    }
    public static void excecuteListeners(Event event) {
        for (CommunicationListener<Event> listener : listeners) {
            if (!listener.checkE(event)) continue;
            listener.onEvent(event);
        }
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
}
