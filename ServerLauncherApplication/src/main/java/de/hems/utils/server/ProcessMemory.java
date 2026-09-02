package de.hems.utils.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * How much memory a running server actually uses.
 * <p>
 * The servers are started inside tmux, so the {@link Process} the launcher holds is the {@code send-keys}
 * call and not the java process - asking it anything about memory is asking the wrong process. What is
 * left is the operating system: on linux every process has a {@code /proc} entry, its working directory is
 * the server's own folder, and that is enough to tell the survival server's jvm from the lobby's.
 * <p>
 * Elsewhere this simply reports nothing. A recommendation without a measurement behind it would be a guess,
 * and a guess about how much heap a server may lose is how a server ends up killed.
 */
public final class ProcessMemory {

    private static final Path PROC = Path.of("/proc");

    private ProcessMemory() {
    }

    /**
     * @return whether this machine can be measured at all
     */
    public static boolean isSupported() {
        return Files.isDirectory(PROC);
    }

    /**
     * Reads how much resident memory the server in a directory holds.
     *
     * @param directory the server's own folder
     * @return the resident size in MB, or {@code -1} when the process could not be found or measured
     */
    public static int residentMB(File directory) {
        if (!isSupported() || directory == null) return -1;
        Path wanted;
        try {
            wanted = directory.getCanonicalFile().toPath();
        } catch (IOException e) {
            return -1;
        }
        try (var entries = Files.list(PROC)) {
            for (Path entry : entries.toList()) {
                String pid = entry.getFileName().toString();
                if (!pid.chars().allMatch(Character::isDigit)) continue;
                if (!isJava(entry)) continue;
                Path cwd;
                try {
                    cwd = entry.resolve("cwd").toRealPath();
                } catch (IOException e) {
                    // another user's process, or one that ended while we looked at it
                    continue;
                }
                if (!cwd.equals(wanted)) continue;
                return residentOf(entry);
            }
        } catch (IOException e) {
            return -1;
        }
        return -1;
    }

    /**
     * @param proc the {@code /proc} entry of a process
     * @return whether it is a java process running a jar
     */
    private static boolean isJava(Path proc) {
        try {
            String cmdline = Files.readString(proc.resolve("cmdline"));
            return cmdline.contains("java") && cmdline.contains("-jar");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @param proc the {@code /proc} entry of a process
     * @return its resident size in MB, or {@code -1}
     */
    private static int residentOf(Path proc) {
        try {
            List<String> status = Files.readAllLines(proc.resolve("status"));
            for (String line : status) {
                if (!line.startsWith("VmRSS:")) continue;
                String[] parts = line.split("\\s+");
                // VmRSS: <number> kB
                if (parts.length < 2) return -1;
                return (int) (Long.parseLong(parts[1]) / 1024L);
            }
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
        return -1;
    }
}
