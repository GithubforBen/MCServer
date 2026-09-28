package de.hems.paper.tablist;

import de.hems.paper.money.MoneyService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The tab list of a mode that has nothing of its own to show beyond its name, and maybe the bits.
 *
 * @param mode     the name of the mode
 * @param accent   its colour
 * @param showBits whether the footer shows the viewer's bits - only where {@link MoneyService} runs
 * @param hint     the commands worth knowing, or {@code null}
 */
public record SimpleTab(String mode, TextColor accent, boolean showBits, @Nullable String hint) implements TabContent {

    @Override
    public List<Component> footer(Player viewer) {
        if (!showBits) return List.of();
        return List.of(Component.text("Bits: ", NamedTextColor.GRAY)
                .append(Component.text(MoneyService.get(viewer.getUniqueId()), NamedTextColor.GOLD)));
    }
}
