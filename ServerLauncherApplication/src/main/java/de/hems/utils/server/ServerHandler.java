package de.hems.utils.server;

import de.hems.FileType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerHandler {
    private List<ServerInstance> instances;

    public ServerHandler() throws IOException {
        instances = new ArrayList<>();
        ServerInstance velocity = new ServerInstance("VELOCITY", 512, FileType.VELOCITY);
        instances.add(velocity);
        velocity.start();
    }

    public void startNewInstance(String name, int allocatedMemoryMB, FileType jarFile) throws IOException {
        ServerInstance instance = new ServerInstance(name, allocatedMemoryMB, jarFile);
        instances.add(instance);
        instance.start();
    }

    public void shutdownNetwork() throws IOException {
        for (ServerInstance instance : instances) {
            instance.stop();
        }
    }
}
