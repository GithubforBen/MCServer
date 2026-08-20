package de.schnorrenbergers.bedwars.addon.impl;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.addon.AddonConfig;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.addon.ListeningAddon;
import de.schnorrenbergers.bedwars.api.BedwarsBedDestroyEvent;
import de.schnorrenbergers.bedwars.api.BedwarsPurchaseEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.TeamColor;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.shop.Cost;
import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.shop.item.ShopItem;
import de.schnorrenbergers.bedwars.shop.item.ShopItems;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The bed token: a bed you carry home.
 * <p>
 * It is sold at an enemy team's keeper and nowhere else, which turns buying it into the actual task -
 * walking into a hostile base with sixteen emeralds and getting out again. Everything else about it
 * follows from the same idea: it glows on whoever carries it, it falls to the ground when they die, and it
 * is only worth anything at the one spot where a team's bed used to be.
 * <p>
 * It is an addon rather than a shop entry because it changes what losing a bed means, and that is exactly
 * the kind of decision a server should be able to switch off.
 */
public final class BedTokenAddon extends ListeningAddon {

    public static final String ID = "bed-token";

    /**
     * What a bed looked like before it fell, so that it can be put back the way it was.
     *
     * @param foot     where the foot half stood
     * @param footData what it was
     * @param head     where the head half stood
     * @param headData what that was
     */
    private record Snapshot(Location foot, BlockData footData, Location head, BlockData headData) {
    }

    private final AddonConfig config;
    /** What each team's bed looked like, taken the moment it fell. */
    private final Map<TeamColor, Snapshot> beds = new EnumMap<>(TeamColor.class);
    /** How many tokens each team has already used. */
    private final Map<TeamColor, Integer> used = new EnumMap<>(TeamColor.class);

    private String category;
    private int slot;
    private List<Cost> costs;
    private double placeRadius;
    private int perTeam;
    private boolean glow;

    public BedTokenAddon(Plugin plugin, AddonSettings settings) {
        super(plugin);
        this.config = new AddonConfig(settings, ID);
        read();
    }

    private void read() {
        category = config.get("category", "utility",
                "Which page of the shop it is sold on. It is only ever shown at another team's keeper.");
        slot = config.get("slot", -1, "Where it sits on that page, -1 to let the shop place it.");
        Currency first = config.currency("price.currency", Currency.DIAMOND,
                "What it costs. Two currencies on purpose: it has to be the most expensive thing",
                "in the round, because it takes a lost bed back.");
        int amount = Math.max(0, config.get("price.amount", 8));
        Currency second = config.currency("extra-price.currency", Currency.EMERALD);
        int extra = Math.max(0, config.get("extra-price.amount", 16));
        costs = extra > 0 ? List.of(new Cost(first, amount), new Cost(second, extra))
                : List.of(new Cost(first, amount));
        placeRadius = Math.max(1.0d, config.get("place-radius", 4.0d,
                "How close to your own bed spot you have to be to use it."));
        perTeam = Math.max(1, config.get("per-team", 1,
                "How many times one team may bring its bed back over the whole round."));
        glow = config.get("glow", true,
                "Whether whoever carries it glows. The walk home is meant to be the hard part.");
        config.save();
    }

    @Override
    public void reload() {
        read();
        // the shop is holding the entry that was built out of the old numbers, so it gets a new one
        if (isListening()) onEnable(Bedwars.getInstance().getGame());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return "A very expensive item, sold only at an enemy keeper, that brings your bed back";
    }

    @Override
    protected void onEnable(Game game) {
        Bedwars.getInstance().getShopSettings().register(item());
    }

    @Override
    protected void onDisable(Game game) {
        Bedwars.getInstance().getShopSettings().unregister(ID);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isGlowing() && carries(player)) player.setGlowing(false);
        }
        beds.clear();
        used.clear();
    }

    /**
     * @return the shop entry, built fresh so that a reload of {@code addons.yml} is enough to reprice it
     */
    private ShopItem item() {
        return new ShopItem(ID, category, Messages.raw("bed-token.name"), Material.WHITE_BED, 1,
                costs, List.of(Messages.raw("bed-token.lore")), Map.of(), List.of(),
                ShopItem.TeamBlock.NONE, false, 0, null, 0, false, slot, 0, true);
    }

    // ------------------------------------------------------------------- buying

    /**
     * Refuses the purchase while it would be pointless: a team whose bed still stands has nothing to bring
     * back, and one that already used its token is not allowed a second.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPurchase(BedwarsPurchaseEvent event) {
        if (!ID.equals(event.getItemId())) return;
        GamePlayer buyer = event.getBuyer();
        Player player = buyer.getPlayer();
        GameTeam team = buyer.getTeam();
        if (player == null || team == null) {
            event.setCancelled(true);
            return;
        }
        if (team.isBedAlive()) {
            event.setCancelled(true);
            Messages.send(player, "bed-token.no-need");
            return;
        }
        if (used.getOrDefault(team.getColor(), 0) >= perTeam) {
            event.setCancelled(true);
            Messages.send(player, "bed-token.used-up", "maximum", String.valueOf(perTeam));
            return;
        }
        for (GamePlayer member : team.getMembers()) {
            Player other = member.getPlayer();
            // a second token is money thrown away: the team can only bring one bed back with it
            if (other != null && carries(other)) {
                event.setCancelled(true);
                Messages.send(player, "bed-token.already-out", "player", member.getName());
                return;
            }
        }
        // a tick later, because at this point the price has not been taken yet - somebody who cannot
        // afford it must not set off the announcement that the whole server then hunts them for
        Bukkit.getScheduler().runTask(getPlugin(), () -> announce(player, team));
    }

    /**
     * Tells everybody, once the token is really in somebody's hands.
     */
    private void announce(Player player, GameTeam team) {
        if (!player.isOnline() || !carries(player)) return;
        updateGlow(player);
        Messages.broadcast("bed-token.bought",
                "team", team.getColor().getDisplayName(),
                "player", player.getName());
        Messages.send(player, "bed-token.carrying");
        player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
    }

    // ------------------------------------------------------------------ carrying

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!ID.equals(ShopItems.idOf(event.getItem().getItemStack()))) return;
        Bukkit.getScheduler().runTask(getPlugin(), () -> updateGlow(player));
        Messages.broadcast("bed-token.picked-up", "player", player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!ID.equals(ShopItems.idOf(event.getItemDrop().getItemStack()))) return;
        event.getItemDrop().setGlowing(true);
        Bukkit.getScheduler().runTask(getPlugin(), () -> updateGlow(event.getPlayer()));
    }

    /**
     * Keeps the token out of the drops the round throws away, and puts it on the ground instead.
     * <p>
     * Runs before the round's own death handling, which clears the drops - a token that vanishes with its
     * carrier would make killing them the safest way to defend a bed that is already broken.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        ItemStack token = null;
        for (ItemStack stack : event.getDrops()) {
            if (ID.equals(ShopItems.idOf(stack))) {
                token = stack;
                break;
            }
        }
        if (token == null) return;
        event.getDrops().remove(token);
        Player player = event.getPlayer();
        player.setGlowing(false);
        Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), token);
        dropped.setGlowing(true);
        Messages.broadcast("bed-token.dropped", "player", player.getName());
    }

    /**
     * @param player somebody in the round
     * @return whether they are carrying a token right now
     */
    private boolean carries(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (ID.equals(ShopItems.idOf(stack))) return true;
        }
        return false;
    }

    private void updateGlow(Player player) {
        if (glow) player.setGlowing(carries(player));
    }

    // ------------------------------------------------------------------ placing

    /**
     * Uses the token: right click anywhere at your own bed spot.
     * <p>
     * Not by placing the bed as a block, on purpose - a bed placed anywhere else would look exactly like
     * the one thing in this game that means something, and half the base would be full of them.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        ItemStack held = event.getItem();
        if (held == null || !ID.equals(ShopItems.idOf(held))) return;
        event.setCancelled(true);

        Game game = Bedwars.getInstance().getGame();
        GamePlayer user = game.get(event.getPlayer());
        GameTeam team = user == null ? null : user.getTeam();
        if (team == null || !game.isRunning()) return;
        Player player = event.getPlayer();

        if (team.isBedAlive()) {
            Messages.send(player, "bed-token.no-need");
            return;
        }
        if (team.isEliminated()) {
            Messages.send(player, "bed-token.too-late");
            return;
        }
        Location spot = bedSpot(game, team);
        if (spot == null || player.getLocation().getWorld() != spot.getWorld()
                || player.getLocation().distanceSquared(spot) > placeRadius * placeRadius) {
            Messages.send(player, "bed-token.wrong-place");
            return;
        }
        if (!rebuild(game, team, spot)) {
            Messages.send(player, "bed-token.no-room");
            return;
        }
        held.setAmount(held.getAmount() - 1);
        team.setBedAlive(true);
        used.merge(team.getColor(), 1, Integer::sum);
        updateGlow(player);
        Messages.broadcast("bed-token.restored",
                "team", team.getColor().getDisplayName(),
                "player", player.getName());
        player.getWorld().playSound(spot, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
    }

    /**
     * A bed is never placed as a block, whatever else happens - see {@link #onInteract}.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!ID.equals(ShopItems.idOf(event.getItemInHand()))) return;
        event.setCancelled(true);
        Messages.send(event.getPlayer(), "bed-token.wrong-place");
    }

    /**
     * Remembers what a bed looked like, the moment before it falls.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedDestroyed(BedwarsBedDestroyEvent event) {
        Game game = event.getGame();
        Location spot = bedSpot(game, event.getOwner());
        if (spot == null) return;
        Block block = spot.getBlock();
        if (!(block.getBlockData() instanceof Bed bed)) return;
        Block other = block.getRelative(bed.getPart() == Bed.Part.HEAD
                ? bed.getFacing().getOppositeFace() : bed.getFacing());
        beds.put(event.getOwner().getColor(), new Snapshot(
                block.getLocation(), block.getBlockData(),
                other.getLocation(), other.getBlockData()));
    }

    /**
     * Puts a bed back where it stood.
     *
     * @return whether there was room for it
     */
    private boolean rebuild(Game game, GameTeam team, Location spot) {
        Snapshot snapshot = beds.get(team.getColor());
        if (snapshot != null) return place(snapshot.foot(), snapshot.footData(),
                snapshot.head(), snapshot.headData());

        // nothing remembered: the addon was switched on after this bed fell, so the bed is rebuilt
        // facing north, which is a bed in the right place rather than no bed at all
        Material material = team.getColor().getBed();
        Bed foot = (Bed) material.createBlockData();
        foot.setPart(Bed.Part.FOOT);
        foot.setFacing(BlockFace.NORTH);
        Bed head = (Bed) material.createBlockData();
        head.setPart(Bed.Part.HEAD);
        head.setFacing(BlockFace.NORTH);
        return place(spot, foot, spot.getBlock().getRelative(BlockFace.NORTH).getLocation(), head);
    }

    /**
     * @return whether both halves could be put back
     */
    private boolean place(Location foot, BlockData footData, Location head, BlockData headData) {
        Block footBlock = foot.getBlock();
        Block headBlock = head.getBlock();
        if (!replaceable(footBlock) || !replaceable(headBlock)) return false;
        // without physics: a bed that is placed piece by piece pops itself off as an item halfway through
        footBlock.setBlockData(footData, false);
        headBlock.setBlockData(headData, false);
        return true;
    }

    private static boolean replaceable(Block block) {
        return block.isEmpty() || block.isLiquid() || block.getType().name().endsWith("_BED");
    }

    /**
     * @return where this team's bed belongs, or {@code null} when the map has no spot for it
     */
    private static @Nullable Location bedSpot(Game game, GameTeam team) {
        World world = game.getWorld();
        if (world == null || game.getArena() == null) return null;
        TeamSpot spot = game.getArena().getTeam(team.getColor());
        MapPoint bed = spot == null ? null : spot.getBed();
        return bed == null ? null : bed.toLocation(world);
    }

    /**
     * @return what the token is called, without formatting, for a message that has its own
     */
    public static String name() {
        return Text.plain(Messages.raw("bed-token.name"));
    }
}
