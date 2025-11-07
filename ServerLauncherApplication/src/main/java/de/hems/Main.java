package de.hems;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.money.RequestPlayerMoneyEvent;
import de.hems.events.RespondDataEvent;
import de.hems.events.RestartServerEvent;
import de.hems.events.StartServerEvent;
import de.hems.utils.Configuration;
import de.hems.utils.server.ServerHandler;
import de.hems.utils.types.RunningMode;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.UUID;

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
            } catch (IOException ignored) {
            }
        }));
        configuration = new Configuration();
        listenerAdapter = new ListenerAdapter(ListenerAdapter.ServerName.HOST);
        new RespondDataEvent();
        serverHandler = new ServerHandler();
        new StartServerEvent();
        new RestartServerEvent();
        serverHandler.startNewInstance(
                ListenerAdapter.ServerName.SURVIVAL.toString(), 4000, FileType.PAPER, 25565, false);
        new Thread(() -> {
            try {
                Thread.sleep(1000 * 40);
                ListenerAdapter.sendListeners(new RequestPlayerMoneyEvent(ListenerAdapter.ServerName.HOST, ListenerAdapter.ServerName.SURVIVAL, UUID.randomUUID()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public static Main getInstance() {
        return instance;
    }

    public void onShutdown() throws IOException {
        serverHandler.shutdownNetwork();
        configuration.save();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ListenerAdapter getListenerAdapter() {
        return listenerAdapter;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public String getIp() throws IOException {
        YamlConfiguration config = configuration.getConfig();
        RunningMode mode = RunningMode.LOCAL;
        if (config.contains("running-mode")) {
            try {
                mode = RunningMode.valueOf(config.getString("running-mode"));
            } catch (Exception e) {
                config.set("running-mode", RunningMode.LOCAL.toString());
            }
        } else {
            config.set("running-mode", RunningMode.LOCAL.toString());
        }
        switch (mode) {
            case LOCAL -> {
                return "localhost";
            }
            case LOCALIP -> {
                return InetAddress.getLocalHost().getHostAddress();
            }
            case PUBLIC -> {
                URL ip = new URL("http://checkip.amazonaws.com");
                BufferedReader reader = new BufferedReader(new InputStreamReader(ip.openStream()));
                return reader.readLine();
            }
        }
        throw new IllegalStateException("Unknown running mode");
    }
}