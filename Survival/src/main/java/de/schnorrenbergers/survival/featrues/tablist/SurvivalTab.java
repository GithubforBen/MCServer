package de.schnorrenbergers.survival.featrues.tablist;

import de.hems.paper.tablist.TabContent;
import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What survival puts into the tab list: the bits, and the team with its members and land.
 * <p>
 * Player names are left alone - the scoreboard already puts the team tag in front of them.
 */
public class SurvivalTab implements TabContent {

    @Override
    public String mode() {
        return "Survival";
    }

    @Override
    public TextColor accent() {
        return NamedTextColor.GREEN;
    }

    @Override
    public List<Component> footer(Player viewer) {
        Component bits = Component.text("Bits: ", NamedTextColor.GRAY)
                .append(Component.text(MoneyHandler.getMoney(viewer.getUniqueId()), NamedTextColor.GOLD));
        return List.of(bits, team(viewer));
    }

    private static Component team(Player viewer) {
        TeamData team = TeamService.getTeamOf(viewer.getUniqueId());
        if (team == null) {
            return Component.text("Kein Team - ", NamedTextColor.GRAY)
                    .append(Component.text("/cteam", NamedTextColor.WHITE));
        }
        NamedTextColor color = colorOf(team);
        Component name = Component.text(team.getName(), color);
        if (team.getTag() != null && !team.getTag().isBlank()) {
            name = Component.text("[" + team.getTag() + "] ", color).append(name);
        }
        return Component.text("Team: ", NamedTextColor.GRAY).append(name)
                .append(Component.text("  ·  " + team.getMembers().size() + " Mitglieder  ·  "
                        + team.getClaims().size() + " Chunks", NamedTextColor.GRAY));
    }

    private static NamedTextColor colorOf(TeamData team) {
        NamedTextColor color = team.getColor() == null ? null
                : NamedTextColor.NAMES.value(team.getColor().toLowerCase(java.util.Locale.ROOT));
        return color == null ? NamedTextColor.WHITE : color;
    }

    @Override
    public @Nullable String hint() {
        return "/shop  ·  /cteam  ·  /backpack  ·  /events  ·  /warp";
    }
}
