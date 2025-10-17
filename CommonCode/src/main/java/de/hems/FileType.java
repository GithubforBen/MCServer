package de.hems;

public enum FileType {
    PAPER,
    VELOCITY;

    public static String getFileURL(FileType type) {
        return switch (type) {
            case PAPER -> "https://fill-data.papermc.io/v1/objects/1e92a8f0b1b0c393b3f3a7aa7b73f4940f18c0cea8730152217c6bcf409abe04/paper-1.21.10-76.jar";
            case VELOCITY -> "https://fill-data.papermc.io/v1/objects/dd3db88b177edeb49488d0526620807a3389cef5e3972615f963a889ff5d16f2/velocity-3.4.0-SNAPSHOT-546.jar";
        };
    }
    public static String getFileName(FileType type) {
        return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
    }
}
