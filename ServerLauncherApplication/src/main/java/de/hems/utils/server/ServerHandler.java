package de.hems.utils.server;

import de.hems.FileType;

import java.io.IOException;
import java.time.Duration;
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

    public void stop(String name) {
        instances.stream().filter(ServerInstance -> ServerInstance.getName().equals(name)).findFirst().ifPresent(ServerInstance -> {
            try {
                ServerInstance.stop();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        new Thread(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(10L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            instances.stream().filter(ServerInstance -> ServerInstance.getName().equals(name)).findFirst().ifPresent(ServerInstance -> {
                if (!ServerInstance.isAlive()) {
                    instances.remove(ServerInstance);
                }
            });
            try {
                Thread.sleep(Duration.ofSeconds(10L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            instances.stream().filter(ServerInstance -> ServerInstance.getName().equals(name)).findFirst().ifPresent(ServerInstance -> {
                if (ServerInstance.isAlive()) {
                    ServerInstance.kill();
                    instances.remove(ServerInstance);
                }
            });
        }).start();
    }
    public void shutdownNetwork() throws IOException {
        for (ServerInstance instance : instances) {
            instance.stop();
        }
    }


}
