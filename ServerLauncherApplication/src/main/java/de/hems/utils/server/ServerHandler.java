package de.hems.utils.server;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.ServerRegisteredEvent;
import de.hems.communication.events.server.ServerUnregisteredEvent;
import de.hems.types.FileType;
import de.hems.types.Server;
import de.hems.types.ServerTemplate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Runs the servers of the network.
 * <p>
 * There is no fixed list of servers anymore: any name can be started, a free port is assigned and
 * remembered, and the rest of the network is told about it. That means an unlimited number of servers - for
 * example one per event - can run next to each other.
 */
public class ServerHandler {

    /** Where the ports and settings of every known server are stored. */
    private static final String CONFIG_ROOT = "servers";

    private final List<ServerInstance> instances = new CopyOnWriteArrayList<>();
    /** Set once the launcher has one, so a granted slot is given back when its server starts. */
    private MemoryWatch memoryWatch;

    public ServerHandler() throws Exception {
        loadKnownServers();
        startNewInstance(ListenerAdapter.ServerName.VELOCITY, ServerTemplate.PROXY, null, new FileType.PLUGIN[0]);
    }

    /**
     * Reads the servers that were created in earlier runs, so their ports stay stable and the proxy keeps
     * offering them.
     */
    private void loadKnownServers() {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        ConfigurationSection section = config.getConfigurationSection(CONFIG_ROOT);
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            int port = section.getInt(name + ".port", ListenerAdapter.ServerName.NO_PORT);
            ListenerAdapter.ServerName.of(name, port);
        }
    }

    /**
     * Remembers a server so its port survives a restart of the launcher.
     *
     * @param name     the server
     * @param memoryMB the memory it was started with
     * @param software the software it runs
     * @param template the blueprint it was created from
     * @param plugins  the plugins that were installed
     */
    private void rememberServer(ListenerAdapter.ServerName name, int memoryMB, FileType.SERVER software,
                                ServerTemplate template, FileType.PLUGIN[] plugins) {
        if (name.isReserved()) return;
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        String path = CONFIG_ROOT + "." + name;
        config.set(path + ".port", name.getPort());
        config.set(path + ".memory", memoryMB);
        config.set(path + ".software", software.toString());
        config.set(path + ".template", template.name());
        List<String> pluginNames = new ArrayList<>();
        for (FileType.PLUGIN plugin : plugins) pluginNames.add(plugin.name());
        config.set(path + ".plugins", pluginNames);
        Main.getInstance().getConfiguration().save();
    }

    /**
     * Makes sure the server has a port. Names that were never used before get the lowest free port of the
     * dynamic range, which is what allows creating servers on the fly.
     *
     * @param name the server
     */
    private void assignPortIfNeeded(ListenerAdapter.ServerName name) {
        if (name.isJoinable() || name.isReserved()) return;
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        int stored = config.getInt(CONFIG_ROOT + "." + name + ".port", ListenerAdapter.ServerName.NO_PORT);
        if (stored != ListenerAdapter.ServerName.NO_PORT) {
            name.setPort(stored);
            return;
        }
        name.assignPort();
        System.out.println("Assigned port " + name.getPort() + " to the new server " + name);
    }

    /**
     * Starts a server from a template - everything an automatically created event server needs.
     *
     * @param name         the name of the server
     * @param template     the blueprint to use
     * @param memoryMB     the memory in MB, or {@code null} for the default of the template
     * @param extraPlugins plugins installed on top of the template
     */
    public void startNewInstance(ListenerAdapter.ServerName name, ServerTemplate template, Integer memoryMB,
                                 FileType.PLUGIN[] extraPlugins) throws Exception {
        Set<FileType.PLUGIN> plugins = template.resolvePlugins(extraPlugins);
        startNewInstance(name, memoryMB == null ? template.getDefaultMemoryMB() : memoryMB,
                template.getSoftware(), plugins.toArray(new FileType.PLUGIN[0]), template);
    }

    public void startNewInstance(ListenerAdapter.ServerName name, int allocatedMemoryMB, FileType.SERVER jarFile,
                                 FileType.PLUGIN[] plugins) throws Exception {
        startNewInstance(name, allocatedMemoryMB, jarFile, plugins, ServerTemplate.forServerName(name.toString()));
    }

    /**
     * Starts a server. The name does not have to be known before - unknown names are registered, get a port
     * and are announced to the network.
     *
     * @param name              the name of the server
     * @param allocatedMemoryMB the memory in MB
     * @param jarFile           the server software
     * @param plugins           the plugins to install
     * @param template          the blueprint the server belongs to
     */
    public void startNewInstance(ListenerAdapter.ServerName name, int allocatedMemoryMB, FileType.SERVER jarFile,
                                 FileType.PLUGIN[] plugins, ServerTemplate template) throws Exception {
        if (name.isReserved() && name != ListenerAdapter.ServerName.VELOCITY) {
            throw new IllegalArgumentException("'" + name + "' is reserved and can not be used as a server name");
        }
        updateInstances();
        if (doesInstanceExist(name)) {
            System.out.println("Server " + name + " is already running - ignoring the start request.");
            return;
        }
        assignPortIfNeeded(name);

        Set<FileType.PLUGIN> pluginList = new LinkedHashSet<>();
        for (FileType.PLUGIN plugin : template.resolvePlugins(plugins)) {
            if (plugin.supports(jarFile)) pluginList.add(plugin);
        }
        FileType.PLUGIN[] resolved = pluginList.toArray(new FileType.PLUGIN[0]);
        System.out.println(resolved.length + " plugins will be installed on " + name + ": " + Arrays.toString(resolved));

        // the machine has the last word on the heap, so a small box does not start four servers that
        // together promise more memory than it has
        int memory = MemoryLimits.apply(name, allocatedMemoryMB);

        ServerInstance instance = new ServerInstance(name, memory, jarFile, resolved, template);
        instances.add(instance);
        // the slot this server was granted before it existed is now the server itself
        if (memoryWatch != null) memoryWatch.release(memory);
        instance.start();
        rememberServer(name, memory, jarFile, template, resolved);
        announceRegistered(instance);
    }

    /**
     * Tells the rest of the network about a server, so the proxy registers it and players can warp to it
     * without anything being restarted.
     *
     * @param instance the server that was started
     */
    private void announceRegistered(ServerInstance instance) {
        if (instance.getName().isReserved()) return;
        try {
            ListenerAdapter.sendListeners(new ServerRegisteredEvent(
                    ListenerAdapter.ServerName.ALL, instance.toServer(true)));
        } catch (Exception e) {
            System.out.println("Could not announce " + instance.getName() + ": " + e.getMessage());
        }
    }

    private void announceUnregistered(ListenerAdapter.ServerName name) {
        if (name.isReserved()) return;
        try {
            ListenerAdapter.sendListeners(new ServerUnregisteredEvent(ListenerAdapter.ServerName.ALL, name));
        } catch (Exception e) {
            System.out.println("Could not announce the shutdown of " + name + ": " + e.getMessage());
        }
    }

    public ServerInstance stop(ListenerAdapter.ServerName name) {
        Optional<ServerInstance> first = instances.stream().filter(instance -> instance.getName().equals(name)).findFirst();
        if (first.isEmpty()) {
            throw new RuntimeException("Server with name " + name + " not found");
        }
        first.ifPresent(instance -> {
            try {
                instance.stop();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        announceUnregistered(name);
        updateInstances();
        return first.get();
    }

    public void shutdownNetwork() throws IOException {
        updateInstances();
        for (ServerInstance instance : instances) {
            instance.stop();
        }
    }

    /**
     * @param memoryWatch the budget this handler starts servers against
     */
    public void setMemoryWatch(MemoryWatch memoryWatch) {
        this.memoryWatch = memoryWatch;
    }

    public boolean doesInstanceExist(ListenerAdapter.ServerName name) {
        updateInstances();
        return instances.stream().anyMatch(instance -> instance.getName().equals(name));
    }

    public ServerInstance getInstance(ListenerAdapter.ServerName name) {
        for (ServerInstance instance : instances) {
            if (instance.getName().equals(name)) return instance;
        }
        return null;
    }

    /**
     * @return every server that is currently running
     */
    public List<ServerInstance> getInstances() {
        return instances;
    }

    public void updateInstances() {
        instances.removeIf(instance -> {
            try {
                if (instance.isAlive()) return false;
                // a server that just started needs a moment before it accepts connections
                return !instance.isStarting();
            } catch (IOException e) {
                return false;
            }
        });
    }

    /**
     * @return a snapshot of every running server, as it is sent to the rest of the network
     */
    public Server[] collectToServer() {
        updateInstances();
        List<Server> servers = new ArrayList<>();
        for (ServerInstance instance : instances) {
            if (instance.getName().isReserved()) continue;
            boolean online;
            try {
                online = instance.isAlive();
            } catch (IOException e) {
                online = false;
            }
            servers.add(instance.toServer(online));
        }
        return servers.toArray(new Server[0]);
    }
}
