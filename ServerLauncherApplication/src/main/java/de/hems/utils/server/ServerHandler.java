package de.hems.utils.server;

import de.hems.FileType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerHandler {
    private List<ServerInstance> instances;

    public ServerHandler() throws Exception {
        instances = new ArrayList<>();
        ServerInstance velocity = new ServerInstance("VELOCITY", 512, FileType.VELOCITY, 25565, true);
        instances.add(velocity);
        //TODO: add velocity.start();
    }

    public void startNewInstance(String name, int allocatedMemoryMB, FileType jarFile, int port, boolean isProxied) throws Exception {
        ServerInstance instance = new ServerInstance(name, allocatedMemoryMB, jarFile, port, isProxied);
        instances.add(instance);
        instance.start();
    }

    public void shutdownNetwork() throws IOException {
        for (ServerInstance instance : instances) {
            instance.stop();
        }
    }
}
