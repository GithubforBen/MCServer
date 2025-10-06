package de.hems;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.blocks.cs.ReceiverAdapter;

import java.util.ArrayList;
import java.util.List;

public class ListenerAdapter extends ReceiverAdapter {

    private static boolean isIniotialized = false;

    public ListenerAdapter() throws Exception {
        if (isIniotialized) return;
        isIniotialized = true;
        JChannel jChannel = new JChannel();
        jChannel.connect("MCServer");
        jChannel.setReceiver((Receiver) this);
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
}
