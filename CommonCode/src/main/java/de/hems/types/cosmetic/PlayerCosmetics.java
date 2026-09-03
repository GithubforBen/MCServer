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
        return getSelected(type, null);
    }

    /**
     * @param type a kind of cosmetic
     * @param slot which slot, for gadgets; every other kind is worn in one place and ignores it
     * @return the id they are wearing there, or {@code null} for none
     */
    public String getSelected(CosmeticType type, GadgetSlot slot) {
        if (selected == null || type == null) return null;
        String worn = selected.get(key(type, slot));
        // somebody who chose a gadget before there were slots has it under the bare kind. Their choice
        // counts in every slot until they change one, rather than silently becoming nothing
        if (worn == null && type == CosmeticType.GADGET && slot != null) return selected.get(type.name());
        return worn;
    }

    /**
     * @param type a kind of cosmetic
     * @param id   what to wear, {@code null} to wear nothing
     */
    public void select(CosmeticType type, String id) {
        select(type, null, id);
    }

    /**
     * @param type a kind of cosmetic
     * @param slot which slot, for gadgets; ignored by every other kind
     * @param id   what to wear, {@code null} to wear nothing
     */
    public void select(CosmeticType type, GadgetSlot slot, String id) {
        if (type == null) return;
        if (selected == null) selected = new HashMap<>();
        if (type == CosmeticType.GADGET && slot != null) splitLegacyGadget();
        String key = key(type, slot);
        if (id == null) {
            selected.remove(key);
            return;
        }
        selected.put(key, id);
    }

    /**
     * Turns a pre-slot gadget choice into the same choice in every slot.
     * <p>
     * Done at the moment the first slot is written, because from then on the bare key is no longer read
     * and leaving it would take the gadget off everywhere else.
     */
    private void splitLegacyGadget() {
        String legacy = selected.remove(CosmeticType.GADGET.name());
        if (legacy == null) return;
        for (GadgetSlot slot : GadgetSlot.values()) {
            selected.putIfAbsent(key(CosmeticType.GADGET, slot), legacy);
        }
    }

    /**
     * @param type a kind
     * @param slot a slot, or {@code null}
     * @return the key it is stored under; plain text, because it ends up as a key in {@code cosmetics.yml}
     */
    private static String key(CosmeticType type, GadgetSlot slot) {
        if (type != CosmeticType.GADGET || slot == null) return type.name();
        return type.name() + "_" + slot.name();
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
