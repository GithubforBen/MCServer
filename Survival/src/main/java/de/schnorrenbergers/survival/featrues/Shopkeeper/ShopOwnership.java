package de.schnorrenbergers.survival.featrues.Shopkeeper;

import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import de.schnorrenbergers.survival.featrues.team.ClaimManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Who a shop belongs to, answered in one place.
 * <p>
 * A shop belongs to a team. Five places used to work that out on their own, each by asking the player's
 * scoreboard for its team - which is only a mirror of the real teams on the launcher, and one that is
 * behind whenever a player has just joined or just switched teams. They all ask {@link TeamService} now,
 * which is the copy the rest of survival goes by.
 */
public final class ShopOwnership {

    private ShopOwnership() {
    }

    /**
     * @param player a player
     * @return the name of their team, or {@code null} when they have none
     */
    public static @Nullable String teamOf(Player player) {
        TeamData team = TeamService.getTeamOf(player.getUniqueId());
        return team == null ? null : team.getName();
    }

    /**
     * @param player a player
     * @param shop   a shop
     * @return whether the player is on the team that owns the shop
     */
    public static boolean owns(Player player, Shopkeeper shop) {
        String team = teamOf(player);
        return team != null && team.equalsIgnoreCase(shop.getOwnerTeam());
    }

    /**
     * Whether a player may put something of a shop - the villager, the chest - into a chunk: only onto land
     * their own team has claimed, so nobody parks a shop on somebody else's.
     *
     * @param player the player
     * @param chunk  the chunk
     * @return what stands in the way, or {@code null} when nothing does
     */
    public static @Nullable String problemWithChunk(Player player, Chunk chunk) {
        String team = teamOf(player);
        if (team == null) return "Du brauchst dafür ein Team.";
        String owner = ClaimManager.getTeamOfChunk(chunk);
        if (owner == null) return "Dieser Chunk ist nicht geclaimt - claime ihn zuerst.";
        if (!owner.equalsIgnoreCase(team)) return "Dieser Chunk gehört deinem Team nicht.";
        return null;
    }
}
