package de.hems.utils.server;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public class ServerConfigurator {
    protected final String directory;
    protected boolean firstTime;

    public ServerConfigurator(String directory) throws IOException {
        this.directory = directory;
        if (!new File(directory).exists()) throw new RuntimeException();
        firstTime = !new File(directory + "/config.check").exists();
        new File(directory + "/config.check").createNewFile();
    }

    public void writeToFile(String file, String content, boolean rewriteEveryTime) throws Exception {
        if (!firstTime && !rewriteEveryTime) return;
        BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "/" + file, true));
        writer.write(content + "\n");
        writer.close();
    }
    public void overwriteToFile(String file, String content, boolean rewriteEveryTime) throws Exception {
        if (!firstTime && !rewriteEveryTime) return;
        BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "/" + file, false));
        writer.write(content + "\n");
        writer.close();
    }

    public void writeToYmlConfiguration(String file, String key, Object value, boolean rewriteEveryTime) throws Exception {
        if (!firstTime && !rewriteEveryTime) return;
        File file1 = new File(directory + "/" + file);
        if (!file1.getParentFile().exists()) file1.getParentFile().mkdirs();
        if (!file1.exists()) {file1.createNewFile();}
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file1);
        config.set(key, value);
        config.save(file1);
    }
}
