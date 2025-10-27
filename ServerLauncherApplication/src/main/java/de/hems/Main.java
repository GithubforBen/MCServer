package de.hems;

import de.hems.communication.ListenerAdapter;
import de.hems.events.RespondDataEvent;
import de.hems.utils.Configuration;
import de.hems.utils.server.ServerHandler;

import java.io.IOException;

public class Main {
    private static Main instance;
    private Configuration configuration;
    private ListenerAdapter listenerAdapter;
    private ServerHandler serverHandler;

    public Main() throws Exception {
        if (instance == null) {
            instance = this;
        } else {
            throw new IllegalStateException("Already initialized");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                onShutdown();
            } catch (IOException ignored){}
        }));
        configuration = new Configuration();
        listenerAdapter = new ListenerAdapter("ServerLauncher");
        new RespondDataEvent();
        serverHandler = new ServerHandler();
        serverHandler.startNewInstance("Paper", 4000, FileType.PAPER);
    }

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public void onShutdown() throws IOException {
        serverHandler.shutdownNetwork();
        configuration.save();
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