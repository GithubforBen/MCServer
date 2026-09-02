package de.hems.utils.bot.verification;

import de.hems.Main;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

/**
 * The one discord account that may do everything.
 * <p>
 * It used to be a number typed into the middle of one command. Now that a second command needs the same
 * answer - handing out operator rights is not something a moderator role should be able to do quietly -
 * it lives in one place, and in the config, so the network can change hands without a new build.
 */
public final class DiscordOwner {

    /** The key in the launcher config. */
    public static final String CONFIG_KEY = "discord-owner-id";
    /** Who it was before it was configurable, so an existing network keeps working untouched. */
    public static final String DEFAULT_ID = "668439460819632143";

    private DiscordOwner() {
    }

    /**
     * @return the discord id of whoever owns this network
     */
    public static String id() {
        Main main = Main.getInstance();
        if (main == null || main.getConfiguration() == null) return DEFAULT_ID;
        YamlConfiguration config = main.getConfiguration().getConfig();
        if (!config.contains(CONFIG_KEY)) {
            config.set(CONFIG_KEY, DEFAULT_ID);
            config.setComments(CONFIG_KEY, List.of(
                    "The discord account that owns this network. It may hand out operator rights and use",
                    "every owner only command of the bot. Everything else goes by discord's own roles."));
            main.getConfiguration().save();
        }
        String configured = config.getString(CONFIG_KEY, DEFAULT_ID);
        return configured == null || configured.isBlank() ? DEFAULT_ID : configured.trim();
    }

    /**
     * @param user somebody using a command
     * @return whether they own this network
     */
    public static boolean is(User user) {
        return user != null && id().equals(user.getId());
    }
}
