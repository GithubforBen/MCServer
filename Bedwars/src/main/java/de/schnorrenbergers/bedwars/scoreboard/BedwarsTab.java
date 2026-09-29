package de.schnorrenbergers.bedwars.scoreboard;

import de.hems.paper.tablist.TabContent;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.phase.PhaseType;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What a bedwars round puts into the tab list: the map, your team and its bed, the teams still in, and
 * what you have done so far. Names are drawn in the colour of their team, so the list doubles as the
 * roster; spectators and players without a team are grey.
 * <p>
 * The sidebar already shows the beds of every team, so this is the per-player view next to it.
 */
public class BedwarsTab implements TabContent {

    private final Bedwars plugin;

    public BedwarsTab(Bedwars plugin) {
        this.plugin = plugin;
    }

    @Override
    public String mode() {
        return "Bedwars";
    }

    @Override
    public TextColor accent() {
        return NamedTextColor.RED;
    }

    @Override
    public List<Component> header(Player viewer) {
        ArenaMap arena = plugin.getGame().getArena();
        if (arena == null) return List.of();
        return List.of(Component.text("Map: ", NamedTextColor.GRAY)
                .append(Component.text(arena.getDisplayName(), NamedTextColor.WHITE)));
    }

    @Override
    public List<Component> footer(Player viewer) {
        Game game = plugin.getGame();
        List<Component> lines = new ArrayList<>();
        if (game.getPhaseType() == PhaseType.LOBBY) {
            lines.add(Component.text("Warten auf den Start  ·  " + game.getOnlineCount() + " Spieler",
                    NamedTextColor.YELLOW));
            return lines;
        }
        GamePlayer self = game.get(viewer);
        GameTeam team = self == null ? null : self.getTeam();
        if (team != null) {
            lines.add(Component.text("Dein Team: ", NamedTextColor.GRAY).append(team.getDisplayName())
                    .append(Component.text("  ·  Bett ", NamedTextColor.GRAY))
                    .append(team.isBedAlive() ? Component.text("✔", NamedTextColor.GREEN)
                            : Component.text("✘", NamedTextColor.RED)));
        } else {
            lines.add(Component.text("Du schaust zu", NamedTextColor.GRAY));
        }
        lines.add(Component.text("Teams im Spiel: ", NamedTextColor.GRAY)
                .append(Component.text(game.getAliveTeams().size(), NamedTextColor.WHITE)));
        if (self != null) {
            lines.add(Component.text("Kills ", NamedTextColor.GRAY)
                    .append(Component.text(self.getKills(), NamedTextColor.WHITE))
                    .append(Component.text("  ·  Finals ", NamedTextColor.GRAY))
                    .append(Component.text(self.getFinalKills(), NamedTextColor.WHITE))
                    .append(Component.text("  ·  Betten ", NamedTextColor.GRAY))
                    .append(Component.text(self.getBedsBroken(), NamedTextColor.WHITE)));
        }
        return lines;
    }

    @Override
    public @Nullable String hint() {
        return "/lobby";
    }

    @Override
    public @Nullable Component listName(Player player) {
        GamePlayer gamePlayer = plugin.getGame().get(player);
        GameTeam team = gamePlayer == null ? null : gamePlayer.getTeam();
        if (team == null || gamePlayer.isSpectator()) return Component.text(player.getName(), NamedTextColor.GRAY);
        return Component.text(player.getName(), team.getColor().getTextColor());
    }
}
