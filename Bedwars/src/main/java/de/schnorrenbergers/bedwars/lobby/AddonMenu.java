package de.schnorrenbergers.bedwars.lobby;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.addon.Addon;
import de.schnorrenbergers.bedwars.addon.AddonRegistry;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The addon switches, as a menu.
 * <p>
 * The same three sources as everywhere else decide what is on, and this is the last word among them - so
 * the menu also says who decided, or an operator would keep switching something that a file switches back.
 * It only works while the round is waiting: an addon that appears halfway through a round is a rule change
 * in the middle of a game.
 */
public final class AddonMenu {

    private AddonMenu() {
    }

    /**
     * @param player who is switching
     */
    public static void open(Player player) {
        Game game = Bedwars.getInstance().getGame();
        AddonRegistry registry = Bedwars.getInstance().getAddons();
        List<Addon> addons = new ArrayList<>(registry.all());
        if (addons.isEmpty()) {
            Messages.send(player, "addon.none");
            return;
        }
        CustomInventory menu = new CustomInventory(9 * ((addons.size() - 1) / 9 + 1),
                Text.legacy(Messages.get("addon.title")), null);
        menu.fillPlaceHolder();

        int slot = 0;
        for (Addon addon : addons) {
            menu.setItem(slot++, icon(registry, addon),
                    new SimpleItemAction(event -> toggle(player, game, registry, addon)));
        }
        CustomInventory.show(player, menu);
    }

    /**
     * Switches one addon and shows the menu again, so the new state is visible where it was clicked.
     */
    private static void toggle(Player player, Game game, AddonRegistry registry, Addon addon) {
        if (!game.isWaiting()) {
            Messages.send(player, "addon.locked", "addon", addon.getId());
            return;
        }
        boolean on = !registry.isEnabled(addon.getId());
        registry.setSessionOverride(addon.getId(), on);
        registry.apply(game);
        Messages.send(player, "addon.switched",
                "addon", addon.getId(),
                "state", Messages.raw(on ? "addon.state.on" : "addon.state.off"));
        player.playSound(player, on ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS,
                1.0f, on ? 1.6f : 0.8f);
        open(player);
    }

    /**
     * @return the button of one addon: what it is, whether it is on, and who said so
     */
    private static ItemStack icon(AddonRegistry registry, Addon addon) {
        boolean on = registry.isEnabled(addon.getId());
        ItemStack stack = new ItemStack(on ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(Text.item("<white>" + addon.getId()));
        List<Component> lore = new ArrayList<>();
        lore.add(Text.item("<gray>" + addon.getDescription()));
        lore.add(Component.empty());
        lore.add(Messages.get(on ? "addon.entry.state.on" : "addon.entry.state.off"));
        lore.add(Messages.get("addon.entry.source", "source", registry.getSource(addon.getId()).getLabel()));
        lore.add(Messages.get("addon.entry.click"));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
