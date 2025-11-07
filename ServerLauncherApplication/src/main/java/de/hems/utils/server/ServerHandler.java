package de.hems.utils.server;

import de.hems.types.FileType;
import de.hems.types.Server;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerHandler {
    private List<ServerInstance> instances;

    public ServerHandler() throws Exception {
        instances = new ArrayList<>();
        //TODO: add velocity.start();
    }

    public void startNewInstance(String name, int allocatedMemoryMB, FileType.SERVER jarFile, int port, boolean isProxied, FileType.PLUGIN[] plugins) throws Exception {
        ServerInstance instance = new ServerInstance(name, allocatedMemoryMB, jarFile, port, isProxied, plugins);
        instances.add(instance);
        instance.start();
    }

    public ServerInstance stop(String name) {
        Optional<ServerInstance> first = instances.stream().filter(ServerInstance -> ServerInstance.getName().equals(name)).findFirst();
        if (!first.isPresent()) {
            throw new RuntimeException("Server with name " + name + " not found");
        }
        first.ifPresent(ServerInstance -> {
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
            first.ifPresent(ServerInstance -> {
                if (!ServerInstance.isAlive()) {
                    instances.remove(ServerInstance);
                }
            });
            try {
                Thread.sleep(Duration.ofSeconds(10L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            first.ifPresent(ServerInstance -> {
                if (ServerInstance.isAlive()) {
                    ServerInstance.kill();
                    instances.remove(ServerInstance);
                }
            });
        }).start();
        return first.get();
    }
    public void shutdownNetwork() throws IOException {
        for (ServerInstance instance : instances) {
            instance.stop();
        }
    }

    public boolean doesInstanceExist(String name) {
        return instances.stream().anyMatch(ServerInstance -> ServerInstance.getName().equals(name));
    }
    public Server[] collectToServer() {
        List<Server> servers = new ArrayList<>();
        for (ServerInstance instance : instances) {
            servers.add(new Server(instance.getName(), instance.getPort(), instance.getAllocatedMemoryMB()));
        }
        return servers.toArray(new Server[0]);
    }
}
