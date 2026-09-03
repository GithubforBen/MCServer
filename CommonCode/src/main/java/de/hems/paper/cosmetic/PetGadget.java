package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * A small animal that walks after its owner.
 * <p>
 * It has no mind of its own: the AI is off and it is put where it belongs a few times a second. A pet
 * with vanilla pathfinding wanders into the void, gets stuck on a fence and pushes people around, and
 * none of that is what somebody bought.
 * <p>
 * It cannot be hurt and it cannot be walked into, which on survival is the whole question this gadget
 * has to answer: a companion that can soak up an arrow or block a doorway is not a cosmetic.
 */
public class PetGadget implements TickingGadget {

    /** Which animal it is. */
    private static final String SETTING_ANIMAL = "animal";
    private static final EntityType DEFAULT_ANIMAL = EntityType.CAT;
    /** How far behind its owner it walks, in blocks. */
    private static final double BEHIND = 1.4d;
    /** How far it may drift before it is put back rather than walked back, in blocks squared. */
    private static final double TELEPORT_AT = 64.0d;
    /** How much of the way it moves per step, so it glides instead of stuttering. */
    private static final double EASING = 0.35d;

    private final GadgetEntities pets = new GadgetEntities();

    @Override
    public String getId() {
        return Cosmetics.GADGET_PET;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public @Nullable String hint() {
        return "Haustier: läuft dir hinterher, bis du es wieder ablegst.";
    }

    @Override
    public void tick(Player player, CosmeticData cosmetic) {
        Entity pet = pets.of(player);
        Location goal = goal(player);
        if (pet == null || pet.getWorld() != player.getWorld()) {
            pets.remove(player);
            spawn(player, cosmetic, goal);
            return;
        }
        Location at = pet.getLocation();
        Location next = at.distanceSquared(goal) > TELEPORT_AT
                ? goal
                : at.add(goal.toVector().subtract(at.toVector()).multiply(EASING));
        next.setDirection(player.getLocation().toVector().subtract(next.toVector()));
        pet.teleport(next);
    }

    @Override
    public void cleanUp(Player player) {
        pets.remove(player);
    }

    private void spawn(Player player, CosmeticData cosmetic, Location where) {
        EntityType type = animal(cosmetic);
        Entity pet = player.getWorld().spawnEntity(where, type);
        if (!(pet instanceof LivingEntity living)) {
            // a setting that names something that is not an animal would otherwise leave a boat or an
            // arrow following somebody around for as long as they wear this
            pet.remove();
            return;
        }
        if (living instanceof Ageable ageable) ageable.setBaby();
        pets.keep(player, living);
    }

    /**
     * @param player somebody
     * @return where the pet belongs: a step behind them, on the ground they are standing on
     */
    private Location goal(Player player) {
        Vector back = player.getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 0.01d) back = new Vector(0, 0, 1);
        return player.getLocation().add(back.normalize().multiply(-BEHIND));
    }

    private EntityType animal(CosmeticData cosmetic) {
        String named = cosmetic == null ? null : cosmetic.getSettings().get(SETTING_ANIMAL);
        if (named == null) return DEFAULT_ANIMAL;
        try {
            return EntityType.valueOf(named.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DEFAULT_ANIMAL;
        }
    }
}
