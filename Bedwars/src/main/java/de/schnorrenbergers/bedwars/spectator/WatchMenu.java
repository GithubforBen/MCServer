package de.schnorrenbergers.bedwars.spectator;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Where to watch from.
 * <p>
 * Minecraft's spectator mode can already follow a player by clicking them - but only one you can find,
 * and a dead player is usually somewhere else entirely. This is the list of everybody still standing,
 * with their team and their kills, so that watching a round is a choice rather than a search.
 */
public final class WatchMenu {

    private WatchMenu() {
    }

    /**
     * @param player who is watching
     */
    public static void open(Player player) {
        Game game = Bedwars.getInstance().getGame();
        List<GamePlayer> alive = new ArrayList<>();
        for (GamePlayer participant : game.getOnlinePlayers()) {
            if (participant.isAlive() && !participant.getUuid().equals(player.getUniqueId())) {
                alive.add(participant);
            }
        }
        if (alive.isEmpty()) {
            Messages.send(player, "watch.nobody");
            return;
        }
        CustomInventory menu = new CustomInventory(9 * ((alive.size() - 1) / 9 + 1),
                Text.legacy(Messages.get("watch.title")), null);
        menu.fillPlaceHolder();

        int slot = 0;
        for (GamePlayer target : alive) {
            menu.setItem(slot++, head(target), new SimpleItemAction(event -> {
                event.getWhoClicked().closeInventory();
                watch(player, target);
            }));
        }
        player.openInventory(menu.getInventory());
    }

    /**
     * Takes the watcher to whoever they picked.
     */
    private static void watch(Player watcher, GamePlayer target) {
        Player standing = target.getPlayer();
        if (standing == null || !target.isAlive()) {
            Messages.send(watcher, "watch.gone", "player", target.getName());
            return;
        }
        watcher.teleport(standing.getLocation());
        Messages.send(watcher, "watch.now", "player", target.getName());
    }

    /**
     * @param target who the head stands for
     * @return their head, with their team and what they have done so far
     */
    private static ItemStack head(GamePlayer target) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        GameTeam team = target.getTeam();
        meta.displayName(Text.item("<white>" + target.getName()));
        List<Component> lore = new ArrayList<>();
        lore.add(Messages.get("watch.team",
                "team", team == null ? Messages.raw("chat.no-team") : team.getColor().getDisplayName()));
        lore.add(Messages.get("watch.kills",
                "kills", String.valueOf(target.getKills()),
                "beds", String.valueOf(target.getBedsBroken())));
        meta.lore(lore);
        // the head of the player it stands for, when the server has their skin at hand
        if (meta instanceof SkullMeta skull && target.getPlayer() != null) {
            skull.setOwningPlayer(target.getPlayer());
            stack.setItemMeta(skull);
            return stack;
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
