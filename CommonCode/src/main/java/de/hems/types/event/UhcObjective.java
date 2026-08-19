package de.hems.types.event;

import de.hems.types.event.EventType;

import java.io.Serializable;
import java.util.List;

/**
 * What a hardcore run has to kill.
 * <p>
 * The two run events differ only in this list, which is why they share everything else: the dragon run
 * wants one kill, the boss run wants all four.
 */
public enum UhcObjective implements Serializable {

    ELDER_GUARDIAN("Elder Guardian", "ELDER_GUARDIAN"),
    WITHER("Wither", "WITHER"),
    WARDEN("Warden", "WARDEN"),
    ENDER_DRAGON("Enderdrache", "ENDER_DRAGON");

    private final String title;
    /** The bukkit EntityType name, compared as a string so a renamed constant cannot break the build. */
    private final String entityType;

    UhcObjective(String title, String entityType) {
        this.title = title;
        this.entityType = entityType;
    }

    public String getTitle() {
        return title;
    }

    public String getEntityType() {
        return entityType;
    }

    /**
     * @param entityTypeName the entity that just died
     * @return the objective it counts for, or {@code null} if it counts for none
     */
    public static UhcObjective byEntityType(String entityTypeName) {
        if (entityTypeName == null) return null;
        for (UhcObjective objective : values()) {
            if (objective.entityType.equalsIgnoreCase(entityTypeName)) return objective;
        }
        return null;
    }

    /**
     * @param type the kind of event
     * @return everything a run of it has to kill, empty for events that are not races
     */
    public static List<UhcObjective> of(EventType type) {
        return switch (type) {
            case UHC_BOSSES -> List.of(ELDER_GUARDIAN, WITHER, WARDEN, ENDER_DRAGON);
            case UHC_DRAGON -> List.of(ENDER_DRAGON);
            default -> List.of();
        };
    }
}
