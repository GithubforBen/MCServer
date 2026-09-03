package de.hems.types.cosmetic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the launcher knows about cosmetics: the catalogue, and - when the asker wanted it - who owns what.
 * <p>
 * The catalogue is small and every server wants all of it. The ownership is not: it grows with every
 * player who ever bought anything, while a game server only ever needs the people standing on it, so it
 * is normally left out and asked for one player at a time when they join. The map is still here for a
 * caller that genuinely wants everybody - the admin side - and for an older server that does not know it
 * could ask for less.
 */
public class CosmeticSnapshot implements Serializable {

    private static final long serialVersionUID = 4803L;

    private ArrayList<CosmeticData> catalog = new ArrayList<>();
    private HashMap<UUID, PlayerCosmetics> players = new HashMap<>();

    public CosmeticSnapshot() {
    }

    public CosmeticSnapshot(ArrayList<CosmeticData> catalog, HashMap<UUID, PlayerCosmetics> players) {
        this.catalog = catalog == null ? new ArrayList<>() : catalog;
        this.players = players == null ? new HashMap<>() : players;
    }

    public List<CosmeticData> getCatalog() {
        return catalog == null ? List.of() : catalog;
    }

    public Map<UUID, PlayerCosmetics> getPlayers() {
        return players == null ? Map.of() : players;
    }
}
