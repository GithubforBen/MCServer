package de.hems.api;

import net.coreprotect.CoreProtect;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class CoreProtectAPI {
    public static net.coreprotect.CoreProtectAPI getCoreProtect() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (plugin == null || !(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        net.coreprotect.CoreProtectAPI coreProtect = ((CoreProtect) plugin).getAPI();
        if (!coreProtect.isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (coreProtect.APIVersion() < 10) {
            return null;
        }

        return coreProtect;
    }
}
