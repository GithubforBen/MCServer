package de.schnorrenbergers.bedwars.addon.impl;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.addon.AddonConfig;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.addon.ListeningAddon;
import de.schnorrenbergers.bedwars.api.BedwarsGameStateChangeEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.phase.PhaseType;
import de.schnorrenbergers.bedwars.generator.Generator;
import de.schnorrenbergers.bedwars.generator.GeneratorManager;
import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Something happens every few minutes, and nobody knows what.
 * <p>
 * A bedwars round has a shape everybody knows by heart after ten of them: the same generator steps at the
 * same times. This addon breaks that up without changing any of the rules - every event is a gift to
 * whoever is quickest to the middle, and every one of them is announced early enough to run there.
 * <p>
 * The warning is not politeness. An event that happens without one rewards standing in the middle, which
 * is the one place a round should not be decided by.
 */
public final class RandomEventsAddon extends ListeningAddon {

    public static final String ID = "random-events";

    /** What can happen. */
    private enum Kind {
        /** Resources rain down over the middle. */
        RESOURCE_RAIN,
        /** Every generator in the middle runs a level faster for a while. */
        DOUBLE_GENERATORS,
        /** A chest full of things appears in the middle. */
        LOOT_CHEST
    }

    private final AddonConfig config;
    private final Random random = new Random();

    private BukkitTask clock;
    private final List<BukkitTask> running = new ArrayList<>();
    /** How far up from the middle the chest looks for somewhere to stand, in blocks. */
    private static final int SEARCH_HEIGHT = 24;

    private Block chest;
    /** The beacon and its base under the chest, so the whole marker comes away with it. */
    private final List<Block> marker = new ArrayList<>();

    private int intervalSeconds;
    private int warningSeconds;
    private List<Kind> enabled = List.of(Kind.values());
    private int rainAmount;
    private Currency rainCurrency;
    private int fasterSeconds;
    private List<String> lootLines;

    public RandomEventsAddon(Plugin plugin, AddonSettings settings) {
        super(plugin);
        this.config = new AddonConfig(settings, ID);
        read();
    }

    private void read() {
        intervalSeconds = Math.max(30, config.get("interval-seconds", 300,
                "How often something happens."));
        warningSeconds = Math.max(1, config.get("warning-seconds", 20,
                "How long before it happens everybody is told.",
                "Without a warning the event belongs to whoever happened to stand in the middle."));
        List<String> names = config.strings("events",
                List.of(Kind.RESOURCE_RAIN.name(), Kind.DOUBLE_GENERATORS.name(), Kind.LOOT_CHEST.name()),
                "Which of them may happen. Take one out and it never comes up.");
        List<Kind> kinds = new ArrayList<>();
        for (String name : names) {
            Kind kind = kind(name);
            if (kind == null) {
                Bukkit.getLogger().warning("[Bedwars] addons.yml: there is no random event called '"
                        + name + "'.");
                continue;
            }
            kinds.add(kind);
        }
        enabled = List.copyOf(kinds);
        rainAmount = Math.max(1, config.get("resource-rain.amount", 16,
                "How many resources the rain drops."));
        rainCurrency = config.currency("resource-rain.currency", Currency.DIAMOND,
                "What it rains.");
        fasterSeconds = Math.max(5, config.get("double-generators.seconds", 45,
                "How long the middle generators keep the extra level."));
        lootLines = config.strings("loot-chest.contents",
                List.of("DIAMOND:4", "EMERALD:2", "GOLDEN_APPLE:2", "TNT:4", "ENDER_PEARL:1"),
                "What is in the chest, written as MATERIAL:AMOUNT.");
        config.save();
    }

    private static @Nullable Kind kind(String name) {
        for (Kind kind : Kind.values()) {
            if (kind.name().equalsIgnoreCase(name)) return kind;
        }
        return null;
    }

    @Override
    public void reload() {
        read();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return "Every few minutes something happens in the middle of the map, announced ahead of time";
    }

    @Override
    public boolean isDefaultEnabled() {
        return false;
    }

    @Override
    protected void onEnable(Game game) {
        if (game.isRunning()) startClock();
    }

    @Override
    protected void onDisable(Game game) {
        stopClock();
        running.forEach(BukkitTask::cancel);
        running.clear();
        removeChest();
    }

    /**
     * The clock only runs while a round does - an event in the waiting lobby would rain diamonds on
     * people who cannot pick them up.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStateChange(BedwarsGameStateChangeEvent event) {
        if (event.getTo() == PhaseType.RUNNING) {
            startClock();
        } else {
            stopClock();
            removeChest();
        }
    }

    private void startClock() {
        stopClock();
        if (enabled.isEmpty()) return;
        long period = intervalSeconds * 20L;
        clock = Bukkit.getScheduler().runTaskTimer(getPlugin(), this::announce, period, period);
    }

    private void stopClock() {
        if (clock == null) return;
        clock.cancel();
        clock = null;
    }

    // ------------------------------------------------------------------- events

    /**
     * Says what is coming, then lets it happen.
     */
    private void announce() {
        Game game = Bedwars.getInstance().getGame();
        if (game == null || !game.isRunning() || enabled.isEmpty()) return;
        Kind kind = enabled.get(random.nextInt(enabled.size()));
        String name = Messages.raw("random-event." + key(kind) + ".name");

        Messages.broadcast("random-event.warning",
                "event", Text.plain(name),
                "seconds", String.valueOf(warningSeconds));
        Bukkit.getServer().showTitle(Title.title(
                Messages.get("random-event.title"),
                Messages.get("random-event.subtitle",
                        "event", Text.plain(name)),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));
        running.add(Bukkit.getScheduler().runTaskLater(getPlugin(),
                () -> run(kind), warningSeconds * 20L));
    }

    private void run(Kind kind) {
        Game game = Bedwars.getInstance().getGame();
        if (game == null || !game.isRunning()) return;
        Location middle = game.getMiddle();
        if (middle == null) return;
        Messages.broadcast("random-event.now",
                "event", Text.plain(Messages.raw("random-event." + key(kind) + ".name")));
        switch (kind) {
            case RESOURCE_RAIN -> rain(middle);
            case DOUBLE_GENERATORS -> faster(game);
            case LOOT_CHEST -> chest(game, middle);
        }
    }

    /**
     * Drops a pile of resources over the middle, high enough that everybody sees them coming down.
     */
    private void rain(Location middle) {
        World world = middle.getWorld();
        if (world == null) return;
        Location above = middle.clone().add(0.0d, 6.0d, 0.0d);
        for (int i = 0; i < rainAmount; i++) {
            Location at = above.clone().add(random.nextDouble() * 4 - 2, 0.0d, random.nextDouble() * 4 - 2);
            world.dropItem(at, new ItemStack(rainCurrency.getMaterial()));
        }
        world.playSound(middle, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    /**
     * Puts every generator in the middle up one level, and back down again afterwards.
     */
    private void faster(Game game) {
        GeneratorManager generators = game.getGenerators();
        if (generators == null) return;
        List<Generator> raised = new ArrayList<>();
        for (Generator generator : generators.all()) {
            if (generator.getOwner() != null) continue;
            int before = generator.getTier();
            generator.setTier(before + 1);
            // only the ones that really went up: a generator already at its highest level cannot be
            // sped up, and putting it "back" afterwards would leave it slower than it started
            if (generator.getTier() > before) raised.add(generator);
        }
        running.add(Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            // one level down rather than back to a remembered number, so a timeline step that happened
            // in the meantime is not undone with it
            for (Generator generator : raised) generator.setTier(generator.getTier() - 1);
            Messages.broadcast("random-event.double-generators.over");
        }, fasterSeconds * 20L));
    }

    /**
     * Puts a chest in the middle. Only one at a time: two of them would sit inside each other.
     */
    private void chest(Game game, Location middle) {
        World world = middle.getWorld();
        if (world == null) return;
        removeChest();

        Block block = standingRoom(middle);
        if (block == null) {
            Bukkit.getLogger().warning("[Bedwars] The loot chest found nowhere to stand in the middle.");
            return;
        }
        block.setType(Material.CHEST, false);
        game.getBlockTracker().remember(block);
        if (block.getState() instanceof Chest state) {
            for (String line : lootLines) {
                ItemStack stack = loot(line);
                // the live inventory of the placed block, and no update() afterwards: update() writes the
                // snapshot the state was taken from back over it, which is how the chest ended up empty
                if (stack != null) state.getInventory().addItem(stack);
            }
        }
        chest = block;
        markWithBeacon(game, block);
        world.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    /**
     * Finds somewhere in the middle that a chest can actually stand.
     * <p>
     * The middle of a map is worked out from its generators, and on a map with a lower level that average
     * lands inside the scenery - which is why the chest used to appear in a wall or not at all. So the
     * column over it is walked until there is a free block with something solid under it.
     *
     * @param middle where the map's middle is
     * @return the block to put the chest in, or {@code null} when the whole column is solid
     */
    private static @Nullable Block standingRoom(Location middle) {
        World world = middle.getWorld();
        if (world == null) return null;
        int bottom = middle.getBlockY();
        for (int y = bottom; y <= Math.min(bottom + SEARCH_HEIGHT, world.getMaxHeight() - 2); y++) {
            Block block = world.getBlockAt(middle.getBlockX(), y, middle.getBlockZ());
            if (!block.isEmpty() || !block.getRelative(BlockFace.UP).isEmpty()) continue;
            if (block.getRelative(BlockFace.DOWN).isEmpty()) continue;
            return block;
        }
        return null;
    }

    /**
     * Puts a beacon under the chest so that everybody can see where it went.
     * <p>
     * With its base, because a beacon without one is a block that glows a little and nothing else. The
     * beam is the point: a chest in the middle that nobody finds is a chest that goes to whoever happened
     * to be standing there.
     */
    private void markWithBeacon(Game game, Block chestBlock) {
        Block beacon = chestBlock.getRelative(BlockFace.DOWN);
        if (beacon.getType() == Material.BEACON) return;
        place(game, beacon, Material.BEACON);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                place(game, beacon.getRelative(x, -1, z), Material.IRON_BLOCK);
            }
        }
    }

    /**
     * Sets one block of the marker, remembering what was there so it can be put back.
     */
    private void place(Game game, Block block, Material material) {
        marker.add(block);
        block.setType(material, false);
        game.getBlockTracker().remember(block);
    }

    private void removeChest() {
        Game game = Bedwars.getInstance() == null ? null : Bedwars.getInstance().getGame();
        for (Block block : marker) {
            if (block.getType() == Material.BEACON || block.getType() == Material.IRON_BLOCK) {
                block.setType(Material.AIR, false);
            }
            if (game != null) game.getBlockTracker().forget(block);
        }
        marker.clear();
        if (chest == null) return;
        if (chest.getType() == Material.CHEST) {
            if (chest.getState() instanceof Chest state) state.getInventory().clear();
            chest.setType(Material.AIR, false);
        }
        if (game != null) game.getBlockTracker().forget(chest);
        chest = null;
    }

    /**
     * @param line one entry of the chest's contents
     * @return the stack, or {@code null} when it names something this server does not have
     */
    private static @Nullable ItemStack loot(String line) {
        String[] parts = line.split(":");
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            Bukkit.getLogger().warning("[Bedwars] addons.yml: the loot chest asks for '" + parts[0]
                    + "', which is not a material.");
            return null;
        }
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        return new ItemStack(material, amount);
    }

    private static String key(Kind kind) {
        return kind.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
