package de.schnorrenbergers.bedwars.shop.upgrade;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.UpgradeSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.trap.Trap;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The team upgrade villager.
 * <p>
 * Upgrades and traps sit in one menu because they are one pocket: both are paid for in diamonds, and the
 * question a team asks in front of this villager is always which of the two the next diamonds go into. The
 * trap queue is shown above them for the same reason - a trap costs what it costs because of what is
 * already waiting there.
 */
public final class UpgradeMenu {

    private static final int SIZE = 54;
    /** Where the queue is drawn, the middle of the second row. */
    private static final int QUEUE_CENTER = 13;
    /** Where upgrades and traps go when the config does not say. */
    private static final int[] UPGRADE_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    private static final int[] TRAP_SLOTS = {37, 38, 39, 40, 41, 42, 43};

    private UpgradeMenu() {
    }

    /**
     * @param player who is at the villager
     */
    public static void open(Player player) {
        Game game = Bedwars.getInstance().getGame();
        UpgradeSettings settings = Bedwars.getInstance().getUpgradeSettings();
        GamePlayer buyer = game.get(player);
        if (buyer == null || buyer.getTeam() == null) {
            Messages.send(player, "upgrade.no-team");
            return;
        }
        GameTeam team = buyer.getTeam();

        CustomInventory menu = new CustomInventory(SIZE, Text.legacy(Messages.get("upgrade.title")), null);
        menu.fillPlaceHolder();
        drawQueue(menu, settings, team);
        drawUpgrades(menu, settings, player, buyer, team);
        drawTraps(menu, settings, player, buyer, team);
        CustomInventory.show(player, menu);
    }

    /**
     * Draws what the team has waiting, one slot per place in the queue.
     */
    private static void drawQueue(CustomInventory menu, UpgradeSettings settings, GameTeam team) {
        int size = settings.getQueueSize();
        if (size <= 0) return;
        List<String> queued = team.getTraps();
        int start = QUEUE_CENTER - (size - 1) / 2;
        for (int index = 0; index < size; index++) {
            int slot = start + index;
            if (slot < 9 || slot > 17) continue;
            Trap trap = index < queued.size() ? settings.getTrap(queued.get(index)) : null;
            if (trap == null) {
                menu.setItem(slot, icon(Material.GRAY_STAINED_GLASS_PANE,
                                Messages.get("trap.queue.empty", "position", String.valueOf(index + 1)),
                                List.of()),
                        SimpleItemAction.display());
                continue;
            }
            menu.setItem(slot, icon(trap.icon(),
                            Messages.get("trap.queue.filled",
                                    "position", String.valueOf(index + 1),
                                    "trap", Text.plain(trap.displayName())),
                            lines(trap.lore())),
                    SimpleItemAction.display());
        }
    }

    /**
     * Draws every upgrade with the level the team is at and what the next one costs.
     */
    private static void drawUpgrades(CustomInventory menu, UpgradeSettings settings, Player player,
                                     GamePlayer buyer, GameTeam team) {
        int next = 0;
        for (Upgrade upgrade : settings.getUpgrades()) {
            int slot = upgrade.slot() >= 0 ? upgrade.slot() : slotFrom(UPGRADE_SLOTS, next++);
            if (slot < 0 || slot >= SIZE) continue;
            int level = team.getUpgradeLevel(upgrade.id());
            boolean maxed = level >= upgrade.maxLevel();
            int price = upgrade.priceFor(level + 1);

            List<Component> lore = lines(upgrade.lore());
            lore.add(Component.empty());
            lore.add(Messages.get("upgrade.level",
                    "level", String.valueOf(level),
                    "maximum", String.valueOf(upgrade.maxLevel())));
            if (maxed) {
                lore.add(Messages.get("upgrade.maxed-lore"));
            } else {
                lore.add(Messages.get("shop.price",
                        "amount", String.valueOf(price),
                        "currency", upgrade.currency().getDisplayName()));
                lore.add(Messages.get(upgrade.currency().count(player) >= price
                        ? "shop.click-to-buy" : "shop.too-expensive"));
            }
            menu.setItem(slot, icon(upgrade.icon(), Text.item(upgrade.displayName()), lore),
                    new SimpleItemAction(event -> {
                        if (maxed) return;
                        act(player, () -> Bedwars.getInstance().getUpgrades()
                                .buy(Bedwars.getInstance().getGame(), buyer, upgrade));
                    }));
        }
    }

    /**
     * Draws every trap with what the next place in the queue costs.
     */
    private static void drawTraps(CustomInventory menu, UpgradeSettings settings, Player player,
                                  GamePlayer buyer, GameTeam team) {
        int queued = team.getTraps().size();
        int price = settings.getTrapPrice(queued);
        boolean full = queued >= settings.getQueueSize();
        int next = 0;
        for (Trap trap : settings.getTraps()) {
            int slot = settings.slotOf(trap.id());
            if (slot == Integer.MAX_VALUE) slot = slotFrom(TRAP_SLOTS, next++);
            if (slot < 0 || slot >= SIZE) continue;

            List<Component> lore = lines(trap.lore());
            lore.add(Component.empty());
            if (full) {
                lore.add(Messages.get("trap.queue-full-lore", "maximum",
                        String.valueOf(settings.getQueueSize())));
            } else {
                lore.add(Messages.get("shop.price",
                        "amount", String.valueOf(price),
                        "currency", settings.getTrapCurrency().getDisplayName()));
                lore.add(Messages.get(settings.getTrapCurrency().count(player) >= price
                        ? "shop.click-to-buy" : "shop.too-expensive"));
            }
            menu.setItem(slot, icon(trap.icon(), Text.item(trap.displayName()), lore),
                    new SimpleItemAction(event -> {
                        if (full) return;
                        act(player, () -> Bedwars.getInstance().getTraps()
                                .buy(Bedwars.getInstance().getGame(), buyer, trap));
                    }));
        }
    }

    /**
     * Runs a purchase and redraws the menu, so the queue and the levels are up to date.
     * <p>
     * Redrawn into the screen that is already open rather than reopened - closing a menu the player is
     * still standing in front of is what made every click of it flicker.
     */
    private static void act(Player player, Runnable purchase) {
        Game game = Bedwars.getInstance().getGame();
        if (!game.isRunning()) return;
        purchase.run();
        open(player);
    }

    /**
     * @param material what to draw
     * @param name     what it is called
     * @param lore     what it says underneath
     * @return the button
     */
    private static ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * @param lore MiniMessage lines out of a config
     * @return them as components, in a list that can still be added to
     */
    private static List<Component> lines(List<String> lore) {
        List<Component> lines = new ArrayList<>();
        lore.forEach(line -> lines.add(Text.item(line)));
        return lines;
    }

    /**
     * @param slots the fallback places
     * @param index how many have been used
     * @return the next one, -1 when they are used up
     */
    private static int slotFrom(int[] slots, int index) {
        return index < slots.length ? slots[index] : -1;
    }
}
