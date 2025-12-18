package de.hems.utils.server;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class ServerInstance {
    private final ListenerAdapter.ServerName name;
    private final File directory;
    private final FileType.SERVER jarFile;
    private Process process;
    private int allocatedMemoryMB;
    private boolean printStream = true;
    private FileType.PLUGIN[] plugins;

    public ServerInstance(ListenerAdapter.ServerName name, int allocatedMemoryMB, FileType.SERVER jarFile, FileType.PLUGIN[] plugins) throws Exception {
        this.name = name;
        this.allocatedMemoryMB = allocatedMemoryMB;
        this.jarFile = jarFile;
        this.directory = new File("./servers/" + name + "/");
        this.plugins = plugins;
        if (!directory.exists()) {
            directory.mkdirs();
        }
        switch (jarFile) {
            case PAPER -> {
                YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
                List<String> ops = config.getStringList("ops");
                new PaperConfigurator(name, true, ops.stream().map((x) -> UUIDFetcher.findUUIDByName(x, true)).toList(), new String[]{"for_Sale", "SA_MI"}, directory.getAbsolutePath(), plugins).configure();
                printStream = true;
                break;
            }
            case VELOCITY -> {
                new VelocityConfigurator(directory.getAbsolutePath(), 25565, plugins).configure();
            }
        }
    }

    public void start() throws IOException {
        System.out.println("Starting server " + name);
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Xmx" + allocatedMemoryMB + "m", FileType.SERVER.getFileName(jarFile)).directory(directory);
        pb.redirectErrorStream(true);
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
        executeCommand("stop");
        new Thread(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(10L));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (process.isAlive()) {
                process.destroy();
            }
            System.out.println("Server " + name + " destroyed");
        }).start();
        System.out.println("Server " + name + " stopped");
    }

    public void kill() {
        if (process.isAlive()) {
            process.destroyForcibly();
            System.out.println("Server " + name + " killed");
        }
    }

    public void executeCommand(String command) throws IOException {
        if (process == null) {
            return;
        }
        if (!process.isAlive()) {
            return;
        }
        OutputStream outputStream = process.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        outputStreamWriter.write(command + "\n");
        outputStreamWriter.flush();
        outputStreamWriter.close();
    }

    public ListenerAdapter.ServerName getName() {
        return name;
    }

    public boolean isAlive() {
        return process.isAlive();
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
}
