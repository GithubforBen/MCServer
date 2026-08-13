package de.hems.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Every jar the launcher has to put onto a server, together with the place it is fetched from.
 * <p>
 * All downloads are pinned to builds that support {@link #MINECRAFT_VERSION}.
 */
public class FileType implements Serializable {
    private static final long serialVersionUID = 100L;

    /** The minecraft version the whole network is built and configured for. */
    public static final String MINECRAFT_VERSION = "26.2";
    /** The value plugins have to declare as {@code api-version} in their plugin.yml. */
    public static final String API_VERSION = "26.2";

    public enum SERVER {
        PAPER,
        VELOCITY;

        public static String getFileURL(SERVER type) {
            return switch (type) {
                // Paper 26.2 build 112 (stable)
                case SERVER.PAPER ->
                        "https://fill-data.papermc.io/v1/objects/bd3a58cf96874e5ea6643f5f6fe9b4f5bf9e34b795fa078c2f0ee8b98b2f907e/paper-26.2-112.jar";
                // Velocity 3.5.1 build 615 (recommended, speaks the 26.2 protocol)
                case SERVER.VELOCITY ->
                        "https://fill-data.papermc.io/v1/objects/b4e3164df5377346854dc6cb9e6a78022b1946ff69e89676313f5f6f1c6f0fb3/velocity-3.5.1-615.jar";
            };
        }

        public static String getFileName(SERVER type) {
            String url = getFileURL(type);
            return url.split("/")[url.split("/").length - 1];
        }

        /**
         * @return the name shown to players when they pick the software of a new server
         */
        public String getDisplayName() {
            return switch (this) {
                case PAPER -> "Paper " + MINECRAFT_VERSION;
                case VELOCITY -> "Velocity Proxy";
            };
        }
    }

    /**
     * Where a plugin can be installed. Used to only offer plugins that actually fit the server software.
     */
    public enum PLATFORM {
        PAPER,
        VELOCITY
    }

    public enum PLUGIN {
        SURVIVAL,
        LOBBY,
        BEDWARS,
        VELOCITY,
        WORLDEDIT,
        CORE_PROTECT,
        CHUNKY,
        SIMPLE_VOICECHAT_PAPER,
        SIMPLE_VOICECHAT_VELOCITY,
        WORLD_GUARD;

        public static String getFileURL(PLUGIN type) {
            return switch (type) {
                case SURVIVAL -> "build:/survival-1.0.jar";
                case LOBBY -> "build:/lobby-1.0.jar";
                case BEDWARS -> "build:/bedwars-1.0.jar";
                case VELOCITY -> "build:/velocityplugin-1.0.jar";
                case WORLDEDIT ->
                        "https://cdn.modrinth.com/data/1u6JkXh5/versions/F5ea2ov3/worldedit-bukkit-7.4.5.jar";
                case WORLD_GUARD ->
                        "https://cdn.modrinth.com/data/DKY9btbd/versions/btHBavWa/worldguard-bukkit-7.0.18.jar";
                case CORE_PROTECT ->
                        "https://cdn.modrinth.com/data/Lu3KuzdV/versions/Kma0kBsY/CoreProtect-CE-24.0.jar";
                case CHUNKY ->
                        "https://cdn.modrinth.com/data/fALzjamp/versions/MdY6JATr/Chunky-Bukkit-1.5.3.jar";
                case SIMPLE_VOICECHAT_PAPER ->
                        "https://cdn.modrinth.com/data/9eGKb6K1/versions/62MVmInV/voicechat-bukkit-2.6.21.jar";
                case SIMPLE_VOICECHAT_VELOCITY ->
                        "https://cdn.modrinth.com/data/9eGKb6K1/versions/ES87t4lm/voicechat-velocity-2.6.18.jar";
            };
        }

        public static String getFileName(PLUGIN type) {
            String url = getFileURL(type);
            return url.split("/")[url.split("/").length - 1];
        }

        public boolean isBuildable() {
            return getFileURL(this).startsWith("build:");
        }

        /**
         * @return the server software this plugin has to be installed on
         */
        public PLATFORM getPlatform() {
            return switch (this) {
                case VELOCITY, SIMPLE_VOICECHAT_VELOCITY -> PLATFORM.VELOCITY;
                default -> PLATFORM.PAPER;
            };
        }

        /**
         * @param software the software of the server the plugin should run on
         * @return whether the plugin can be installed on that software
         */
        public boolean supports(SERVER software) {
            return switch (getPlatform()) {
                case VELOCITY -> software == SERVER.VELOCITY;
                case PAPER -> software == SERVER.PAPER;
            };
        }

        /**
         * @return the name shown in the plugin selection UI
         */
        public String getDisplayName() {
            return switch (this) {
                case SURVIVAL -> "Survival";
                case LOBBY -> "Lobby";
                case BEDWARS -> "Bedwars";
                case VELOCITY -> "Netzwerk Proxy Plugin";
                case WORLDEDIT -> "WorldEdit";
                case WORLD_GUARD -> "WorldGuard";
                case CORE_PROTECT -> "CoreProtect";
                case CHUNKY -> "Chunky";
                case SIMPLE_VOICECHAT_PAPER -> "Simple Voicechat";
                case SIMPLE_VOICECHAT_VELOCITY -> "Simple Voicechat (Proxy)";
            };
        }

        /**
         * @return a short explanation shown as lore in the plugin selection UI
         */
        public String getDescription() {
            return switch (this) {
                case SURVIVAL -> "Das komplette Survival Spielsystem";
                case LOBBY -> "Lobby Features, Parkour und Server Manager";
                case BEDWARS -> "Bedwars Minispiel";
                case VELOCITY -> "Verbindet den Proxy mit dem Netzwerk";
                case WORLDEDIT -> "Welten schnell bearbeiten";
                case WORLD_GUARD -> "Regionen schuetzen";
                case CORE_PROTECT -> "Block Logging und Rollback";
                case CHUNKY -> "Welt vorgenerieren";
                case SIMPLE_VOICECHAT_PAPER -> "Proximity Voicechat";
                case SIMPLE_VOICECHAT_VELOCITY -> "Voicechat Unterstuetzung auf dem Proxy";
            };
        }

        /**
         * @param software the software of the server
         * @return every plugin that can be installed on that software
         */
        public static List<PLUGIN> selectableFor(SERVER software) {
            List<PLUGIN> plugins = new ArrayList<>();
            for (PLUGIN plugin : values()) {
                if (plugin.supports(software)) plugins.add(plugin);
            }
            return plugins;
        }
    }
}
