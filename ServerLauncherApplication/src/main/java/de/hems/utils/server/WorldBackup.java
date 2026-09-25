package de.hems.utils.server;

import de.hems.files.FileTrees;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copies the worlds of a server away before it is started on a new Minecraft version.
 * <p>
 * A new Minecraft version converts every world it loads, and there is no way back: the old server cannot
 * read the converted world any more. So the first start of a server on a new version - recognised by the
 * paper jar that is still lying in its directory from the last start - is preceded by a copy of every
 * world folder into {@code ./backups/<server>/<old version>-<time>/}.
 * <p>
 * If the copy fails, the start fails. A server that stays off until somebody has looked is better than a
 * world that was converted without a way back.
 */
public final class WorldBackup {

    /** Where the copies go, next to the launcher. */
    public static final String DIRECTORY = "./backups";

    private static final Pattern PAPER_JAR = Pattern.compile("paper-(.+)-(\\d+)\\.jar");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private WorldBackup() {
    }

    /**
     * @param jarName a paper jar name, like {@code paper-26.3-40.jar}
     * @return the Minecraft version in it, or {@code null} if it is not a paper jar
     */
    static String versionOf(String jarName) {
        Matcher matcher = PAPER_JAR.matcher(jarName);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * Backs up the worlds of a server if it is about to move to another Minecraft version.
     *
     * @param serverDirectory the server
     * @param newJarName      the paper jar it is about to be started with
     * @return where the backup went, or {@code null} when none was needed
     * @throws IOException when a backup was needed and could not be made
     */
    public static File beforeUpgrade(File serverDirectory, String newJarName) throws IOException {
        String newVersion = versionOf(newJarName);
        if (newVersion == null) return null;
        String oldVersion = null;
        File[] jars = serverDirectory.listFiles((dir, name) -> versionOf(name) != null);
        if (jars != null) {
            for (File jar : jars) {
                String version = versionOf(jar.getName());
                if (!newVersion.equals(version)) oldVersion = version;
            }
        }
        if (oldVersion == null) return null;
        List<File> worlds = worldsIn(serverDirectory);
        if (worlds.isEmpty()) return null;

        File target = new File(DIRECTORY + "/" + serverDirectory.getName() + "/" + oldVersion + "-"
                + LocalDateTime.now().format(STAMP));
        System.out.println("Server " + serverDirectory.getName() + " moves from Minecraft " + oldVersion + " to "
                + newVersion + " - backing up " + worlds.size() + " world(s) to " + target.getPath() + " first.");
        for (File world : worlds) {
            FileTrees.copy(world.toPath(), new File(target, world.getName()).toPath(), FileTrees.LOCK_ONLY);
        }
        System.out.println("Backup of " + serverDirectory.getName() + " done.");
        return target;
    }

    /**
     * @param serverDirectory the server
     * @return every folder directly in it that is a world, recognised by its {@code level.dat}
     */
    static List<File> worldsIn(File serverDirectory) {
        List<File> worlds = new ArrayList<>();
        File[] children = serverDirectory.listFiles(File::isDirectory);
        if (children == null) return worlds;
        for (File child : children) {
            if (new File(child, "level.dat").isFile()) worlds.add(child);
        }
        return worlds;
    }

}
