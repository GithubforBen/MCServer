package de.hems.utils;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Reading and writing the yaml files the launcher keeps its state in - money, awards, teams, events.
 * <p>
 * Every store used to do this itself, the same way, with two holes in it:
 * <ul>
 *     <li>A file was written in place. A crash in the middle of the write - power, {@code kill -9}, a full
 *     disk - left half a file behind.</li>
 *     <li>A file that could not be read was read as <em>empty</em>. The next write then replaced it for good,
 *     and for {@code money.yml} an empty file even meant importing the old survival balances again.</li>
 * </ul>
 * Here a file is written next to itself and moved over the old one in one step, so it is either the old
 * version or the new one. And a file that cannot be read stops the launcher, after a copy of it has been put
 * aside: a network that does not start is a problem somebody notices, a network that starts with everybody's
 * money at zero is one they notice too late.
 */
public final class YamlFiles {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private YamlFiles() {
    }

    /**
     * Reads a file, creating it empty if it is not there.
     *
     * @param file the file
     * @return its contents
     * @throws IllegalStateException when the file exists but cannot be read - it is copied aside first
     */
    public static YamlConfiguration load(File file) {
        YamlConfiguration config = new YamlConfiguration();
        if (!file.exists()) {
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new IllegalStateException("Could not create " + file.getPath() + ": " + e.getMessage(), e);
            }
            return config;
        }
        try {
            config.load(file);
            return config;
        } catch (IOException | InvalidConfigurationException e) {
            File aside = new File(file.getPath() + ".broken-" + LocalDateTime.now().format(STAMP));
            try {
                Files.copy(file.toPath(), aside.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException copy) {
                System.out.println("Could not even copy " + file.getPath() + " aside: " + copy.getMessage());
            }
            throw new IllegalStateException(file.getPath() + " cannot be read (" + e.getMessage() + "). A copy is in "
                    + aside.getPath() + ". Nothing is started, so the file is not overwritten with empty data - "
                    + "fix it or restore it from a backup.", e);
        }
    }

    /**
     * Writes a file so that it is never half written: into a file next to it, then moved over it.
     *
     * @param config what to write
     * @param file   where
     * @throws IOException when it could not be written; the old file is then untouched
     */
    public static void save(YamlConfiguration config, File file) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, config.saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Writes a file and reports a failure instead of throwing - what every store does on a write.
     *
     * @param config what to write
     * @param file   where
     * @return whether it was written
     */
    public static boolean saveOrLog(YamlConfiguration config, File file) {
        try {
            save(config, file);
            return true;
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
