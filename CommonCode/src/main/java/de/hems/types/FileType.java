package de.hems.types;

import java.io.Serializable;

public class FileType implements Serializable {
    private static final long serialVersionUID = 100L;
    public enum SERVER {
        PAPER,
        VELOCITY;
        public static String getFileURL(FileType.SERVER type) {
            return switch (type) {
                case SERVER.PAPER -> "https://fill-data.papermc.io/v1/objects/d5f47f6393aa647759f101f02231fa8200e5bccd36081a3ee8b6a5fd96739057/paper-1.21.10-115.jar";
                case SERVER.VELOCITY -> "https://fill-data.papermc.io/v1/objects/dd3db88b177edeb49488d0526620807a3389cef5e3972615f963a889ff5d16f2/velocity-3.4.0-SNAPSHOT-546.jar";
            };
        }
        public static String getFileName(FileType.SERVER type) {
            return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
        }
    }
    public enum PLUGIN {
        WORLDEDIT,
		CORE_PROTECT,
        SIMPLE_VOICECHAT;
        public static String getFileURL(FileType.PLUGIN type) {
            return switch (type) {
                case SIMPLE_VOICECHAT -> "https://cdn.modrinth.com/data/9eGKb6K1/versions/ps3C3lpD/voicechat-bukkit-2.6.6.jar";//TODO: setup voicechat
                case PLUGIN.WORLDEDIT -> "https://www.dropbox.com/scl/fi/c4sqk7ralpgrmxnjiy2z2/worldedit-bukkit-7.3.17.jar?rlkey=diqdimwoz32xjb97mswbgxmat&st=535t6o3f&dl=0https://www.dropbox.com/scl/fi/c4sqk7ralpgrmxnjiy2z2/worldedit-bukkit-7.3.17.jar?rlkey=diqdimwoz32xjb97mswbgxmat&st=535t6o3f&dl=1";
				case PLUGIN.CORE_PROTECT -> "https://drive.google.com/uc?export=download&id=1uMlT0X8bzOyhr7lNBY8K-1Md6URNiIE1";
            };
        }
        public static String getFileName(FileType.PLUGIN type) {
            if (type == PLUGIN.WORLDEDIT) return "worldedit-bukkit-7.3.17.jar";
			if (type == PLUGIN.CORE_PROTECT) return "coreprotect.jar";
            return getFileURL(type).split("/")[getFileURL(type).split("/").length - 1];
        }
    }

}
