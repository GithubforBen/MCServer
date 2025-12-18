package de.hems.utils.server;

import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.Server;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class ServerHandler {
    private List<ServerInstance> instances;

    public ServerHandler() throws Exception {
        instances = new ArrayList<>();
        startNewInstance(ListenerAdapter.ServerName.VELOCITY, 1000, FileType.SERVER.VELOCITY,  new FileType.PLUGIN[]{});
    }

    public void startNewInstance(ListenerAdapter.ServerName name, int allocatedMemoryMB, FileType.SERVER jarFile, FileType.PLUGIN[] plugins) throws Exception {
        Set<FileType.PLUGIN> pluginList = new HashSet<>(Arrays.asList(plugins));
        pluginList.addAll(List.of(FileType.PLUGIN.CORE_PROTECT, FileType.PLUGIN.WORLDEDIT, FileType.PLUGIN.CHUNKY, FileType.PLUGIN.WORLD_GUARD));
        switch (name) {
            case SURVIVAL -> {
                pluginList.add(FileType.PLUGIN.SURVIVAL);
                break;
            }
            case LOBBY -> {
                pluginList.add(FileType.PLUGIN.LOBBY);
                break;
            }
            case EVENT -> {
                pluginList.add(FileType.PLUGIN.BEDWARS);
                break;
            }
            case VELOCITY -> {
                pluginList.clear();
                //TODO: fix pluginList.add(FileType.PLUGIN.VELOCITY);
                pluginList.add(FileType.PLUGIN.SIMPLE_VOICECHAT_VELOCITY);
                break;
            }
        }
        plugins = pluginList.toArray(new FileType.PLUGIN[0]);
        System.out.println(plugins.length + " plugins will be installed.");
        ServerInstance instance = new ServerInstance(name, allocatedMemoryMB, jarFile, plugins);
        instances.add(instance);
        instance.start();
    }

    public ServerInstance stop(ListenerAdapter.ServerName name) {
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
        updateInstances();
        for (ServerInstance instance : instances) {
            instance.stop();
        }
    }

    public boolean doesInstanceExist(ListenerAdapter.ServerName name) {
        updateInstances();
        return instances.stream().anyMatch(ServerInstance -> ServerInstance.getName().equals(name));
    }

    public void updateInstances() {
        instances.removeIf(instance -> !instance.isAlive());
    }

    public Server[] collectToServer() {
        updateInstances();
        List<Server> servers = new ArrayList<>();
        for (ServerInstance instance : instances) {
            servers.add(new Server(instance.getName().toString(), instance.getName().getPort(), instance.getAllocatedMemoryMB()));
        }
        return servers.toArray(new Server[0]);
    }
}
