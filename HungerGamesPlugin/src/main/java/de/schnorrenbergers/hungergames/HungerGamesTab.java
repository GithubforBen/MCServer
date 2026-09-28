package de.schnorrenbergers.hungergames;

import de.hems.paper.tablist.TabContent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What the arena puts into the tab list: the phase, and how many are still standing. The dead are drawn in
 * grey, so the list is the list of who is left.
 */
public class HungerGamesTab implements TabContent {

    private final Game game;

    public HungerGamesTab(Game game) {
        this.game = game;
    }

    @Override
    public String mode() {
        return "Hunger Games";
    }

    @Override
    public TextColor accent() {
        return NamedTextColor.GOLD;
    }

    @Override
    public List<Component> footer(Player viewer) {
        return switch (game.getState()) {
            case WAITING -> List.of(Component.text("Warten auf den Start", NamedTextColor.YELLOW));
            case COUNTDOWN -> List.of(Component.text("Gleich geht es los", NamedTextColor.YELLOW));
            case RUNNING -> List.of(
                    Component.text("Phase: ", NamedTextColor.GRAY)
                            .append(Component.text(game.getPhase().getTitle(), NamedTextColor.WHITE)),
                    Component.text("Noch am Leben: ", NamedTextColor.GRAY)
                            .append(Component.text(game.aliveCount(), NamedTextColor.WHITE))
                            .append(game.isAlive(viewer.getUniqueId()) ? Component.empty()
                                    : Component.text("  ·  du schaust zu", NamedTextColor.GRAY)));
            case ENDED -> List.of(Component.text("Das Spiel ist vorbei", NamedTextColor.GRAY));
        };
    }

    @Override
    public @Nullable String hint() {
        return "/lobby";
    }

    @Override
    public @Nullable Component listName(Player player) {
        if (game.getState() != Game.State.RUNNING) return Component.text(player.getName(), NamedTextColor.WHITE);
        return Component.text(player.getName(),
                game.isAlive(player.getUniqueId()) ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY);
    }
}
