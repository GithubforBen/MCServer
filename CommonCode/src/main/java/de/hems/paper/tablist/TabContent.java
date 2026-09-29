package de.hems.paper.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What one kind of server puts into the tab list, around the frame every server shares.
 * <p>
 * {@link TabList} draws the name of the network, which server this is, how many are online and what the
 * events are doing. Everything a game mode knows better than that - the bits and the team on survival,
 * the map and the teams on bedwars, who is still alive in the arena - comes from here.
 */
public interface TabContent {

    /**
     * @return the name of the game mode, shown in the header
     */
    String mode();

    /**
     * @return the colour of this mode, used for its name and the lines around it
     */
    TextColor accent();

    /**
     * @param viewer who is looking
     * @return lines under the header, empty for none
     */
    default List<Component> header(Player viewer) {
        return List.of();
    }

    /**
     * @param viewer who is looking
     * @return the lines of this mode in the footer, empty for none
     */
    default List<Component> footer(Player viewer) {
        return List.of();
    }

    /**
     * @return the commands worth knowing here, shown small at the very bottom - {@code null} for none
     */
    default @Nullable String hint() {
        return null;
    }

    /**
     * @param player a player in the list
     * @return how their name is shown in the list, or {@code null} to leave it as it is - survival, for
     *         one, shows the team tag through the scoreboard and must not be overwritten
     */
    default @Nullable Component listName(Player player) {
        return null;
    }
}
