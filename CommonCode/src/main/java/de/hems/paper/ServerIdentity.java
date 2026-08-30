package de.hems.paper;

import de.hems.communication.ListenerAdapter;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Works out which server of the network a plugin is running on.
 * <p>
 * Servers are created on the fly and named by whoever created them, so a plugin can not have its name
 * compiled in. The launcher starts every server in a directory called after it, which makes the working
 * directory the one thing that always carries the name.
 */
public final class ServerIdentity {

    private ServerIdentity() {
    }

    /**
     * @param plugin   the plugin asking
     * @param fallback the name to use when the directory does not say anything useful
     * @return the name this server is known by in the network
     */
    public static ListenerAdapter.ServerName of(Plugin plugin, String fallback) {
        try {
            return ListenerAdapter.ServerName.valueOf(nameOf(plugin, fallback));
        } catch (IllegalArgumentException e) {
            // a directory whose name normalises to nothing is still no reason to leave the plugin off
            plugin.getLogger().warning("'" + nameOf(plugin, fallback) + "' is not a usable server name, using "
                    + fallback + " instead.");
            return ListenerAdapter.ServerName.valueOf(fallback);
        }
    }

    /**
     * @param plugin   the plugin asking
     * @param fallback the name to use when the directory does not say anything useful
     * @return the raw name, before it is normalised
     */
    public static String nameOf(Plugin plugin, String fallback) {
        File container = plugin.getServer().getWorldContainer();
        try {
            // canonical, not absolute: a world container of "." keeps its dot through getAbsoluteFile(),
            // and the server would try to register itself under the name "." - which is not a usable name,
            // so the plugin would throw out of onEnable and stay disabled for the life of the server
            container = container.getCanonicalFile();
        } catch (IOException e) {
            container = container.getAbsoluteFile();
        }
        String directory = container.getName();
        if (directory == null || directory.isBlank() || ".".equals(directory) || "..".equals(directory)) {
            return fallback;
        }
        return directory;
    }
}
