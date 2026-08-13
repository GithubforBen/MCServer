package de.hems.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Gives the shared paper code access to the plugin it is running inside, which is needed to schedule work
 * and to talk to the proxy. It is set automatically as soon as a plugin registers the custom inventories.
 */
public final class PaperContext {

    private static Plugin plugin;

    private PaperContext() {
    }

    public static void setPlugin(Plugin plugin) {
        if (PaperContext.plugin == null) PaperContext.plugin = plugin;
    }

    public static Plugin getPlugin() {
        if (plugin == null) {
            throw new IllegalStateException("No plugin registered yet - create a CustomInventoryListener first");
        }
        return plugin;
    }

    public static boolean hasPlugin() {
        return plugin != null;
    }

    /**
     * Runs work that talks to the host off the main thread, so the server never freezes while waiting.
     *
     * @param runnable the work to run
     */
    public static void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), runnable);
    }

    /**
     * Runs work that touches players or inventories back on the main thread.
     *
     * @param runnable the work to run
     */
    public static void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(getPlugin(), runnable);
    }
}
