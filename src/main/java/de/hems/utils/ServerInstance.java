package de.hems.utils;

import de.hems.FileHandler;
import de.hems.FileType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ServerInstance {
    private Process process;
    private String name;
    private File directory;

    public ServerInstance(String name) throws IOException {
        this.name = name;
        this.directory = new File("./" + name + "/");
        if (!directory.exists()) {
            directory.mkdir();
        }
        File paper = new File(directory.getPath() + "/" + FileType.getFileName(FileType.PAPER));
        if (!paper.exists()) {
            File file = new FileHandler().provideFile(FileType.PAPER);
            Files.copy(file.toPath(), paper.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void start() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", FileType.getFileName(FileType.PAPER)).directory(directory);
        process = pb.start();
    }
}
