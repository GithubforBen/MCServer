package de.hems.utils.server;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class ServerConfigurator {
    protected final String directory;

    public ServerConfigurator(String directory) {
        this.directory = directory;
    }

    public void writeToFile(String file, String content) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "/" + file));
        writer.write(content);
        writer.close();
    }

	public boolean doesFileExist(String file) {
		File file = new File(file);
		return file.exists();
	}

	public void writeToFileIfFileDoesntExist(String file, String content) throws Exception {
		if (!doesFileExist(file)) {
			writeToFile(file, content);
		}
	}
}
