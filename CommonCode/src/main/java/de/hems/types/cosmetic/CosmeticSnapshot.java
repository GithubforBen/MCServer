package de.hems.types.cosmetic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the launcher knows about cosmetics in one answer: the catalogue and who owns what.
 * <p>
 * The whole ownership map travels, the same way the whole team list does. That is a lot of ids for a
 * network with a lot of players, and it is still the right call here: a win effect has to be looked up at
 * the moment somebody wins, on a server that may never have seen that player before, and asking the
 * launcher at that moment would put a network round trip inside the celebration.
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
