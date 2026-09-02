package de.hems.utils.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Bedwars maps that were not shipped with the launcher.
 * <p>
 * A map that comes with a version is an {@link de.hems.types.FileType.ASSET}, which means adding one is a
 * code change and a release. That is right for the maps the network is delivered with and wrong for the
 * one somebody downloaded on a Tuesday. This is the other way in: drop the world folder into
 * {@code ./bedwars-maps/} next to the launcher, and every round server created from then on has it.
 * <p>
 * A map is a folder with a {@code level.dat} in it, plus optionally a {@code <name>.yml} beside it holding
 * the points that were set with {@code /bw setup}. Without the yaml the map is there but not playable
 * until somebody sets it up - which is exactly the state a freshly downloaded world is in.
 * <p>
 * Copied only where nothing of that name is there yet. A round server whose copy an admin has edited keeps
 * the edit; the folder here is a source, not a master that overwrites.
 */
public class CustomMaps {

    /** Where the maps an admin adds themselves live, next to the launcher. */
    public static final String DIRECTORY = "./bedwars-maps";
    /** Where they belong on a bedwars server, which is where the plugin looks for them. */
    private static final String TARGET = "maps";

    private final File directory;

    public CustomMaps() {
        this(new File(DIRECTORY));
    }

    public CustomMaps(File directory) {
        this.directory = directory;
    }

    /**
     * @return the id of every map lying here, lower case and sorted
     */
    public List<String> list() {
        File[] entries = directory.listFiles();
        if (entries == null) return List.of();
        List<String> names = new ArrayList<>();
        for (File entry : entries) {
            if (!entry.isDirectory()) continue;
            if (!new File(entry, "level.dat").isFile()) continue;
            names.add(entry.getName().toLowerCase(Locale.ROOT));
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Puts every map that is missing onto one server.
     *
     * @param serverDirectory the server to install into
     */
    public void installInto(File serverDirectory) {
        List<String> maps = list();
        if (maps.isEmpty()) return;
        File target = new File(serverDirectory, TARGET);
        target.mkdirs();
        for (String map : maps) {
            try {
                install(map, target);
            } catch (IOException e) {
                // a map that cannot be copied is one map, not a reason to leave the server unstarted
                System.out.println("Could not install the map " + map + " on "
                        + serverDirectory.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * @param map    the map to copy
     * @param target the {@code maps} directory of a server
     */
    private void install(String map, File target) throws IOException {
        File world = new File(target, map);
        if (!world.exists()) {
            copyTree(new File(directory, map).toPath(), world.toPath());
            System.out.println("Installed the map " + map + " on " + target.getParentFile().getName());
        }
        File definition = new File(directory, map + ".yml");
        File installed = new File(target, map + ".yml");
        if (definition.isFile() && !installed.isFile()) {
            Files.copy(definition.toPath(), installed.toPath());
        }
    }

    /**
     * Copies a whole directory, keeping its shape.
     *
     * @param from where it is
     * @param to   where it should be
     */
    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path path : paths.toList()) {
                Path destination = to.resolve(from.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                    continue;
                }
                Files.createDirectories(destination.getParent());
                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
