package de.schnorrenbergers.bedwars.map.setup;

import de.schnorrenbergers.bedwars.map.ArenaMap;
import org.bukkit.World;

import java.util.UUID;

/**
 * A map that is being set up right now.
 * <p>
 * There is at most one of these, the same way there is at most one round - a server that is having a map
 * built on it is not hosting a game, and pretending otherwise would only invent problems.
 */
public class SetupSession {

    private final ArenaMap map;
    private final World world;
    private final UUID startedBy;

    /** Whether something was changed that is not written to disk yet. */
    private boolean dirty;

    public SetupSession(ArenaMap map, World world, UUID startedBy) {
        this.map = map;
        this.world = world;
        this.startedBy = startedBy;
    }

    public ArenaMap getMap() {
        return map;
    }

    public World getWorld() {
        return world;
    }

    public UUID getStartedBy() {
        return startedBy;
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Marks the map as changed, called by every setter of the setup command.
     */
    public void touch() {
        dirty = true;
    }

    public void clean() {
        dirty = false;
    }
}
