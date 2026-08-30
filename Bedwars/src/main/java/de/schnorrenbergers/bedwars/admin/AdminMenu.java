package de.schnorrenbergers.bedwars.admin;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.Feature;
import de.schnorrenbergers.bedwars.config.FeatureSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.Rules;
import de.schnorrenbergers.bedwars.lobby.AddonMenu;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The switches of the server as a menu.
 * <p>
 * Everything in {@link Feature} is one button here, and clicking it flips the switch, writes it to
 * {@code features.yml} and applies it to the world and to everybody standing in it right away. The point
 * is that the things which change how a round feels - 1.8 combat, the locator bar, whether the sun moves -
 * can be changed by whoever is running the round rather than by whoever can edit a file and restart.
 */
public final class AdminMenu {

    private static final int SIZE = 45;

    /** Where the switches go: the two middle rows, leaving the frame around them clear. */
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25};

    private AdminMenu() {
    }

    /**
     * Draws the menu onto an admin's screen.
     *
     * @param player who is looking
     */
    public static void open(Player player) {
        FeatureSettings features = Bedwars.getInstance().getFeatureSettings();
        CustomInventory menu = new CustomInventory(SIZE,
                Text.legacy(Messages.get("admin.title")), null);
        menu.fillPlaceHolder();

        Feature[] all = Feature.values();
        for (int i = 0; i < all.length && i < SLOTS.length; i++) {
            Feature feature = all[i];
            menu.setItem(SLOTS[i], icon(feature, features.is(feature)),
                    new SimpleItemAction(event -> toggle(player, feature)));
        }
        menu.setItem(SIZE - 5, button(Material.CHEST, Messages.get("admin.addons"),
                        List.of(Messages.get("admin.addons-lore"))),
                new SimpleItemAction(event -> AddonMenu.open(player)));
        CustomInventory.show(player, menu);
    }

    /**
     * Flips one switch, applies it and draws the menu again so the button shows what it now is.
     */
    private static void toggle(Player player, Feature feature) {
        Bedwars plugin = Bedwars.getInstance();
        FeatureSettings features = plugin.getFeatureSettings();
        boolean on = features.toggle(feature);
        Game game = plugin.getGame();
        if (game != null) Rules.reapply(game, features);

        Messages.send(player, "admin.switched",
                "feature", feature.getTitle(),
                "state", Messages.raw(on ? "admin.state.on" : "admin.state.off"));
        player.playSound(player, on ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS,
                1.0f, on ? 1.6f : 0.8f);
        open(player);
    }

    /**
     * @param feature the switch
     * @param on      whether it is on
     * @return its button: what it is, what it does and what it is set to
     */
    private static ItemStack icon(Feature feature, boolean on) {
        List<Component> lore = new ArrayList<>();
        for (String line : feature.getDescription()) {
            // a blank line in the description is a blank line in the tooltip: these read as paragraphs,
            // and a wall of grey text is a wall nobody reads
            lore.add(line.isEmpty() ? Component.empty() : Text.item("<gray>" + line));
        }
        lore.add(Component.empty());
        lore.add(Messages.get(on ? "admin.on" : "admin.off"));
        lore.add(Messages.get(on ? "admin.click-off" : "admin.click-on"));
        return button(feature.getIcon(), Text.item("<white>" + feature.getTitle()), lore);
    }

    /**
     * @param material what to draw
     * @param name     what it is called
     * @param lore     what it says underneath
     * @return the button
     */
    private static ItemStack button(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        stack.setItemMeta(meta);
        return stack;
    }
}
