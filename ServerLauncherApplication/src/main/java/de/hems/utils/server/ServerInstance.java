package de.hems.utils.server;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.Server;
import de.hems.types.ServerTemplate;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.net.SocketFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ServerInstance {

    /** How long a server may take to open its port before it counts as dead. */
    private static final long STARTUP_GRACE_MS = 180_000L;
    /** The port the proxy listens on - it has no server port of its own. */
    private static final int PROXY_PORT = 25565;

    private final ListenerAdapter.ServerName name;
    private final File directory;
    private final FileType.SERVER jarFile;
    private final ServerTemplate template;
    private Process process;
    private final int allocatedMemoryMB;
    private boolean printStream = true;
    private final FileType.PLUGIN[] plugins;
    private long startedAt;
    private boolean stopRequested;

    public ServerInstance(ListenerAdapter.ServerName name, int allocatedMemoryMB, FileType.SERVER jarFile, FileType.PLUGIN[] plugins) throws Exception {
        this(name, allocatedMemoryMB, jarFile, plugins, ServerTemplate.forServerName(name.toString()));
    }

    public ServerInstance(ListenerAdapter.ServerName name, int allocatedMemoryMB, FileType.SERVER jarFile,
                          FileType.PLUGIN[] plugins, ServerTemplate template) throws Exception {
        this.name = name;
        this.allocatedMemoryMB = allocatedMemoryMB;
        this.jarFile = jarFile;
        this.template = template;
        this.directory = new File("./servers/" + name + "/");
        this.plugins = plugins;
        if (!directory.exists()) {
            directory.mkdirs();
        }
        switch (jarFile) {
            case PAPER -> {
                YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
                List<String> ops = config.getStringList("ops");
                List<String> whitelist = config.getStringList("whitelist");
                new PaperConfigurator(name, true, ops.stream().map((x) -> UUIDFetcher.findUUIDByName(x, true)).toList(),
                        whitelist.toArray(new String[0]), directory.getAbsolutePath(), plugins).configure();
                printStream = true;
                break;
            }
            case VELOCITY -> {
                new VelocityConfigurator(directory.getAbsolutePath(), 25565, plugins).configure();
            }
        }
    }

    private void exec(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" ")).directory(directory);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.start();
    }

    public void start() throws IOException {
        System.out.println("Starting server " + name + (name.isJoinable() ? " on port " + name.getPort() : ""));
        startedAt = System.currentTimeMillis();
        exec("tmux new-session -d -s server-" + name.toString());
        ProcessBuilder pb = new ProcessBuilder("tmux", "send-keys", "-t","server-"+name.toString() ,  "java -jar -Xmx" + allocatedMemoryMB + "m " + FileType.SERVER.getFileName(jarFile), "C-m").directory(directory);
        System.out.println(pb.command());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        process = pb.start();
        new Thread(() -> {
            while (process.isAlive()) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (printStream) {
                            System.out.println(line);
                        }
                    }
                    BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String errorLine;
                    while ((errorLine = error.readLine()) != null) {
                        if (printStream) {
                            System.out.println(errorLine);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Server " + name + " exited");
        }).start();
        System.out.println("Server " + name + " started");
    }

    public void stop() throws IOException {
        System.out.println("Stopping server " + name);
        stopRequested = true;
        executeCommand("stop");
    }


    public void executeCommand(String command) throws IOException {
        exec("tmux new-session -d -s server-" + name.toString());
        System.out.println(command);
        ProcessBuilder pb = new ProcessBuilder("tmux", "send-keys", "-t","server-"+name.toString(), command, "C-m").directory(directory);
        System.out.println(pb.command());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        process = pb.start();
    }

    public ListenerAdapter.ServerName getName() {
        return name;
    }

    public boolean isAlive() throws IOException {
        int port = name.isJoinable() ? name.getPort() : PROXY_PORT;
        if (!name.isJoinable() && jarFile != FileType.SERVER.VELOCITY) return isStarting();
        Socket socket = SocketFactory.getDefault().createSocket();
        try {
            socket.setSoTimeout(5000);
            socket.connect(new InetSocketAddress("127.0.0.1", port), 2000);
            socket.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * @return whether the server is still booting and therefore not reachable yet
     */
    public boolean isStarting() {
        return !stopRequested && startedAt != 0 && System.currentTimeMillis() - startedAt < STARTUP_GRACE_MS;
    }

    /**
     * @param online whether the server currently answers on its port
     * @return the snapshot that is sent to the rest of the network
     */
    public Server toServer(boolean online) {
        return new Server(name.toString(), name.getPort(), allocatedMemoryMB, template, jarFile, plugins,
                online || isStarting());
    }

    public File getDirectory() {
        return directory;
    }

    public int getAllocatedMemoryMB() {
        return allocatedMemoryMB;
    }

    public FileType.SERVER getJarFile() {
        return jarFile;
    }

    public FileType.PLUGIN[] getPlugins() {
        return plugins;
    }

    public ServerTemplate getTemplate() {
        return template;
    }
}
