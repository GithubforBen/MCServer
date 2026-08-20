package de.schnorrenbergers.bedwars.addon.impl;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.addon.AddonConfig;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.addon.ListeningAddon;
import de.schnorrenbergers.bedwars.game.BlockTracker;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.Cost;
import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.shop.item.ShopCategory;
import de.schnorrenbergers.bedwars.shop.item.ShopItem;
import de.schnorrenbergers.bedwars.shop.item.ShopItems;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The four items that are not in hypixel's shop.
 * <p>
 * They all answer the same question - how do I get from here to there, or back out of there - which is
 * what a bedwars map is mostly made of. They live on a page of their own so that a server that switches
 * them off has a shop that looks exactly like it did before, rather than one with four holes in it.
 * <p>
 * Everything they build is handed to the round's block tracker, so it can be broken and blown up like any
 * other placed block. A bridge that nobody can take down again would be a wall.
 */
public final class CustomItemsAddon extends ListeningAddon {

    public static final String ID = "custom-items";

    private static final String CATEGORY = "specials";
    private static final String HOOK = "grappling-hook";
    private static final String PLATFORM = "rescue-platform";
    private static final String EGG = "bridge-egg";
    private static final String PAD = "jump-pad";

    private final AddonConfig config;
    /** The tasks this addon started, so that switching it off really stops it. */
    private final List<BukkitTask> tasks = new ArrayList<>();
    /** When each player last used a jump pad, so standing on one does not launch them every tick. */
    private final Map<UUID, Long> lastJump = new HashMap<>();

    private int categorySlot;
    private double hookPower;
    private Material platformMaterial;
    private int platformSeconds;
    private int platformRadius;
    private int bridgeLength;
    private Material padMaterial;
    private double padPower;
    private int padCooldown;

    public CustomItemsAddon(Plugin plugin, AddonSettings settings) {
        super(plugin);
        this.config = new AddonConfig(settings, ID);
        read();
    }

    private void read() {
        categorySlot = config.get("category-slot", 6, "Where the page sits in the shop's top row.");
        hookPower = Math.max(0.1d, config.get("grappling-hook.power", 1.6d,
                "How hard the hook pulls."));
        platformMaterial = config.material("rescue-platform.material", Material.SLIME_BLOCK,
                "What the platform is made of. Slime so that falling onto it does not hurt.");
        platformSeconds = Math.max(1, config.get("rescue-platform.seconds", 10,
                "How long the platform stays before it disappears again."));
        platformRadius = Math.max(0, config.get("rescue-platform.radius", 1,
                "How far the platform reaches from the middle. 1 makes it three by three."));
        bridgeLength = Math.max(1, config.get("bridge-egg.blocks", 48,
                "How many blocks one egg lays at most."));
        padMaterial = config.material("jump-pad.material", Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
                "What a jump pad is. It has to be something you step on: that is how it is set off,",
                "which costs nothing at all to watch for.");
        padPower = Math.max(0.1d, config.get("jump-pad.power", 1.1d, "How far a pad throws."));
        padCooldown = Math.max(1, config.get("jump-pad.cooldown-ticks", 20,
                "How long before the same player can be launched again."));
        price(HOOK, Currency.GOLD, 6);
        price(PLATFORM, Currency.GOLD, 4);
        price(EGG, Currency.EMERALD, 1);
        price(PAD, Currency.GOLD, 4);
        config.save();
    }

    private void price(String id, Currency currency, int amount) {
        config.get(id + ".price.amount", amount, "What " + id + " costs.");
        config.get(id + ".price.currency", currency.name());
        config.get(id + ".amount", id.equals(PAD) || id.equals(EGG) ? 2 : 1);
        config.get(id + ".slot", -1);
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
        return "Grappling hook, rescue platform, bridge egg and jump pad, on a page of their own";
    }

    @Override
    protected void onEnable(Game game) {
        var shop = Bedwars.getInstance().getShopSettings();
        shop.register(new ShopCategory(CATEGORY, Messages.raw("custom-item.category"),
                Material.ENDER_EYE, categorySlot, List.of()));
        shop.register(item(HOOK, Material.FISHING_ROD, ShopItem.TeamBlock.NONE));
        shop.register(item(PLATFORM, Material.BLAZE_ROD, ShopItem.TeamBlock.NONE));
        shop.register(item(EGG, Material.EGG, ShopItem.TeamBlock.NONE));
        shop.register(item(PAD, padMaterial, ShopItem.TeamBlock.NONE));
    }

    @Override
    protected void onDisable(Game game) {
        var shop = Bedwars.getInstance().getShopSettings();
        for (String id : List.of(HOOK, PLATFORM, EGG, PAD, CATEGORY)) shop.unregister(id);
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        lastJump.clear();
    }

    /**
     * @return the shop entry of one of the four, named and described from {@code messages.yml}
     */
    private ShopItem item(String id, Material material, ShopItem.TeamBlock block) {
        Currency currency = config.currency(id + ".price.currency", Currency.GOLD);
        int amount = Math.max(0, config.get(id + ".price.amount", 1));
        return new ShopItem(id, CATEGORY,
                Messages.raw("custom-item." + id + ".name"), material,
                Math.max(1, config.get(id + ".amount", 1)),
                List.of(new Cost(currency, amount)),
                List.of(Messages.raw("custom-item." + id + ".lore")),
                Map.of(), List.of(), block, false, 0, null, 0, false,
                config.get(id + ".slot", -1), 0, false);
    }

    // -------------------------------------------------------------- grappling hook

    /**
     * Pulls whoever threw the hook towards where it landed, and takes the rod.
     * <p>
     * One throw per rod on purpose: a rod that can be used forever is a movement item that costs six gold
     * once and then decides every fight over the middle for the rest of the round.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        if (!HOOK.equals(ShopItems.idOf(rod))) return;
        if (event.getState() != PlayerFishEvent.State.IN_GROUND
                && event.getState() != PlayerFishEvent.State.REEL_IN) {
            return;
        }
        Location hook = event.getHook().getLocation();
        Vector pull = hook.toVector().subtract(player.getLocation().toVector());
        if (pull.lengthSquared() < 0.01d) return;
        player.setVelocity(pull.normalize().multiply(hookPower).setY(Math.max(0.4d, pull.getY() * 0.2d)));
        player.playSound(player, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.4f);
        rod.setAmount(rod.getAmount() - 1);
        event.getHook().remove();
    }

    // ------------------------------------------------------------- rescue platform

    /**
     * Builds a platform under whoever is falling, and takes it away again a few seconds later.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack held = event.getItem();
        if (held == null || !PLATFORM.equals(ShopItems.idOf(held))) return;
        event.setCancelled(true);

        Game game = Bedwars.getInstance().getGame();
        Player player = event.getPlayer();
        if (!game.isRunning() || game.getWorld() == null) return;

        List<Block> placed = new ArrayList<>();
        Location under = player.getLocation().subtract(0.0d, 1.0d, 0.0d);
        BlockTracker tracker = game.getBlockTracker();
        for (int x = -platformRadius; x <= platformRadius; x++) {
            for (int z = -platformRadius; z <= platformRadius; z++) {
                Block block = under.clone().add(x, 0.0d, z).getBlock();
                if (!block.getType().isAir()) continue;
                block.setType(platformMaterial, false);
                tracker.remember(block);
                placed.add(block);
            }
        }
        if (placed.isEmpty()) return;
        held.setAmount(held.getAmount() - 1);
        player.playSound(player, Sound.BLOCK_SLIME_BLOCK_PLACE, 1.0f, 1.0f);
        track(Bukkit.getScheduler().runTaskLater(getPlugin(),
                () -> takeAway(tracker, placed), platformSeconds * 20L));
    }

    /**
     * Takes a platform away, leaving alone whatever somebody built on top of it in the meantime.
     */
    private void takeAway(BlockTracker tracker, List<Block> placed) {
        for (Block block : placed) {
            if (block.getType() != platformMaterial) continue;
            block.setType(Material.AIR, false);
            tracker.forget(block);
        }
    }

    // ----------------------------------------------------------------- bridge egg

    /**
     * Lays a bridge under a thrown egg, in the colour of the team that threw it.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onThrow(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Egg egg)) return;
        if (!(egg.getShooter() instanceof Player player)) return;
        if (!isBridgeEgg(egg, player)) return;

        Game game = Bedwars.getInstance().getGame();
        GamePlayer thrower = game.get(player);
        GameTeam team = thrower == null ? null : thrower.getTeam();
        if (!game.isRunning() || team == null) return;
        Material wool = team.getColor().getWool();
        BlockTracker tracker = game.getBlockTracker();

        BukkitRunnable bridge = new BukkitRunnable() {
            private int laid;

            @Override
            public void run() {
                if (!egg.isValid() || laid >= bridgeLength) {
                    cancel();
                    return;
                }
                laid += lay(egg.getLocation(), egg.getVelocity(), wool, tracker);
            }
        };
        track(bridge.runTaskTimer(getPlugin(), 1L, 1L));
    }

    /**
     * Lays one step of a bridge: the block under the egg and one to either side of its flight.
     *
     * @return how many blocks were laid
     */
    private int lay(Location at, Vector direction, Material wool, BlockTracker tracker) {
        Vector side = new Vector(-direction.getZ(), 0.0d, direction.getX());
        if (side.lengthSquared() < 0.01d) return 0;
        side.normalize();
        int laid = 0;
        for (int offset = -1; offset <= 1; offset++) {
            Block block = at.clone().add(side.clone().multiply(offset)).subtract(0.0d, 1.0d, 0.0d).getBlock();
            if (!block.getType().isAir()) continue;
            block.setType(wool, false);
            tracker.remember(block);
            laid++;
        }
        return laid;
    }

    /**
     * Works out whether a thrown egg is a bridge egg.
     * <p>
     * Both ways round on purpose: a projectile does not always carry the item it was thrown from, and the
     * hand is empty once the last egg of a stack has left it. Nothing else in the shop is an egg, so
     * either answer being yes is enough.
     *
     * @param egg     the egg in the air
     * @param shooter who threw it
     * @return whether a bridge belongs under it
     */
    private static boolean isBridgeEgg(Egg egg, Player shooter) {
        if (EGG.equals(ShopItems.idOf(egg.getItem()))) return true;
        return EGG.equals(ShopItems.idOf(shooter.getInventory().getItemInMainHand()))
                || EGG.equals(ShopItems.idOf(shooter.getInventory().getItemInOffHand()));
    }

    /**
     * Remembers a task so that switching the addon off really stops it, and drops the ones that are over -
     * a round throws a lot of eggs.
     */
    private void track(BukkitTask task) {
        tasks.removeIf(BukkitTask::isCancelled);
        tasks.add(task);
    }

    /**
     * A bridge egg hatches nothing: a chicken running around the middle of the map is not what anybody
     * paid an emerald for.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHatch(PlayerEggThrowEvent event) {
        if (!isBridgeEgg(event.getEgg(), event.getPlayer())) return;
        event.setHatching(false);
    }

    // ------------------------------------------------------------------- jump pad

    /**
     * Throws whoever steps on a pad.
     * <p>
     * A pressure plate rather than a block that is watched for: stepping on one fires an event by itself,
     * so a pad costs nothing while nobody is on it.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onStep(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != padMaterial) return;
        Game game = Bedwars.getInstance().getGame();
        // only pads somebody put there: a map that happens to be built with pressure plates is not full
        // of jump pads because this addon is on
        if (!game.isRunning() || !game.getBlockTracker().wasPlaced(block)) return;

        Player player = event.getPlayer();
        long now = player.getWorld().getFullTime();
        Long last = lastJump.get(player.getUniqueId());
        if (last != null && now - last < padCooldown) return;
        lastJump.put(player.getUniqueId(), now);

        Vector push = player.getLocation().getDirection().setY(0.0d);
        if (push.lengthSquared() > 0.01d) push.normalize().multiply(padPower);
        player.setVelocity(push.setY(padPower));
        player.playSound(player, Sound.ENTITY_SLIME_JUMP, 1.0f, 1.2f);
    }

    /**
     * @param stack anything
     * @return which of these four it is, or {@code null} when it is none of them
     */
    public static @Nullable String kindOf(@Nullable ItemStack stack) {
        String id = ShopItems.idOf(stack);
        return HOOK.equals(id) || PLATFORM.equals(id) || EGG.equals(id) || PAD.equals(id) ? id : null;
    }
}
