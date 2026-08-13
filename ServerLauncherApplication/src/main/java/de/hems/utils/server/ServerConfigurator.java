package de.hems.utils.server;

import de.hems.types.FileType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Sets a {@code key=value} entry, replacing an entry that is already there. Unlike
     * {@link #writeToFile(String, String, boolean)} this also works on servers that were configured before,
     * which is what keeps ports and settings correct when a server is reconfigured.
     *
     * @param file  the properties file, relative to the server directory
     * @param key   the key to set
     * @param value the value to set
     */
    public void setProperty(String file, String key, Object value) throws IOException {
        File target = new File(directory + "/" + file);
        List<String> lines = new ArrayList<>();
        if (target.exists()) lines = new ArrayList<>(Files.readAllLines(target.toPath(), StandardCharsets.UTF_8));
        String entry = key + "=" + value;
        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("#") || !line.contains("=")) continue;
            if (line.substring(0, line.indexOf('=')).trim().equals(key)) {
                lines.set(i, entry);
                replaced = true;
            }
        }
        if (!replaced) lines.add(entry);
        Files.write(target.toPath(), lines, StandardCharsets.UTF_8);
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

    /**
     * Removes jars of plugins that are not installed anymore, so an updated plugin does not end up next to
     * the version it replaces.
     *
     * @param plugins the plugins that belong on the server
     */
    protected void removeStalePlugins(Collection<FileType.PLUGIN> plugins) {
        File pluginFolder = new File(directory + "/plugins/");
        File[] installed = pluginFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (installed == null) return;
        Set<String> wanted = new HashSet<>();
        for (FileType.PLUGIN plugin : plugins) wanted.add(FileType.PLUGIN.getFileName(plugin));
        for (File jar : installed) {
            if (wanted.contains(jar.getName())) continue;
            if (jar.delete()) System.out.println("Removed outdated plugin " + jar.getName());
        }
    }

    /**
     * Removes server jars that are not used anymore, e.g. after an update to a new minecraft version.
     *
     * @param currentJarName the file name of the jar that is in use
     */
    protected void removeStaleServerJars(String currentJarName) {
        File[] jars = new File(directory).listFiles((dir, name) ->
                name.toLowerCase().endsWith(".jar") && !name.equals(currentJarName));
        if (jars == null) return;
        for (File jar : jars) {
            if (jar.delete()) System.out.println("Removed outdated server jar " + jar.getName());
        }
    }
}
