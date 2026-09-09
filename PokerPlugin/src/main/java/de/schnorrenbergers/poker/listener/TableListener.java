package de.schnorrenbergers.poker.listener;

import de.schnorrenbergers.poker.Casino;
import de.schnorrenbergers.poker.CasinoTable;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import de.schnorrenbergers.poker.ui.RaiseUi;
import de.schnorrenbergers.poker.ui.TurnControls;
import de.schnorrenbergers.poker.world.CasinoLayout;
import de.schnorrenbergers.poker.world.CasinoWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Everything the casino has to hear about: somebody clicking a chair, somebody pressing a button, and
 * somebody logging off in the middle of a hand.
 * <p>
 * The last one is the one that matters. Chips are bits, so a player who disconnects has money on a table,
 * and it has to leave that table the same way it would have if they had stood up - not stay there until the
 * server is stopped and not quietly disappear.
 */
public final class TableListener implements Listener {

    private final Plugin plugin;
    private final CasinoLayout layout;

    public TableListener(Plugin plugin, CasinoLayout layout) {
        this.plugin = plugin;
        this.layout = layout;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /* ------------------------------------------------------------------ coming and going */

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        CasinoWorld.sendToSpawn(player, layout);
        TurnControls.clear(player);
        player.sendMessage(Component.text("Willkommen im Casino. Setz dich mit Rechtsklick auf einen "
                + "Stuhl - /poker sagt dir, was es kostet.", NamedTextColor.GOLD));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // logging off is standing up. The chips have to come off the table either way, and leaving them
        // there would be leaving somebody's bits in a server that is about to be thrown away
        Casino.handleQuit(event.getPlayer());
    }

    /* ------------------------------------------------------------------ playing */

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        int button = TurnControls.buttonOf(player.getInventory().getItemInMainHand());
        if (button >= 0) {
            event.setCancelled(true);
            press(player, button);
            return;
        }
        if (event.getClickedBlock() == null) return;
        if (!isChair(event.getClickedBlock().getType())) return;
        event.setCancelled(true);
        sitDown(player, event);
    }

    /**
     * Somebody clicked a chair.
     */
    private void sitDown(Player player, PlayerInteractEvent event) {
        CasinoTable table = Casino.tableNear(event.getClickedBlock().getLocation());
        if (table == null) {
            player.sendMessage(Component.text("Der Stuhl gehört zu keinem Tisch. Ein Admin kann das mit "
                    + "/poker setup richten.", NamedTextColor.YELLOW));
            return;
        }
        int seat = table.seatNear(event.getClickedBlock().getLocation());
        Casino.sitDown(player, table, seat);
    }

    /**
     * Somebody pressed one of the buttons on their hotbar.
     */
    private void press(Player player, int button) {
        CasinoTable table = Casino.tableOf(player);
        if (table == null) {
            TurnControls.clear(player);
            return;
        }
        if (button == TurnControls.SLOT_LEAVE) {
            Casino.leave(player);
            return;
        }
        PokerTable rules = table.getRules();
        PokerPlayer seat = table.find(player.getUniqueId());
        if (seat == null) return;
        if (rules.getActing() != seat) {
            player.sendMessage(Component.text("Du bist nicht dran.", NamedTextColor.GRAY));
            return;
        }
        int toCall = rules.toCall(seat);
        switch (button) {
            case TurnControls.SLOT_FOLD -> {
                if (toCall == 0) {
                    // folding for free is never what somebody meant to do, and it costs them the hand
                    player.sendMessage(Component.text("Schieben ist gratis - benutz den grünen Knopf.",
                            NamedTextColor.YELLOW));
                    return;
                }
                rules.act(seat, de.schnorrenbergers.poker.game.Action.fold());
            }
            case TurnControls.SLOT_CHECK_CALL -> rules.act(seat, toCall > 0
                    ? de.schnorrenbergers.poker.game.Action.call(toCall)
                    : de.schnorrenbergers.poker.game.Action.check());
            case TurnControls.SLOT_RAISE -> player.openInventory(
                    RaiseUi.build(player, rules, seat).getInventory());
            default -> {
            }
        }
    }

    /**
     * @param material a block somebody clicked
     * @return whether it is the kind of thing a chair is made of
     */
    private static boolean isChair(Material material) {
        String name = material.name();
        return name.endsWith("_STAIRS") || name.endsWith("_SLAB");
    }

    /* ------------------------------------------------------------------ keeping the room intact */

    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().isOp()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getPlayer().isOp()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        // the buttons are items, and an item on the floor is a button somebody else can pick up
        if (TurnControls.buttonOf(event.getItemDrop().getItemStack()) >= 0) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (TurnControls.buttonOf(event.getCurrentItem()) >= 0) event.setCancelled(true);
        if (TurnControls.buttonOf(event.getCursor()) >= 0) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) event.setCancelled(true);
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }
}
