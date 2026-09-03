package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.GeneratorSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The chests standing in the bases.
 * <p>
 * A chest in a base is the team's, and nothing said so up to now: an enemy who walked in could empty it,
 * which made storing anything in one pointless. Ownership is read off the map rather than off the block,
 * because the chests are part of the arena and were never placed by anybody the game could remember.
 * <p>
 * The second half is the punch: hitting your own chest puts every resource you are carrying into it. A
 * team chest is used in the seconds between two fights, and those seconds are not enough to open a menu
 * and drag four stacks across it.
 */
public class TeamChestListener implements Listener {

    /** How long one punch lasts, in milliseconds. */
    private static final long PUNCH_COOLDOWN = 400L;

    /** Who punched a chest when, so that holding the button down is one deposit and not twenty. */
    private final Map<UUID, Long> lastPunch = new HashMap<>();

    public TeamChestListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || !isChest(block)) return;
        Game game = game();
        if (game == null || !game.isRunning() || game.isSetupMode()) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        GameTeam owner = ownerOf(game, block);
        if (owner == null) return;

        GamePlayer participant = game.get(player);
        GameTeam own = participant == null ? null : participant.getTeam();
        if (!owner.equals(own)) {
            // cancelled for both hands: a right click opens it, and a left click is the first tick of
            // mining it away, which would take the whole chest and everything in it
            event.setCancelled(true);
            Messages.send(player, "chest.not-yours", "team", owner.getColor().getDisplayName());
            player.playSound(player, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        // cancelled before the cooldown: a punch that is swallowed as a repeat must still not chip away
        // at a chest that is part of the map
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long last = lastPunch.get(player.getUniqueId());
        if (last != null && now - last < PUNCH_COOLDOWN) return;
        lastPunch.put(player.getUniqueId(), now);
        deposit(player, block);
    }

    /**
     * Puts everything the puncher is carrying that a generator makes into the chest.
     *
     * @param player who punched
     * @param block  their team's chest
     */
    private void deposit(Player player, Block block) {
        if (!(block.getState() instanceof Container container)) return;
        Set<Material> resources = resourceMaterials();
        Inventory chest = container.getInventory();
        Map<Material, Integer> stored = new HashMap<>();

        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !resources.contains(stack.getType())) continue;
            // what did not fit goes back into the slot it came from, so a full chest costs nothing
            Map<Integer, ItemStack> rest = chest.addItem(stack.clone());
            ItemStack leftover = rest.isEmpty() ? null : rest.values().iterator().next();
            int put = stack.getAmount() - (leftover == null ? 0 : leftover.getAmount());
            if (put <= 0) continue;
            stored.merge(stack.getType(), put, Integer::sum);
            player.getInventory().setItem(slot, leftover);
        }

        if (stored.isEmpty()) {
            Messages.send(player, "chest.nothing-to-deposit");
            return;
        }
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        Messages.send(player, "chest.deposited", "what", stored.entrySet().stream()
                .map(entry -> entry.getValue() + "x " + niceName(entry.getKey()))
                .collect(Collectors.joining(", ")));
    }

    /**
     * @param game  the round
     * @param block a chest
     * @return the team whose base it stands in, or {@code null} when it stands outside every base
     */
    private @Nullable GameTeam ownerOf(Game game, Block block) {
        ArenaMap arena = game.getArena();
        if (arena == null) return null;
        double radius = game.getSettings().getTeamChestRadius();
        if (radius <= 0.0d) return null;

        Location at = block.getLocation().add(0.5d, 0.5d, 0.5d);
        GameTeam nearest = null;
        double best = radius * radius;
        // the nearest base rather than the first one inside the radius: two bases whose radii overlap on a
        // small map would otherwise hand every chest to whichever team the map happens to list first
        for (GameTeam team : game.getTeams()) {
            // a base nobody joined owns nothing: its chests are part of the map and are there to be looted
            if (team.isEmpty()) continue;
            TeamSpot spot = arena.getTeam(team.getColor());
            MapPoint spawn = spot == null ? null : spot.getSpawn();
            if (spawn == null) continue;
            double dx = at.getX() - spawn.x();
            double dy = at.getY() - spawn.y();
            double dz = at.getZ() - spawn.z();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance > best) continue;
            best = distance;
            nearest = team;
        }
        return nearest;
    }

    private static boolean isChest(Block block) {
        return block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST;
    }

    /**
     * @return every material a generator of this round makes
     */
    private static Set<Material> resourceMaterials() {
        GeneratorSettings settings = Bedwars.getInstance().getGeneratorSettings();
        return settings.all().values().stream()
                .map(GeneratorSettings.Type::material)
                .collect(Collectors.toSet());
    }

    private static String niceName(Material material) {
        return Text.niceName(material.name());
    }

    private static @Nullable Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}
