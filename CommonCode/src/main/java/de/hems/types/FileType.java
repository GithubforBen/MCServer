package de.hems.types;

import java.io.Serializable;

public class FileType implements Serializable {
    private static final long serialVersionUID = 100L;
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
        WORLDEDIT,
        SIMPLE_VOICECHAT;
        public static String getFileURL(FileType.PLUGIN type) {
            return switch (type) {
                case SIMPLE_VOICECHAT -> "https://cdn.modrinth.com/data/9eGKb6K1/versions/ps3C3lpD/voicechat-bukkit-2.6.6.jar";//TODO: setup voicechat
                case PLUGIN.WORLDEDIT -> "https://www.dropbox.com/scl/fi/c4sqk7ralpgrmxnjiy2z2/worldedit-bukkit-7.3.17.jar?rlkey=diqdimwoz32xjb97mswbgxmat&st=535t6o3f&dl=0https://www.dropbox.com/scl/fi/c4sqk7ralpgrmxnjiy2z2/worldedit-bukkit-7.3.17.jar?rlkey=diqdimwoz32xjb97mswbgxmat&st=535t6o3f&dl=1";
            };
        }
        public static String getFileName(FileType.PLUGIN type) {
            if (type == PLUGIN.WORLDEDIT) return "worldedit-bukkit-7.3.17.jar";
            return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
        }
    }

}
