package de.hems;

public enum FileType {
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
