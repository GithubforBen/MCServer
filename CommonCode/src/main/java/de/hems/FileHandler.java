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

enum FileType {
    PAPER,
    VELOCITY;

    public static String getFileURL(FileType type) {
        return switch (type) {
            case PAPER -> "https://file.url/";//TODO: Insert correct urls here
            case VELOCITY -> "https://file.url/";
        };
    }
    public static String getFileName(FileType type) {
        //TODO: Fix potentially
        return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
    }
}
