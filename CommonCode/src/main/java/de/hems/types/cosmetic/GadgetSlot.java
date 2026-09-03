package de.hems.types.cosmetic;

import java.util.Collection;
import java.util.Locale;

/**
 * Where a gadget is worn.
 * <p>
 * Gadgets are the one kind of cosmetic that is a rule rather than a picture, and the rules that make
 * sense are not the same everywhere: an ender pearl that is never used up is a cosmetic in a twenty
 * minute round and an economy in a world people build in. So a player wears one gadget <em>per slot</em>
 * rather than one in total - putting the double jump on in the lobby must not take the harvest helper
 * off on survival.
 * <p>
 * A server says which slot it is when it switches its gadgets on, see
 * {@code Gadgets.setGuard(Predicate, GadgetSlot)}. A server that never says stays without gadgets.
 */
public enum GadgetSlot {

    /** The hub everybody stands around in. */
    LOBBY("Lobby"),
    /** The world people build in. */
    SURVIVAL("Survival"),
    /** A bedwars round, whichever server happens to be running it. */
    BEDWARS("Bedwars");

    private final String displayName;

    GadgetSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param name a slot's name, in any case, or {@code null}
     * @return the slot, or {@code null} when there is none by that name
     */
    public static GadgetSlot of(String name) {
        if (name == null) return null;
        for (GadgetSlot slot : values()) {
            if (slot.name().equalsIgnoreCase(name.trim())) return slot;
        }
        return null;
    }

    /**
     * @param slots the slots to name
     * @return them as a German list, for a menu: "Lobby, Survival"
     */
    public static String list(Collection<GadgetSlot> slots) {
        StringBuilder names = new StringBuilder();
        // in the order they are declared in rather than the order they were handed in: a set has none,
        // and a menu entry whose text shuffles between two openings reads as a bug
        for (GadgetSlot slot : values()) {
            if (!slots.contains(slot)) continue;
            if (!names.isEmpty()) names.append(", ");
            names.append(slot.getDisplayName());
        }
        return names.isEmpty() ? "nirgends" : names.toString();
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
