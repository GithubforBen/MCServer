package de.hems.paper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.plugin.Plugin;

/**
 * Writes what our own plugins do to the world into CoreProtect, so {@code /co inspect} and a rollback see
 * it like anything a player did by hand.
 * <p>
 * CoreProtect only notices what goes through a Bukkit event. A shop taking items out of its chest, or a
 * gadget planting a seed, changes the world directly - without this, a rollback would not know about it and
 * an inspection would show the chest with items nobody took out.
 * <p>
 * Every call is safe on a server without CoreProtect: it does nothing. The CoreProtect classes are only
 * touched once the plugin is known to be there, so this class can be loaded anywhere.
 */
public final class CoreProtectLog {

    private CoreProtectLog() {
    }

    /**
     * Logs a change to a container. Has to be called <b>before</b> the contents change: CoreProtect takes a
     * picture now and writes down the difference a moment later.
     *
     * @param user     who the change is put down to
     * @param location where the container is
     */
    public static void container(String user, Location location) {
        if (!available() || user == null || location == null) return;
        try {
            Api.container(user, location);
        } catch (LinkageError | RuntimeException e) {
            warn(e);
        }
    }

    /**
     * Logs a block that was put into the world, after it is there.
     *
     * @param user  who the change is put down to
     * @param state the block as it is now
     */
    public static void placed(String user, BlockState state) {
        if (!available() || user == null || state == null) return;
        try {
            Api.placed(user, state);
        } catch (LinkageError | RuntimeException e) {
            warn(e);
        }
    }

    /**
     * Logs a block that was taken out of the world. Has to be called <b>before</b> it changes, with the
     * block as it was.
     *
     * @param user  who the change is put down to
     * @param state the block as it was
     */
    public static void removed(String user, BlockState state) {
        if (!available() || user == null || state == null) return;
        try {
            Api.removed(user, state);
        } catch (LinkageError | RuntimeException e) {
            warn(e);
        }
    }

    private static boolean available() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
        return plugin != null && plugin.isEnabled();
    }

    private static void warn(Throwable e) {
        Bukkit.getLogger().warning("Could not write to CoreProtect: " + e);
    }

    /** The only place that names CoreProtect's classes, loaded on first use only. */
    private static final class Api {

        private static net.coreprotect.CoreProtectAPI api() {
            return de.hems.api.CoreProtectAPI.getCoreProtect();
        }

        static void container(String user, Location location) {
            net.coreprotect.CoreProtectAPI api = api();
            if (api != null) api.logContainerTransaction(user, location);
        }

        static void placed(String user, BlockState state) {
            net.coreprotect.CoreProtectAPI api = api();
            if (api != null) api.logPlacement(user, state);
        }

        static void removed(String user, BlockState state) {
            net.coreprotect.CoreProtectAPI api = api();
            if (api != null) api.logRemoval(user, state);
        }
    }
}
