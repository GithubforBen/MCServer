package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prints left and right where its owner walked.
 * <p>
 * Not a trail, although it looks like a cousin of one: a trail is worn everywhere and drawn every other
 * tick, and this is deliberately neither. It puts one print down per step, alternating sides, and it does
 * it in the lobby only - a line of footprints across a survival world would be a tracking device for
 * whoever is following you.
 */
public class FootstepsGadget implements TickingGadget {

    /** How far somebody walks between two prints, in blocks. */
    private static final double STRIDE = 0.9d;
    /** How far to the side of the middle a print goes, in blocks. */
    private static final double WIDTH = 0.22d;

    /** Where the last print went, and on which foot, per wearer. */
    private final Map<UUID, Location> lastPrint = new ConcurrentHashMap<>();
    private final Set<UUID> rightFoot = ConcurrentHashMap.newKeySet();

    @Override
    public String getId() {
        return Cosmetics.GADGET_FOOTSTEPS;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY);
    }

    @Override
    public @Nullable String hint() {
        return "Fußspuren: du hinterlässt Abdrücke, wo du langgehst.";
    }

    @Override
    public void tick(Player player, CosmeticData cosmetic) {
        Location at = player.getLocation();
        if (!player.isOnGround()) return;

        UUID id = player.getUniqueId();
        Location previous = lastPrint.get(id);
        if (previous != null && previous.getWorld() == at.getWorld()
                && previous.distanceSquared(at) < STRIDE * STRIDE) {
            return;
        }
        lastPrint.put(id, at.clone());

        boolean right = !rightFoot.contains(id);
        if (right) {
            rightFoot.add(id);
        } else {
            rightFoot.remove(id);
        }
        // sideways from the direction they are facing, so the two prints sit left and right of the line
        // they walked rather than one behind the other
        Vector side = at.getDirection().setY(0).normalize().crossProduct(new Vector(0, 1, 0))
                .multiply(right ? WIDTH : -WIDTH);
        Location print = at.clone().add(side).add(0.0d, 0.05d, 0.0d);
        at.getWorld().spawnParticle(Particle.WHITE_ASH, print, 4, 0.06d, 0.0d, 0.06d, 0.0d);
    }

    @Override
    public void cleanUp(Player player) {
        lastPrint.remove(player.getUniqueId());
        rightFoot.remove(player.getUniqueId());
    }
}
