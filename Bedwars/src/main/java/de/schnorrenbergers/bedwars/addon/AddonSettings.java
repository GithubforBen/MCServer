package de.schnorrenbergers.bedwars.addon;

import de.schnorrenbergers.bedwars.util.ConfigFile;

/**
 * What {@code addons.yml} says about each addon.
 * <p>
 * Every addon gets its own block, written the first time it registers, so the file lists what is available
 * instead of only what somebody happened to type into it.
 */
public final class AddonSettings {

    private final ConfigFile file;

    public AddonSettings() {
        file = new ConfigFile("addons.yml");
        file.section("addons",
                "One block per addon. 'enabled' switches it, everything under 'settings' belongs to the",
                "addon itself. An event that starts this server can override 'enabled', and an operator",
                "can override that again in the waiting lobby.");
        file.save();
    }

    /**
     * Writes the block of an addon, without touching a setting that is already there.
     *
     * @param addon the addon to document
     */
    public void define(Addon addon) {
        String path = "addons." + addon.getId();
        file.get(path + ".enabled", addon.isDefaultEnabled(), addon.getDescription());
        file.section(path + ".settings");
        file.save();
    }

    /**
     * @param addon the addon to look up
     * @return whether the file switches it on
     */
    public boolean isEnabled(Addon addon) {
        return file.get("addons." + addon.getId() + ".enabled", addon.isDefaultEnabled());
    }

    /**
     * @param addonId the addon whose own settings are wanted
     * @return the path its settings live under in {@link #getFile()}
     */
    public static String settingsPath(String addonId) {
        return "addons." + addonId + ".settings";
    }

    public ConfigFile getFile() {
        return file;
    }

    public void reload() {
        file.reload();
    }
}
