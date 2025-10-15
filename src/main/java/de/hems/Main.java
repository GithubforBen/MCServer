package de.hems;

import de.hems.communication.ListenerAdapter;
import de.hems.events.RespondDataEvent;
import de.hems.utils.Configuration;

public class Main {
    private static Main instance;
    private Configuration configuration;
    private ListenerAdapter listenerAdapter;

    public Main() throws Exception {
        if (instance == null) {
            instance = this;
        } else {
            throw new IllegalStateException("Already initialized");
        }
        configuration = new Configuration();
        listenerAdapter = new ListenerAdapter("ServerLauncher");
        new RespondDataEvent();
    }

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public static Main getInstance() {
        return instance;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ListenerAdapter getListenerAdapter() {
        return listenerAdapter;
    }
}