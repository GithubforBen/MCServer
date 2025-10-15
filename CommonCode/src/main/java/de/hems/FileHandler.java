package de.hems;

import java.io.File;

public class FileHandler {

    public FileHandler() {
        File file = new File("./downloads/");
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public File provideFile(FileType type) {
        downloadIfNeeded(type);
        return new File("./downloads/" + FileType.getFileName(type));
    }

    public void downloadIfNeeded(FileType type) {
        File file = new File("./downloads/" + FileType.getFileName(type));
        if (file.exists()) {
            return;
        }
        //TODO: download logic

    }
}

