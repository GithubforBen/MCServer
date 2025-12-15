package de.hems.utils.server;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class ServerConfigurator {
    protected final String directory;

    public ServerConfigurator(String directory) {
        this.directory = directory;
    }

    public void writeToFile(String file, String content) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "/" + file, true));
        writer.write(content + "\n");
        writer.close();
    }

    public void writeToYmlConfiguration(String file, String key, Object value) throws Exception {
        File file1 = new File(directory + "/" + file);
        if (!file1.exists()) {file1.createNewFile();}
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file1);
        config.set(key, value);
        config.save(file1);
    }

	public boolean doesFileExist(String fileName) {
		File file = new File(fileName);
		return file.exists();
	}

	public void writeToFileIfFileDoesntExist(String file, String content) throws Exception {
		if (!doesFileExist(file)) {
			writeToFile(file, content);
		}
	}
}
