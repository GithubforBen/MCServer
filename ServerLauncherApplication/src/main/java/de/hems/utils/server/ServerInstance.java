package de.hems.utils.server;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.Server;
import de.hems.types.ServerTemplate;
import de.hems.utils.server.console.ConsoleBuffer;
import de.hems.utils.server.console.ConsoleTailer;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.net.SocketFactory;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ServerInstance {

    /** How long a server may take to open its port before it counts as dead. */
    private static final long STARTUP_GRACE_MS = 180_000L;
    /** The port the proxy listens on - it has no server port of its own. */
    private static final int PROXY_PORT = 25565;
    /** The file tmux pipes the console output of the server into. */
    private static final String CONSOLE_LOG = "console.log";

    private final ListenerAdapter.ServerName name;
    private final File directory;
    private final FileType.SERVER jarFile;
    private final ServerTemplate template;
    private Process process;
    private final int allocatedMemoryMB;
    private final FileType.PLUGIN[] plugins;
    private long startedAt;
    private boolean stopRequested;
    private final ConsoleBuffer console = new ConsoleBuffer();
    private ConsoleTailer consoleTailer;

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
                break;
            }
            case VELOCITY -> {
                new VelocityConfigurator(directory.getAbsolutePath(), 25565, plugins).configure();
            }
        }
    }

    private void exec(String command) throws IOException {
        exec(command.split(" "));
    }

    /**
     * Runs a command without splitting it on spaces, for arguments that contain some.
     *
     * @param command the command and its arguments, already separated
     */
    private void exec(String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command).directory(directory);
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
        startConsoleCapture();
        System.out.println("Server " + name + " started");
    }

    /**
     * Starts collecting the console output of the server.
     * <p>
     * The server itself runs inside tmux, so nothing it prints ever reaches this process - the reader
     * thread that used to sit here watched the {@code tmux send-keys} process, which exits after a
     * millisecond, and therefore never read a single line. {@code tmux pipe-pane} writes the output of the
     * pane into a file instead, and {@link ConsoleTailer} follows that file.
     */
    private void startConsoleCapture() throws IOException {
        File log = new File(directory, CONSOLE_LOG);
        // start each run with an empty log, otherwise the view would open on the previous run
        if (log.exists() && !log.delete()) {
            System.out.println("Could not clear the old console log of " + name);
        }
        console.clear();
        exec("tmux", "pipe-pane", "-t", "server-" + name, "cat >> '" + log.getAbsolutePath() + "'");
        if (consoleTailer != null) consoleTailer.stop();
        consoleTailer = new ConsoleTailer(log, console);
        consoleTailer.start();
    }

    /**
     * Stops collecting console output and tells tmux to stop piping.
     */
    private void stopConsoleCapture() {
        if (consoleTailer != null) {
            consoleTailer.stop();
            consoleTailer = null;
        }
        try {
            // pipe-pane without a command switches the pipe off again
            exec("tmux", "pipe-pane", "-t", "server-" + name);
        } catch (IOException e) {
            System.out.println("Could not stop the console pipe of " + name + ": " + e.getMessage());
        }
    }

    /**
     * @return the recent console output of this server and the hook to watch it live
     */
    public ConsoleBuffer getConsole() {
        return console;
    }

    public void stop() throws IOException {
        System.out.println("Stopping server " + name);
        stopRequested = true;
        executeCommand("stop");
        stopConsoleCapture();
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
