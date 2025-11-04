package de.hems;

public enum FileType {
    PAPER,
    VELOCITY;

    public static String getFileURL(FileType type) {
        return switch (type) {
            case PAPER -> "https://fill-data.papermc.io/v1/objects/0a1cf5ed22d69fae2edaf263f76d44d968288328ad97a48385f6fd1336ebc031/paper-1.21.10-104.jar";
            case VELOCITY -> "https://fill-data.papermc.io/v1/objects/dd3db88b177edeb49488d0526620807a3389cef5e3972615f963a889ff5d16f2/velocity-3.4.0-SNAPSHOT-546.jar";
        };
    }
    public static String getFileName(FileType type) {
        return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
    }
}
