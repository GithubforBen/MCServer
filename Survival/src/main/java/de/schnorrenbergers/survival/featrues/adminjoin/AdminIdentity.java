package de.schnorrenbergers.survival.featrues.adminjoin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Who the admin becomes: a name and a skin, both out of the config.
 * <p>
 * The skin is borrowed from a real Minecraft account, named in {@code admin-join.yml}. It is fetched once
 * when the server starts and then kept - Mojang is asked once a restart rather than once a disguise,
 * because a command that waits on somebody else's web service is a command that sometimes does nothing.
 * <p>
 * Without a fetched skin the disguise does not happen at all. Half a disguise - the right name over the
 * wrong skin - is worse than none: it is exactly the tell that gives an admin away, and it would give
 * them away without them knowing.
 */
public final class AdminIdentity {

    /** Where the name and the account to borrow from are set. Settings only - the disguises
     * themselves are the service's, in the plugin's own folder. */
    private static final String FILE = "./configs/admin-join.yml";
    private static final String DEFAULT_NAME = "Admin";

    private static String displayName = DEFAULT_NAME;
    private static String skinAccount = DEFAULT_NAME;
    private static volatile Set<ProfileProperty> skin;

    private AdminIdentity() {
    }

    /**
     * Reads the config and starts fetching the skin.
     *
     * @param plugin the plugin the fetch runs on
     */
    public static void init(Plugin plugin) {
        java.io.File file = new java.io.File(FILE);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        displayName = config.getString("display-name", DEFAULT_NAME);
        skinAccount = config.getString("skin-account", displayName);
        if (!config.contains("skin-account")) {
            // written out on the first start, so the two things somebody actually wants to change are
            // in the file rather than in this class
            config.set("display-name", displayName);
            config.set("skin-account", skinAccount);
            try {
                file.getParentFile().mkdirs();
                config.save(file);
            } catch (java.io.IOException e) {
                plugin.getLogger().warning("Could not write admin-join.yml: " + e.getMessage());
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> fetch(plugin));
    }

    /**
     * @return the name the disguise wears
     */
    public static String getDisplayName() {
        return displayName;
    }

    /**
     * @return whether the skin arrived, which is what the command needs before it can do anything
     */
    public static boolean isReady() {
        return skin != null && !skin.isEmpty();
    }

    /**
     * @return the account the skin is borrowed from, for the message that says it is missing
     */
    public static String getSkinAccount() {
        return skinAccount;
    }

    /**
     * Puts the disguise on somebody.
     * <p>
     * The profile keeps its uuid and swaps its name and its textures. That way round on purpose:
     * everything of this network that matters - money, teams, cosmetics, bans - is stored under the
     * uuid, so an admin in disguise is still themselves to every one of them.
     *
     * @param player who
     */
    public static void wear(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.setName(displayName);
        profile.setProperties(skin);
        player.setPlayerProfile(profile);
        player.displayName(net.kyori.adventure.text.Component.text(displayName));
        player.playerListName(net.kyori.adventure.text.Component.text(displayName));
    }

    /**
     * Takes it off again.
     *
     * @param player   who
     * @param original the profile they logged in with
     */
    public static void take(Player player, PlayerProfile original) {
        player.setPlayerProfile(original);
        player.displayName(net.kyori.adventure.text.Component.text(original.getName()));
        player.playerListName(net.kyori.adventure.text.Component.text(original.getName()));
    }

    /**
     * Asks Mojang what the borrowed account looks like. Off the main thread, once per start.
     */
    private static void fetch(Plugin plugin) {
        try {
            PlayerProfile profile = Bukkit.createProfile(skinAccount);
            // the textures are what is wanted, and they only come with the full lookup - the cache alone
            // answers for accounts that have been on this server before, which the borrowed one has not
            if (!profile.complete(true) || profile.getProperties().isEmpty()) {
                plugin.getLogger().warning("Could not fetch the admin skin of " + skinAccount
                        + " - /admin join stays off until the next restart.");
                return;
            }
            skin = new LinkedHashSet<>(profile.getProperties());
            plugin.getLogger().info("Admin skin loaded from " + skinAccount + ".");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not fetch the admin skin: " + e.getMessage());
        }
    }
}
