package de.schnorrenbergers.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * The one world the lobby runs in.
 * <p>
 * There is exactly one lobby world, and it is meant to be a built map rather than generated terrain. So it
 * is kept as a template on disk and copied into place, which means the running world can be thrown away and
 * restored at any time and every start looks the same.
 * <p>
 * Until a real map is dropped in, the template directory is simply not there and whatever world the server
 * already has is used as it stands - that is the placeholder, and it needs no configuration to work.
 */
public final class LobbyWorld {

    /** Where a built lobby map is dropped in. */
    private static final String DEFAULT_SOURCE = "./lobby-world";

    private static World world;

    private LobbyWorld() {
    }

    /**
     * Works out which world the lobby lives in, restoring it from the template if there is one.
     *
     * @param plugin the lobby plugin, for its config and its logger
     */
    public static void load(LobbyPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        config.addDefault("lobby.world", worldName(plugin));
        config.addDefault("lobby.source", DEFAULT_SOURCE);
        // a built map does not change, so restoring it on every start keeps the lobby tidy by itself
        config.addDefault("lobby.restore-on-start", true);
        config.options().copyDefaults(true);
        plugin.saveConfig();

        String name = config.getString("lobby.world", worldName(plugin));
        File source = new File(config.getString("lobby.source", DEFAULT_SOURCE));
        boolean restore = config.getBoolean("lobby.restore-on-start", true);

        if (source.isDirectory()) {
            if (restore || Bukkit.getWorld(name) == null) {
                restore(plugin, source, name);
            }
        } else {
            plugin.getLogger().info("No lobby map at " + source.getPath()
                    + " - using the world that is already here.");
        }

        world = Bukkit.getWorld(name);
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(name));
        }
        if (world == null) {
            plugin.getLogger().warning("The lobby world '" + name + "' could not be loaded.");
            return;
        }
        plugin.getLogger().info("Lobby world is " + world.getName());
    }

    /**
     * Copies the template over the running world.
     *
     * @param plugin the plugin, for logging
     * @param source the template directory
     * @param name   the world to replace
     */
    private static void restore(LobbyPlugin plugin, File source, String name) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            // players have to be out of a world before it can be unloaded
            for (Player player : existing.getPlayers()) {
                player.kick(net.kyori.adventure.text.Component.text("Die Lobby wird geladen."));
            }
            if (!Bukkit.unloadWorld(existing, false)) {
                plugin.getLogger().warning("Could not unload '" + name + "' - keeping what is there.");
                return;
            }
        }
        File target = new File(Bukkit.getWorldContainer(), name);
        try {
            if (target.exists()) deleteTree(target.toPath());
            copyTree(source.toPath(), target.toPath());
            // a copied world carries the identity of the one it came from, which confuses the server
            new File(target, "uid.dat").delete();
            new File(target, "session.lock").delete();
            plugin.getLogger().info("Restored the lobby world from " + source.getPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not restore the lobby world: " + e.getMessage());
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * @param plugin the plugin
     * @return the name of the world the server came up with, used as the default
     */
    private static String worldName(LobbyPlugin plugin) {
        return Bukkit.getWorlds().isEmpty() ? "world" : Bukkit.getWorlds().getFirst().getName();
    }

    /**
     * @return the lobby world, or {@code null} if it could not be loaded
     */
    public static World get() {
        return world;
    }

    /**
     * @return where a player belongs when they arrive in the lobby
     */
    public static Location spawn() {
        return world == null ? null : world.getSpawnLocation();
    }

    /**
     * Puts a player where they belong. There is only one lobby world, so anybody standing anywhere else
     * has ended up there by accident.
     *
     * @param player who to place
     */
    public static void place(Player player) {
        Location spawn = spawn();
        if (spawn == null) return;
        if (!player.getWorld().equals(world)) {
            player.teleport(spawn);
            return;
        }
        // a fresh arrival goes to spawn as well, the lobby is not a place you keep a position in
        player.teleport(spawn);
    }
}
