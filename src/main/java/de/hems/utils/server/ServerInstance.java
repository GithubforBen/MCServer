package de.hems.utils.server;

import de.hems.FileHandler;
import de.hems.FileType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class ServerInstance {
    private Process process;
    private final String name;
    private final File directory;
    private int allocatedMemoryMB;
    private final FileType jarFile;
    private boolean printStream = true;

    public ServerInstance(String name, int allocatedMemoryMB, FileType jarFile) throws IOException {
        this.name = name;
        this.allocatedMemoryMB = allocatedMemoryMB;
        this.jarFile = jarFile;
        this.directory = new File("./servers/" + name + "/");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File jar = new File(directory.getPath() + "/" + FileType.getFileName(jarFile));
        if (!jar.exists()) {
            File file = new FileHandler().provideFile(jarFile);
            Files.copy(file.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String str = "eula=true";
            BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "/eula.txt"));
            writer.write(str);
            writer.close();
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
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
        }
    }

    public void executeCommand(String command) throws IOException {
        OutputStream outputStream = process.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        outputStreamWriter.write(command + "\n");
        outputStreamWriter.flush();
        outputStreamWriter.close();
    }
}
