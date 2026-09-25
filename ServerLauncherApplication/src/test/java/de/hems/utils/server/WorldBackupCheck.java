package de.hems.utils.server;

import java.io.File;
import java.nio.file.Files;

/**
 * Checks that a server's worlds are copied away before its first start on a new Minecraft version, and
 * only then. Run like {@code RewardCheck}; exits non-zero when something is wrong.
 */
public final class WorldBackupCheck {

    private static int failed;
    private static int passed;

    public static void main(String[] args) throws Exception {
        File server = Files.createTempDirectory("SURVIVAL").toFile();
        new File(server, "world/region").mkdirs();
        Files.writeString(new File(server, "world/level.dat").toPath(), "level");
        Files.writeString(new File(server, "world/region/r.0.0.mca").toPath(), "blocks");
        Files.writeString(new File(server, "world/session.lock").toPath(), "lock");
        new File(server, "plugins").mkdirs();

        check("no old jar, no backup", WorldBackup.beforeUpgrade(server, "paper-26.3-40.jar") == null);
        Files.writeString(new File(server, "paper-26.3-12.jar").toPath(), "jar");
        check("a new build of the same version needs no backup",
                WorldBackup.beforeUpgrade(server, "paper-26.3-40.jar") == null);
        new File(server, "paper-26.3-12.jar").delete();
        Files.writeString(new File(server, "paper-26.2-112.jar").toPath(), "jar");

        File backup = WorldBackup.beforeUpgrade(server, "paper-26.3-40.jar");
        check("a version change makes a backup", backup != null);
        check("named after the old version", backup != null && backup.getName().startsWith("26.2-"));
        check("the world is in it", backup != null && new File(backup, "world/region/r.0.0.mca").isFile());
        check("the lock is not", backup != null && !new File(backup, "world/session.lock").exists());
        check("folders without level.dat are no worlds", backup != null && !new File(backup, "plugins").exists());
        check("the version is read off the jar", "26.2".equals(WorldBackup.versionOf("paper-26.2-112.jar")));
        check("other jars are no paper", WorldBackup.versionOf("velocity-4.2.0-30.jar") == null);

        System.out.println(passed + " passed, " + failed + " failed (backup landed in " + backup + ")");
        if (failed > 0) System.exit(1);
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
            return;
        }
        failed++;
        System.out.println("FAIL " + what);
    }
}
