package de.hems.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Copying and removing whole folders - worlds, maps, server directories.
 * <p>
 * This used to be written out again in nine places (the casino, the prepared maps, the lobby world, the
 * bedwars maps, the backups, the run reset, the settlement ...), each a little different. It is here once,
 * and what differed is now a parameter: which files a copy leaves behind.
 */
public final class FileTrees {

    /**
     * The files a copied world must not carry: the lock of the server it was running on, and its identity -
     * two worlds with the same {@code uid.dat} confuse the server about which one it is looking at, and the
     * symptom turns up much later than the copy.
     */
    public static final Set<String> WORLD_IDENTITY = Set.of("session.lock", "uid.dat");
    /** Only the lock - for a backup, which should be the world as it was, identity included. */
    public static final Set<String> LOCK_ONLY = Set.of("session.lock");

    private FileTrees() {
    }

    /**
     * Copies a folder with everything in it, replacing files that are already there.
     *
     * @param from where it is; nothing happens when it does not exist
     * @param to   where it should be
     */
    public static void copy(Path from, Path to) throws IOException {
        copy(from, to, Set.of());
    }

    /**
     * Copies a folder with everything in it, leaving out files of the given names wherever they are.
     *
     * @param from  where it is; nothing happens when it does not exist
     * @param to    where it should be
     * @param leave file names that are not copied
     */
    public static void copy(Path from, Path to, Set<String> leave) throws IOException {
        if (!Files.exists(from)) return;
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path path : paths.toList()) {
                Path name = path.getFileName();
                if (name != null && !path.equals(from) && leave.contains(name.toString())) continue;
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

    /**
     * Removes a folder with everything in it, deepest entry first. Stops at the first entry that cannot be
     * removed.
     *
     * @param path the folder; nothing happens when it does not exist
     */
    public static void delete(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> entries = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path entry : entries) Files.deleteIfExists(entry);
        }
    }

    /**
     * The folders a world keeps its players in: {@code players} since 26.1, the other three before. A map
     * or a casino copied to another server must not bring along where somebody stood and what they carried
     * on the server it was built on.
     */
    public static final List<String> PLAYER_DATA = List.of("players", "playerdata", "stats", "advancements");

    /**
     * Removes what a world remembers about players, so a copy starts without them.
     *
     * @param world the world folder
     */
    public static void stripPlayers(File world) {
        for (String folder : PLAYER_DATA) deleteQuietly(new File(world, folder));
    }

    /**
     * Removes as much of a folder as it can, carrying on past entries that will not go.
     *
     * @param folder the folder or file, may be {@code null}
     * @return whether it is gone
     */
    public static boolean deleteQuietly(File folder) {
        if (folder == null || !folder.exists()) return true;
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            for (Path entry : paths.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.deleteIfExists(entry);
                } catch (IOException ignored) {
                    // the rest still goes; whether the whole is gone is what the answer says
                }
            }
        } catch (IOException e) {
            return false;
        }
        return !folder.exists();
    }
}
