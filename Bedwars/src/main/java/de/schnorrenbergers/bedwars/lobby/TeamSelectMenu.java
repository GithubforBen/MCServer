package de.schnorrenbergers.bedwars.lobby;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Equipment;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The wool menu of the waiting lobby.
 * <p>
 * One block per team, in the team's colour, with everybody who is already in it underneath. Picking is
 * never required - whoever does not pick is put somewhere sensible when the round starts.
 */
public final class TeamSelectMenu {

    /** One row is enough: a round has at most eight teams. */
    private static final int SIZE = 9;

    private TeamSelectMenu() {
    }

    /**
     * @param player who wants to pick
     */
    public static void open(Player player) {
        Game game = Bedwars.getInstance().getGame();
        List<GameTeam> teams = new ArrayList<>(game.getTeams());
        if (teams.isEmpty()) return;

        CustomInventory menu = new CustomInventory(SIZE, title(), null);
        menu.fillPlaceHolder();

        int start = Math.max(0, (SIZE - teams.size()) / 2);
        for (int i = 0; i < teams.size() && start + i < SIZE; i++) {
            GameTeam team = teams.get(i);
            menu.setItem(start + i, Equipment.teamWool(team, lore(game, team)),
                    new SimpleItemAction(event -> {
                        if (event.getWhoClicked() instanceof Player clicker) choose(clicker, team);
                    }));
        }
        CustomInventory.show(player, menu);
    }

    /**
     * @param game the round
     * @param team the team the block stands for
     * @return who is in it, or a hint that it is still empty
     */
    private static List<String> lore(Game game, GameTeam team) {
        List<String> lines = new ArrayList<>();
        int size = game.getMode().getTeamSize();
        lines.add(plain(Messages.get("lobby.team.count",
                "size", String.valueOf(team.size()),
                "maximum", String.valueOf(size))));
        for (GamePlayer member : team.getMembers()) {
            lines.add(plain(Messages.get("lobby.team.member", "player", member.getName())));
        }
        if (team.isEmpty()) lines.add(plain(Messages.get("lobby.team.empty")));
        return lines;
    }

    /**
     * Puts a player into a team, or tells them why that did not work.
     *
     * @param player who picked
     * @param team   what they picked
     */
    private static void choose(Player player, GameTeam team) {
        Game game = Bedwars.getInstance().getGame();
        GamePlayer chooser = game.get(player);
        if (chooser == null || !game.isWaiting()) return;
        if (team.contains(chooser)) {
            Messages.send(player, "lobby.team.already", "team", team.getColor().getDisplayName());
            return;
        }
        if (team.isFull(game.getMode().getTeamSize())) {
            Messages.send(player, "lobby.team.full", "team", team.getColor().getDisplayName());
            return;
        }
        team.add(chooser);
        Messages.send(player, "lobby.team.chosen", "team", team.getColor().getDisplayName());
        // the menu stays open, so it has to show who is in which team now
        open(player);
    }

    private static String title() {
        return plain(Messages.get("lobby.select.title"));
    }

    /**
     * @param component a message
     * @return it as the legacy string the inventory api still wants
     */
    private static String plain(net.kyori.adventure.text.Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
