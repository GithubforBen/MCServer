package de.hems.paper.cosmetic;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The one entity a gadget keeps for a player, and the promise that it goes away again.
 * <p>
 * Pets, balloons, mounts and seats are the same problem four times over: something is spawned for
 * somebody and has to disappear when they take it off, walk into another world, log off or die - and
 * anything that is spawned and forgotten is a lobby that fills up over a weekend. Keeping the bookkeeping
 * in one place means every one of them forgets in the same way.
 * <p>
 * Nothing here is saved with the world. An entity a gadget spawned is never worth keeping over a restart,
 * and a crash must not leave one behind that nobody can trace back to its owner.
 */
final class GadgetEntities {

    private final Map<UUID, UUID> entities = new ConcurrentHashMap<>();

    /**
     * @param owner somebody
     * @return what was spawned for them, or {@code null} when there is nothing or it is gone
     */
    @Nullable Entity of(Player owner) {
        UUID id = entities.get(owner.getUniqueId());
        if (id == null) return null;
        Entity entity = Bukkit.getEntity(id);
        if (entity == null || !entity.isValid()) {
            entities.remove(owner.getUniqueId());
            return null;
        }
        return entity;
    }

    /**
     * Takes an entity into a gadget's care: never saved, never in the way, never hurt by anything.
     *
     * @param owner  who it belongs to
     * @param entity what was spawned
     */
    void keep(Player owner, Entity entity) {
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setCollidable(false);
            living.setRemoveWhenFarAway(true);
        }
        remove(owner);
        entities.put(owner.getUniqueId(), entity.getUniqueId());
    }

    /**
     * @param owner somebody; whatever was spawned for them is taken out of the world
     */
    void remove(Player owner) {
        UUID id = entities.remove(owner.getUniqueId());
        if (id == null) return;
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.eject();
            entity.remove();
        }
    }
}
