package de.hems;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

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
}

