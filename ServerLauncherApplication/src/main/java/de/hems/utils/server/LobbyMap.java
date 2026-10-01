package de.hems.utils.server;

import de.hems.files.FileTrees;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Puts the built lobby in place before the lobby server starts.
 * <p>
 * The lobby is a map, not a world anybody plays in: whatever happens to it during a day should be gone the
 * next time the server comes up. The lobby plugin used to restore it itself, but the lobby is the server's
 * main world, and a running server cannot unload its main world - so the restore only ever warned and kept
 * what was there. Done here, while the server is still off, it simply works.
 * <p>
 * The map lies in {@code lobby-world/} inside the server directory, where the {@code LOBBY_SPAWN} asset
 * unpacks it on the first start; an admin who rebuilt the spawn puts the new map there. The plugin's own
 * {@code lobby.restore-on-start} still decides - set to {@code false}, the world is left alone.
 */
public final class LobbyMap {

    private static final String SOURCE = "lobby-world";
    private static final String TARGET = "world";

    private LobbyMap() {
    }

    /**
     * @param serverDirectory the lobby server's directory
     */
    public static void installInto(File serverDirectory) {
        File source = new File(serverDirectory, SOURCE);
        if (!new File(source, "level.dat").isFile()) return;
        if (!restoreWanted(serverDirectory)) return;
        File target = new File(serverDirectory, TARGET);
        try {
            FileTrees.delete(target.toPath());
            // the copy must not carry the map's identity, or the server takes it for a world it already saw
            FileTrees.copy(source.toPath(), target.toPath(), FileTrees.WORLD_IDENTITY);
            FileTrees.stripPlayers(target);
            System.out.println("Lobby world restored from " + SOURCE + "/");
        } catch (IOException e) {
            System.out.println("Could not restore the lobby world: " + e.getMessage() + " - keeping what is there.");
        }
    }

    private static boolean restoreWanted(File serverDirectory) {
        File config = new File(serverDirectory, "plugins/LobbyPlugin/config.yml");
        if (!config.isFile()) return true;
        return YamlConfiguration.loadConfiguration(config).getBoolean("lobby.restore-on-start", true);
    }
}
