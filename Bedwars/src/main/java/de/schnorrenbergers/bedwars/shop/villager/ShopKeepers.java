package de.schnorrenbergers.bedwars.shop.villager;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The two villagers in every base.
 * <p>
 * They are furniture rather than creatures: no ai, no sound, no damage and no trades of their own. All
 * that is left of a villager is a model standing where the map says, and the click that opens a menu -
 * which is exactly what a shop keeper is.
 */
public final class ShopKeepers {

    /**
     * One keeper that is standing in the world.
     *
     * @param team     whose base it stands in
     * @param upgrades whether it sells team upgrades rather than items
     */
    public record Keeper(GameTeam team, boolean upgrades) {
    }

    private final Map<UUID, Keeper> keepers = new HashMap<>();
    private final List<Villager> spawned = new ArrayList<>();

    /**
     * Puts a keeper into every base the map has a spot for.
     *
     * @param game the round
     */
    public void spawn(Game game) {
        remove();
        if (game.getArena() == null || game.getWorld() == null) return;
        for (GameTeam team : game.getTeams()) {
            if (!team.isAlive()) continue;
            TeamSpot spot = game.getArena().getTeam(team.getColor());
            if (spot == null) continue;
            place(game, team, spot.getShop(), false);
            place(game, team, spot.getUpgrade(), true);
        }
    }

    /**
     * Spawns one keeper, doing nothing when the map has no spot for it.
     */
    private void place(Game game, GameTeam team, @Nullable MapPoint point, boolean upgrades) {
        if (point == null || game.getWorld() == null) return;
        Location at = point.toLocation(game.getWorld());
        Villager villager = game.getWorld().spawn(at, Villager.class, entity -> {
            entity.setProfession(upgrades ? Villager.Profession.WEAPONSMITH : Villager.Profession.LIBRARIAN);
            entity.setAI(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setCollidable(false);
            entity.setRemoveWhenFarAway(false);
            // not persistent: the arena is a copy that is thrown away, and a saved villager would be one
            // more thing that has to be cleaned up before the world is written back during setup
            entity.setPersistent(false);
            entity.customName(Messages.get(upgrades ? "shop.keeper.upgrades" : "shop.keeper.items",
                    "team", team.getColor().getDisplayName()));
            entity.setCustomNameVisible(true);
        });
        keepers.put(villager.getUniqueId(), new Keeper(team, upgrades));
        spawned.add(villager);
    }

    /**
     * @param entity something that was clicked or hit
     * @return the keeper it is, or {@code null} when it is anything else
     */
    public @Nullable Keeper get(@Nullable Entity entity) {
        return entity == null ? null : keepers.get(entity.getUniqueId());
    }

    /**
     * Takes every keeper out of the world again.
     */
    public void remove() {
        for (Villager villager : spawned) {
            if (villager.isValid()) villager.remove();
        }
        spawned.clear();
        keepers.clear();
    }
}
