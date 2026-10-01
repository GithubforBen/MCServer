package de.hems.utils;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Configuration {
    private File file;
    private YamlConfiguration config;
    /** When the file was last read or written here. */
    private long seen;
    /** Sections other programs may change in the file, see {@link #adopt}. */
    private final java.util.Set<String> adopted = new java.util.LinkedHashSet<>();
    public Configuration() {
        file = new File("./main-config.yml");
        System.out.println(file.getAbsolutePath());
        // strict on purpose: this file holds the proxy secret and the website account, and reading a broken
        // one as empty would quietly make new ones
        config = YamlFiles.load(file);
        seen = file.lastModified();
    }

    public synchronized void save() {
        // a change made on disk in the meantime is taken over first rather than written over
        adoptChanges();
        try {
            YamlFiles.save(config, file);
            seen = file.lastModified();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Takes one section over from the file on disk if the file was changed by somebody else since it was
     * read or written here - {@code ./admin-passwort.sh} resets a password while the launcher runs. Without
     * this the running launcher would keep checking the old password, and its next save would write the old
     * one back. Once named here, the section is also taken over before every save.
     *
     * @param path the section to take over, e.g. {@code web.admins}
     */
    public synchronized void adopt(String path) {
        adopted.add(path);
        adoptChanges();
    }

    private void adoptChanges() {
        long modified = file.lastModified();
        if (modified == seen || adopted.isEmpty()) return;
        seen = modified;
        try {
            YamlConfiguration disk = YamlFiles.load(file);
            for (String path : adopted) config.set(path, disk.get(path));
        } catch (RuntimeException e) {
            System.out.println("Could not read " + adopted + " from " + file.getName() + ": " + e.getMessage());
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}
