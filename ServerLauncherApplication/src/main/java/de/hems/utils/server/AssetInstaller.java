package de.hems.utils.server;

import de.hems.FileHandler;
import de.hems.types.FileType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Puts the content a template ships with onto a server: the worlds, and the configuration next to them.
 * <p>
 * Jars are replaced on every start, because code is the launcher's to decide. Content is not: the point of
 * delivering a map is that somebody can then move a generator in it with {@code /bw setup}, and unpacking
 * again on the next restart would quietly throw that away. So an asset is written once and then left alone
 * until the version shipped with the launcher changes, which is the one moment where a corrected map is
 * worth more than local edits.
 */
public class AssetInstaller {

    /** Where the installed versions are recorded, inside the server directory. */
    private static final String MARKER_DIRECTORY = ".assets";

    private final File directory;

    /**
     * @param directory the server directory to install into
     */
    public AssetInstaller(File directory) {
        this.directory = directory;
    }

    /**
     * Installs everything that is not there yet.
     *
     * @param assets the assets the template ships with
     */
    public void install(List<FileType.ASSET> assets) {
        for (FileType.ASSET asset : assets) {
            try {
                install(asset);
            } catch (IOException e) {
                // a missing map is worth saying out loud, but it must not stop the server from starting
                System.out.println("Could not install " + asset + ": " + e.getMessage());
            }
        }
    }

    /**
     * @param asset the asset to install
     */
    private void install(FileType.ASSET asset) throws IOException {
        String wanted = FileType.ASSET.getVersion(asset);
        File marker = new File(new File(directory, MARKER_DIRECTORY), asset.name());
        if (marker.isFile() && wanted.equals(Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim())) {
            return;
        }
        File archive = new FileHandler().provideFile(asset);
        System.out.println("Installing " + asset.getDisplayName() + " on " + directory.getName());
        unpack(archive, directory.toPath());
        marker.getParentFile().mkdirs();
        Files.writeString(marker.toPath(), wanted, StandardCharsets.UTF_8);
    }

    /**
     * Lays the contents of an archive over a directory.
     * <p>
     * Every path is resolved and then checked to still be inside the target. An archive with {@code ../} in
     * its names would otherwise write wherever it likes, and this one unpacks with the rights of the
     * launcher.
     *
     * @param archive the zip to unpack
     * @param target  where its contents belong
     */
    private static void unpack(File archive, Path target) throws IOException {
        Path root = target.toAbsolutePath().normalize();
        try (ZipFile zip = new ZipFile(archive)) {
            for (ZipEntry entry : zip.stream().toList()) {
                Path destination = root.resolve(entry.getName()).normalize();
                if (!destination.startsWith(root)) {
                    throw new IOException("The archive " + archive.getName()
                            + " wants to write outside the server directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }
                Files.createDirectories(destination.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
