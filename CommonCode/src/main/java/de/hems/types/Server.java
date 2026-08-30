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
    /** How far the server is with starting up, {@code null} when the sender does not track it. */
    public ServerPhase phase;
    /** How much of the current phase is done, {@code 0} when it has no progress to report. */
    public int phasePercent;

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
        this.phase = online ? ServerPhase.READY : ServerPhase.OFFLINE;
    }

    public Server(String name, int port, int memory, ServerTemplate template, FileType.SERVER software,
                  FileType.PLUGIN[] plugins, boolean online, ServerPhase phase, int phasePercent) {
        this(name, port, memory, template, software, plugins, online);
        this.phase = phase;
        this.phasePercent = phasePercent;
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
     * @return how far the server is with starting up, never {@code null}
     */
    public ServerPhase getPhase() {
        if (phase != null) return phase;
        // a sender that does not know about phases only ever reports servers that are up
        return online ? ServerPhase.READY : ServerPhase.OFFLINE;
    }

    public int getPhasePercent() {
        return phasePercent;
    }

    /**
     * @return whether the server is on its way up, so waiting for it is worth it
     */
    public boolean isStartingUp() {
        return getPhase().isStartingUp();
    }

    /**
     * @return what to tell a waiting player, including the progress when there is any
     */
    public String getPhaseDescription() {
        ServerPhase current = getPhase();
        if (current == ServerPhase.GENERATING && phasePercent > 0) {
            return current.getDescription() + " " + phasePercent + "%";
        }
        return current.getDescription();
    }

    /**
     * @return whether players can be warped to this server
     */
    public boolean isJoinable() {
        return online && port > 0 && software != FileType.SERVER.VELOCITY && getPhase().isReady();
    }

    @Override
    public String toString() {
        return name + "(" + port + ", " + memory + "MB)";
    }
}
