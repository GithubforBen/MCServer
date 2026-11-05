package de.schnorrenbergers.survival.featrues.lab.brew;

import de.schnorrenbergers.survival.featrues.lab.LabSettings;
import de.schnorrenbergers.survival.featrues.lab.chain.LabChain;
import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.upgrade.LabFuel;
import de.schnorrenbergers.survival.featrues.lab.upgrade.LabUpgradeReader;
import de.schnorrenbergers.survival.featrues.lab.upgrade.LabUpgrades;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.BrewingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.view.BrewingStandView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Brewing without bottles: sugar cane on top of a stand with empty bottom slots, run by our own tick
 * because vanilla never brews on empty slots. Progress lives in the block, so unloading, restarts and
 * crashes need no separate save. Every start, finish, pause and resume is logged with the position.
 * see knowledge: /decisions/gadget-choices.md
 */
public final class LabBrewing implements Listener {

    private static final int STEP_TICKS = 20;
    private static final int VANILLA_BREW_TICKS = 400;

    private final JavaPlugin plugin;
    private final BrewSettings settings;
    private final LabChain chain;
    private final LabSettings labSettings;
    private final LabUpgradeReader upgrades;
    private final NamespacedKey progressKey;
    private final Set<Block> active = new HashSet<>();

    private LabBrewing(JavaPlugin plugin, BrewSettings settings, LabSettings labSettings, LabChain chain,
                       LabUpgradeReader upgrades) {
        this.plugin = plugin;
        this.settings = settings;
        this.chain = chain;
        this.labSettings = labSettings;
        this.upgrades = upgrades;
        this.progressKey = new NamespacedKey(plugin, "lab_brew_progress");
    }

    /** Starts lab brewing: the reagent recipe, the listeners, the tick, and the stands already loaded. */
    public static LabBrewing init(JavaPlugin plugin) {
        File file = new File("./configs/lab.yml");
        LabSettings labSettings = new LabSettings(file, plugin.getLogger());
        LabChain chain = LabChain.of(labSettings);
        BrewSettings brewSettings = new BrewSettings(file, plugin.getLogger());
        LabUpgradeReader upgrades = new LabUpgradeReader(plugin);
        LabBrewing brewing = new LabBrewing(plugin, brewSettings, labSettings, chain, upgrades);
        LabPurifying.init(plugin, chain, brewSettings, labSettings, upgrades);
        LabCutBrewing.init(plugin, brewSettings, labSettings, upgrades);
        Bukkit.getPluginManager().registerEvents(new LabFuel(labSettings, upgrades), plugin);
        brewing.registerReagent();
        Bukkit.getPluginManager().registerEvents(brewing, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, brewing::tick, STEP_TICKS, STEP_TICKS);
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) brewing.activate(chunk, "start");
        }
        return brewing;
    }

    /**
     * Sugar cane gets a brewing recipe so the top slot and hoppers accept it. Its input never occurs, so
     * vanilla cannot start a brew from it. Whether the reagent set follows is checked on the server run.
     */
    private void registerReagent() {
        NamespacedKey key = new NamespacedKey(plugin, "lab_sugar_cane");
        ItemStack never = new ItemStack(Material.DEBUG_STICK);
        never.editMeta(meta -> meta.setItemName("lab-never"));
        BrewingRecipe recipe = new BrewingRecipe(key, new ItemStack(Material.STICK),
                new RecipeChoice.ExactChoice(never), new RecipeChoice.MaterialChoice(Material.SUGAR_CANE));
        boolean added;
        try {
            Bukkit.removeRecipe(key);
            added = Bukkit.addRecipe(recipe);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Lab: sugar cane reagent recipe failed: " + e.getMessage());
            return;
        }
        plugin.getLogger().info("Lab: sugar cane reagent recipe added=" + added
                + ", Bukkit reports it=" + (Bukkit.getRecipe(key) != null));
    }

    // --- which stands run ---

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        activate(event.getChunk(), "resume");
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        active.removeIf(block -> {
            if (!sameChunk(block, chunk)) return false;
            BlockState state = block.getState();
            if (state instanceof BrewingStand stand) {
                String saved = stand.getPersistentDataContainer().get(progressKey, PersistentDataType.STRING);
                if (saved != null) log("pause", block, "progress=" + saved);
            }
            return true;
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        watch(event.getInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        watch(event.getInventory());
    }

    @EventHandler
    public void onMove(InventoryMoveItemEvent event) {
        // a hopper below may take results out, unless the ingredient on top still works on them
        if (event.getSource() instanceof BrewerInventory source
                && StepRule.isValidStep(source.getIngredient(), event.getItem())) {
            event.setCancelled(true);
            return;
        }
        // items leaving free a slot, so the source is checked on the next tick like the destination
        watch(event.getSource());
        watch(event.getDestination());
    }

    private void watch(Inventory inventory) {
        if (inventory instanceof BrewerInventory && inventory.getLocation() != null) {
            active.add(inventory.getLocation().getBlock());
        }
    }

    private void activate(Chunk chunk, String what) {
        for (BlockState state : chunk.getTileEntities(block -> block.getType() == Material.BREWING_STAND, false)) {
            active.add(state.getBlock());
            if (state instanceof BrewingStand stand) {
                String saved = stand.getPersistentDataContainer().get(progressKey, PersistentDataType.STRING);
                if (saved != null) log(what, state.getBlock(), "progress=" + saved);
            }
        }
    }

    private static boolean sameChunk(Block block, Chunk chunk) {
        return block.getWorld().equals(chunk.getWorld())
                && (block.getX() >> 4) == chunk.getX() && (block.getZ() >> 4) == chunk.getZ();
    }

    // --- the tick ---

    private void tick() {
        for (Block block : new ArrayList<>(active)) {
            try {
                step(block);
            } catch (RuntimeException e) {
                active.remove(block);
                plugin.getLogger().warning("Lab: brew step failed at " + pos(block) + ": " + e.getMessage());
            }
        }
    }

    private void step(Block block) {
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
                || block.getType() != Material.BREWING_STAND) {
            active.remove(block);
            return;
        }
        BrewingStand stand = (BrewingStand) block.getState();
        BrewerInventory inventory = stand.getSnapshotInventory();
        String saved = stand.getPersistentDataContainer().get(progressKey, PersistentDataType.STRING);
        boolean eligible = isCane(inventory.getIngredient()) && stand.getFuelLevel() > 0
                && firstEmptyOutput(inventory, 0) >= 0;

        // upgrades are read when a brew starts and kept for it; a running brew uses what it started with
        LabUpgrades used = saved != null ? upgrades.stored(stand.getPersistentDataContainer())
                : eligible ? upgrades.scan(block) : LabUpgrades.NONE;
        BrewState brew = BrewState.load(saved, used.brewTicks(settings.brewTicks(), labSettings));
        BrewState.Step result = brew.advance(STEP_TICKS, eligible);
        switch (result) {
            case IDLE -> {
                if (saved == null) active.remove(block);
            }
            case STARTED -> {
                log("start", block, "ticks=" + brew.duration() + " upgrades=" + used.bits());
                upgrades.store(stand.getPersistentDataContainer(), used);
                store(stand, brew);
            }
            case RUNNING -> {
                store(stand, brew);
                showProgress(block, brew);
            }
            case RESET -> {
                log("reset", block, "ingredient gone");
                store(stand, brew);
            }
            case FINISHED -> {
                finish(block, stand, inventory, used);
                // stays in the set: if it can still brew, the next brew starts on the next tick
                active.add(block);
            }
        }
    }

    private void store(BrewingStand stand, BrewState brew) {
        String value = brew.save();
        if (value == null) {
            stand.getPersistentDataContainer().remove(progressKey);
            upgrades.clear(stand.getPersistentDataContainer());
        }
        else stand.getPersistentDataContainer().set(progressKey, PersistentDataType.STRING, value);
        stand.update(true, false);
    }

    private void finish(Block block, BrewingStand stand, BrewerInventory inventory,
                        LabUpgrades upgradesUsed) {
        ItemStack cane = inventory.getIngredient();
        LabData[] slots = new LabData[3];
        for (int i = 0; i < 3; i++) slots[i] = LabSlots.read(inventory.getItem(i));
        // one sugar cane per free output slot, and only as many slots as there is cane
        LabChain.Brew brew = chain.brew(null, cane == null ? 0 : cane.getAmount(), slots,
                ThreadLocalRandom.current(), upgradesUsed.goodsFactor(labSettings));
        for (int i = 0; i < 3; i++) {
            if (brew.rolled()[i]) inventory.setItem(i, LabSlots.toItem(brew.slots()[i]));
        }
        int used = brew.used();
        if (cane != null) {
            cane.setAmount(cane.getAmount() - used);
            inventory.setIngredient(cane.getAmount() > 0 ? cane : null);
        }
        stand.setFuelLevel(Math.max(0, stand.getFuelLevel() - 1));
        stand.getPersistentDataContainer().remove(progressKey);
        upgrades.clear(stand.getPersistentDataContainer());
        stand.update(true, false);
        log("finish", block, "slots=" + used);
        showProgress(block, new BrewState(settings.brewTicks()));
    }

    private static boolean isCane(ItemStack item) {
        return item != null && item.getType() == Material.SUGAR_CANE && item.getAmount() > 0;
    }

    private static int firstEmptyOutput(BrewerInventory inventory, int from) {
        for (int i = from; i < 3; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) return i;
        }
        return -1;
    }

    /**
     * Vanilla sets its own brew time to zero for a stand it cannot brew, so the arrow is pushed to
     * everyone who has the stand open. It counts down from the recipe time, like vanilla's.
     */
    private void showProgress(Block block, BrewState brew) {
        if (!(block.getState(false) instanceof BrewingStand stand)) return;
        int remaining = brew.isRunning()
                ? (int) Math.round(VANILLA_BREW_TICKS * (1 - brew.fraction())) : 0;
        for (HumanEntity viewer : stand.getInventory().getViewers()) {
            if (viewer.getOpenInventory() instanceof BrewingStandView view) {
                view.setRecipeBrewTime(VANILLA_BREW_TICKS);
                view.setBrewingTicks(remaining);
            }
        }
    }

    private void log(String what, Block block, String detail) {
        plugin.getLogger().info("Lab brew " + what + " at " + pos(block) + " " + detail);
    }

    private static String pos(Block block) {
        return block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
