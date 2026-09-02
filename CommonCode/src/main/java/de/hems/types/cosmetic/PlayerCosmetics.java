package de.hems.types.cosmetic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What one player owns and what of it they are wearing.
 * <p>
 * Owning and having selected are two different things on purpose: somebody who buys a second win effect
 * does not lose the first one, and switching back costs nothing.
 */
public class PlayerCosmetics implements Serializable {

    private static final long serialVersionUID = 4802L;

    private UUID player;
    private ArrayList<String> owned = new ArrayList<>();
    private HashMap<String, String> selected = new HashMap<>();

    public PlayerCosmetics() {
    }

    public PlayerCosmetics(UUID player) {
        this.player = player;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    /**
     * @return the ids they have bought, or were given
     */
    public Set<String> getOwned() {
        return new LinkedHashSet<>(owned == null ? List.of() : owned);
    }

    public void setOwned(Set<String> owned) {
        this.owned = new ArrayList<>(owned == null ? Set.of() : owned);
    }

    public boolean owns(String id) {
        return id != null && owned != null && owned.contains(id);
    }

    public void add(String id) {
        if (owned == null) owned = new ArrayList<>();
        if (id != null && !owned.contains(id)) owned.add(id);
    }

    /**
     * @param type a kind of cosmetic
     * @return the id they are wearing of that kind, or {@code null} for none
     */
    public String getSelected(CosmeticType type) {
        return selected == null || type == null ? null : selected.get(type.name());
    }

    /**
     * @param type a kind of cosmetic
     * @param id   what to wear, {@code null} to wear nothing
     */
    public void select(CosmeticType type, String id) {
        if (type == null) return;
        if (selected == null) selected = new HashMap<>();
        if (id == null) {
            selected.remove(type.name());
            return;
        }
        selected.put(type.name(), id);
    }

    public HashMap<String, String> getSelections() {
        return selected == null ? new HashMap<>() : new HashMap<>(selected);
    }

    public void setSelections(HashMap<String, String> selections) {
        this.selected = selections == null ? new HashMap<>() : new HashMap<>(selections);
    }

    public PlayerCosmetics copy() {
        PlayerCosmetics copy = new PlayerCosmetics(player);
        copy.owned = new ArrayList<>(owned == null ? List.of() : owned);
        copy.selected = getSelections();
        return copy;
    }
}
