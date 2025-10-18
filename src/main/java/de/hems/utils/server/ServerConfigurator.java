package de.hems.utils.server;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class ServerConfigurator {
    private final String directory;

    public ServerConfigurator(String directory) {
        this.directory = directory;
    }

    public void writeToFile(String file, String content) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "/" + file));
        writer.write(content);
        writer.close();
    }
}
