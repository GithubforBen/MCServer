package de.hems.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A snapshot of one running server, as it is sent to the other nodes of the network.
 */
public class Server implements Serializable {
    private static final long serialVersionUID = 1930L;
    public String name;
    public int port;
    public int memory;
    public ServerTemplate template;
    public FileType.SERVER software;
    public FileType.PLUGIN[] plugins;
    public boolean online;

    public Server(String name, int port, int memory) {
        this(name, port, memory, ServerTemplate.forServerName(name), FileType.SERVER.PAPER, new FileType.PLUGIN[0], true);
    }

    public Server(String name, int port, int memory, ServerTemplate template, FileType.SERVER software,
                  FileType.PLUGIN[] plugins, boolean online) {
        this.name = name;
        this.port = port;
        this.memory = memory;
        this.template = template;
        this.software = software;
        this.plugins = plugins == null ? new FileType.PLUGIN[0] : plugins;
        this.online = online;
    }

    public Server() {

    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public int getMemory() {
        return memory;
    }

    public ServerTemplate getTemplate() {
        return template;
    }

    public FileType.SERVER getSoftware() {
        return software;
    }

    public List<FileType.PLUGIN> getPlugins() {
        return plugins == null ? new ArrayList<>() : Arrays.asList(plugins);
    }

    public boolean isOnline() {
        return online;
    }

    /**
     * @return whether players can be warped to this server
     */
    public boolean isJoinable() {
        return online && port > 0 && software != FileType.SERVER.VELOCITY;
    }

    @Override
    public String toString() {
        return name + "(" + port + ", " + memory + "MB)";
    }
}
