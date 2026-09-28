package de.schnorrenbergers.lobby.tablist;

import de.hems.paper.money.MoneyService;
import de.hems.paper.tablist.TabContent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What the lobby puts into the tab list: the bits, since the cosmetics and the lotto are paid for here, and
 * the ways to everything else.
 */
public class LobbyTab implements TabContent {

    @Override
    public String mode() {
        return "Lobby";
    }

    @Override
    public TextColor accent() {
        return NamedTextColor.AQUA;
    }

    @Override
    public List<Component> footer(Player viewer) {
        return List.of(Component.text("Bits: ", NamedTextColor.GRAY)
                .append(Component.text(MoneyService.get(viewer.getUniqueId()), NamedTextColor.GOLD)));
    }

    @Override
    public @Nullable String hint() {
        return "/warp  ·  /events  ·  /runde  ·  /cosmetics  ·  /parkour  ·  /lotto";
    }
}
