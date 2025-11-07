package de.hems;

public class FileType {
    public enum SERVER {
        PAPER,
        VELOCITY;
        public static String getFileURL(FileType.SERVER type) {
            return switch (type) {
                case SERVER.PAPER -> "https://fill-data.papermc.io/v1/objects/0a1cf5ed22d69fae2edaf263f76d44d968288328ad97a48385f6fd1336ebc031/paper-1.21.10-104.jar";
                case SERVER.VELOCITY -> "https://fill-data.papermc.io/v1/objects/dd3db88b177edeb49488d0526620807a3389cef5e3972615f963a889ff5d16f2/velocity-3.4.0-SNAPSHOT-546.jar";
            };
        }
        public static String getFileName(FileType.SERVER type) {
            return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
        }
    }
    public enum PLUGIN {
        WORLDEDIT;
        public static String getFileURL(FileType.PLUGIN type) {
            return switch (type) {
                case PLUGIN.WORLDEDIT -> "https://dev.bukkit.org/projects/worldedit/files/7150859/download";
            };
        }
        public static String getFileName(FileType.PLUGIN type) {
            if (type == PLUGIN.WORLDEDIT) return "worldedit-bukkit-7.3.17.jar";
            return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
        }
    }

}
