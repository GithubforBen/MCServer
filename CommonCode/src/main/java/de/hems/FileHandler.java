package de.hems;

import de.hems.types.FileType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Provides the jars the launcher installs onto the servers: downloads them once into {@code ./downloads/}
 * and builds the plugins of this project when they are needed.
 */
public class FileHandler {

    /** The plugins of this project are only rebuilt once per launcher run. */
    private static boolean built = false;

    public FileHandler() {
        File file = new File("./downloads/");
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public File provideFile(FileType.SERVER type) {
        downloadIfNeeded(type);
        return new File("./downloads/" + FileType.SERVER.getFileName(type));
    }

    public File provideFile(FileType.PLUGIN type) throws IOException, InterruptedException {
        downloadIfNeeded(type);
        return new File("./downloads/" + FileType.PLUGIN.getFileName(type));
    }

    /**
     * Provides a content archive - a world and the configuration that belongs to it.
     * <p>
     * A shipped asset lies in {@code ./assets/} and is used from there, because it is part of this
     * repository rather than something that was fetched. Anything else is downloaded like a jar, so an
     * asset too big to keep in git can be moved to a url without a single caller changing.
     *
     * @param type the asset
     * @return the archive to unpack
     */
    public File provideFile(FileType.ASSET type) throws IOException {
        String name = FileType.ASSET.getFileName(type);
        if (FileType.ASSET.isShipped(type)) {
            File shipped = new File("./assets/" + name);
            if (!shipped.isFile()) {
                throw new IOException("The asset " + type + " is missing - expected " + shipped.getPath());
            }
            return shipped;
        }
        File file = new File("./downloads/" + name);
        if (!file.exists()) download(FileType.ASSET.getFileURL(type), file);
        return file;
    }

    public void downloadIfNeeded(FileType.SERVER type) {
        File file = new File("./downloads/" + FileType.SERVER.getFileName(type));
        if (file.exists()) {
            return;
        }
        try {
            download(FileType.SERVER.getFileURL(type), file);
        } catch (IOException e) {
            throw new RuntimeException("Could not download " + type, e);
        }
    }

    public void downloadIfNeeded(FileType.PLUGIN type) throws IOException, InterruptedException {
        File file = new File("./downloads/" + FileType.PLUGIN.getFileName(type));
        if (type.isBuildable()) {
            buildFile(type);
            File built = new File("./builds/" + type + "/" + FileType.PLUGIN.getFileName(type));
            if (!built.exists()) {
                throw new IOException("The plugin " + type + " was not built - expected " + built.getPath());
            }
            Files.copy(built.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        if (file.exists()) {
            return;
        }
        download(FileType.PLUGIN.getFileURL(type), file);
    }

    /**
     * Downloads a file, following redirects and only replacing the target once the download finished.
     *
     * @param url    where to download from
     * @param target where the file should end up
     */
    private void download(String url, File target) throws IOException {
        System.out.println("Downloading " + url);
        File temporary = new File(target.getPath() + ".part");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(60_000);
        connection.setRequestProperty("User-Agent", "MCServer-Launcher");
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, temporary.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Download complete: " + target.getName());
    }

    public void buildFile(FileType.PLUGIN type) throws IOException, InterruptedException {
        if (built) return;
        build();
        built = true;
    }

    private void build() throws IOException, InterruptedException {
        String file = "./mvnw";
        if (System.getProperty("os.name").toLowerCase().contains("windows")) file = file + ".cmd";
        ProcessBuilder pb = new ProcessBuilder(file, "clean", "install", "-DskipTests").directory(new File("./"));
        // Without JAVA_HOME the wrapper falls back to whatever "java" sits on the PATH, which is not
        // necessarily the JDK running the launcher. The plugins target the same release as this
        // application, so hand the build the exact JVM we are already running on.
        String javaHome = System.getProperty("java.home");
        pb.environment().put("JAVA_HOME", javaHome);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        int exitCode = pb.start().waitFor();
        if (exitCode != 0) {
            throw new IOException("Building the plugins failed with exit code " + exitCode
                    + " (Java " + System.getProperty("java.version") + " at " + javaHome
                    + ") - the cause is in the Maven output above");
        }
    }
}
