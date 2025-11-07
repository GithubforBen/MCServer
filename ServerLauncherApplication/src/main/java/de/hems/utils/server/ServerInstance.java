package de.hems.utils.server;

import de.hems.FileType;
import de.hems.Main;

import java.io.*;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class ServerInstance {
    private Process process;
    private final String name;
    private final File directory;
    private int allocatedMemoryMB;
    private final FileType jarFile;
    private boolean printStream = true;
    private int port;
    private boolean isProxied;

    public int getPort() {
        return port;
    }

    public ServerInstance(String name, int allocatedMemoryMB, FileType jarFile, int port, boolean isProxied) throws Exception {
        this.name = name;
        this.allocatedMemoryMB = allocatedMemoryMB;
        this.jarFile = jarFile;
        this.port = port;
        this.directory = new File("./servers/" + name + "/");
        this.isProxied = isProxied;
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File jar = new File(directory.getPath() + "/" + FileType.getFileName(jarFile));
        if (!jar.exists()) {
            switch (jarFile) {
                case PAPER -> {
                    List<String> ops = Main.getInstance().getConfiguration().getConfig().getStringList("ops");
                    new PaperConfigurator(Main.getInstance().getIp(), port, isProxied, ops.stream().map((x) -> UUID.fromString(x)).toList(), new String[]{"for_Sale"}, directory.getAbsolutePath()).configure();
                }
            }
        }
    }

    public void start() throws IOException {
        System.out.println("Starting server " + name);
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Xmx" + allocatedMemoryMB + "m", FileType.getFileName(jarFile)).directory(directory);
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

    public String getName() {
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

    public FileType getJarFile() {
        return jarFile;
    }

    public boolean isProxied() {
        return isProxied;
    }
}
