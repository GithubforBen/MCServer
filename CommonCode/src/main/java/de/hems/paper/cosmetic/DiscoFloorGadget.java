package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The floor lights up under whoever is wearing it.
 * <p>
 * Nothing here touches the world. The colours are block changes sent to the people who can see them, so
 * the lobby's floor is still its floor: nobody can mine a colour, nothing has to be put back after a
 * crash, and a restart in the middle of a dance leaves no trace. That is also the reason it is the
 * lobby's alone - on survival a floor that lies about what it is made of is a floor somebody digs into.
 */
public class DiscoFloorGadget implements TickingGadget {

    /** How far away the colours are still sent, in blocks. */
    private static final double RANGE = 24.0d;
    /** How long one colour lasts, in ticks. */
    private static final int DEFAULT_DURATION_TICKS = 6;
    /** The colours it cycles through. */
    private static final Material[] COLOURS = {
            Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE, Material.PURPLE_CONCRETE,
            Material.BLUE_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.LIME_CONCRETE,
            Material.YELLOW_CONCRETE, Material.ORANGE_CONCRETE};

    /** Which blocks are currently lying to somebody's neighbours, per wearer. */
    private final Map<UUID, List<Block>> painted = new ConcurrentHashMap<>();
    /** How often the colour has changed, which is all the cycle needs to know. */
    private final Map<UUID, Integer> steps = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return Cosmetics.GADGET_DISCO_FLOOR;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY);
    }

    @Override
    public @Nullable String hint() {
        return "Disco-Boden: der Boden unter dir leuchtet - nur zum Angucken.";
    }

    @Override
    public void tick(Player player, CosmeticData cosmetic) {
        UUID id = player.getUniqueId();
        int step = steps.merge(id, 1, Integer::sum);
        int every = Math.max(1, cosmetic.getNumber(Cosmetics.SETTING_DURATION_TICKS,
                DEFAULT_DURATION_TICKS));
        List<Block> under = under(player);
        List<Block> before = painted.get(id);

        boolean sameFloor = before != null && before.equals(under);
        // the floor is redrawn when its owner walked on, and otherwise only when the colour is due to
        // change - a player standing still is nine packets every other tick for nothing
        if (sameFloor && step % every != 0) return;
        if (!sameFloor) restore(player, before);

        Material colour = COLOURS[(step / every) % COLOURS.length];
        for (Block block : under) show(player, block, colour.createBlockData());
        painted.put(id, under);
    }

    @Override
    public void cleanUp(Player player) {
        restore(player, painted.remove(player.getUniqueId()));
        steps.remove(player.getUniqueId());
    }

    /**
     * @param player somebody
     * @return the floor under them: the nine blocks around their feet that are solid enough to colour
     */
    private List<Block> under(Player player) {
        List<Block> floor = new ArrayList<>(9);
        Block middle = player.getLocation().subtract(0.0d, 1.0d, 0.0d).getBlock();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block block = middle.getRelative(x, 0, z);
                if (block.getType().isOccluding()) floor.add(block);
            }
        }
        return floor;
    }

    private void restore(Player player, List<Block> blocks) {
        if (blocks == null) return;
        for (Block block : blocks) show(player, block, block.getBlockData());
    }

    /**
     * Sends one block as something it is not, to everybody standing close enough to see it.
     */
    private void show(Player player, Block block, org.bukkit.block.data.BlockData data) {
        Location at = block.getLocation();
        for (Player viewer : player.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) > RANGE * RANGE) continue;
            viewer.sendBlockChange(at, data);
        }
    }
}
