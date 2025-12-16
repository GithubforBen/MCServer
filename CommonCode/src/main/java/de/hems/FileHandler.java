package de.hems;

import de.hems.types.FileType;
import org.eclipse.aether.util.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileHandler {

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

    public void downloadIfNeeded(FileType.SERVER type) {
        File file = new File("./downloads/" + FileType.SERVER.getFileName(type));
        if (file.exists()) {
            return;
        }
        System.out.println("Downloading " + type );
        try {
            BufferedInputStream in = new BufferedInputStream(new URL(FileType.SERVER.getFileURL(type)).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Download complete");
    }
    public void downloadIfNeeded(FileType.PLUGIN type) throws IOException, InterruptedException {//TODO: add a way that the project builds itself and provides the plugins to the application
        File file = new File("./downloads/" + FileType.PLUGIN.getFileName(type));
        if (type.isBuildable()) buildFile(type);
        if (file.exists()) {
            return;
        }
        if (type.isBuildable()) {
            File f = new File("./builds/"+ type.toString() + "/" + FileType.PLUGIN.getFileName(type));
            if (!f.exists()) {
                buildFile(type);
            }
            Files.copy(f.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        System.out.println("Downloading " + type );
        try {
            BufferedInputStream in = new BufferedInputStream(new URL(FileType.PLUGIN.getFileURL(type)).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Download complete");
    }

    public void buildFile(FileType.PLUGIN type) throws IOException, InterruptedException {
        File buildDir = new File("./builds/");
        if (!buildDir.exists()) {
            build();
            return;
        }
        File check = new File("./builds/"+ type.toString() + "/buidtime.txt");
        if (check.exists()) {
            long lastModified = check.lastModified();
            long now = System.currentTimeMillis();
            if (now - lastModified < 1000 * 60 * 60) {
                build();
                check.setLastModified(System.currentTimeMillis());
            }
        } else {
            build();
            check.createNewFile();
        }
    }

    private void build() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("./mvnw", "clean", "install").directory(new File("./"));
        pb.start().waitFor();
    }
}

