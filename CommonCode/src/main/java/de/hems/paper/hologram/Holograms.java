package de.hems.paper.hologram;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Every hologram this server put into the world.
 * <p>
 * There for one reason: taking them all away again. A {@link Hologram} is not persistent, so a clean
 * shutdown loses nothing - but a plugin that is reloaded, a round that ends, or a world that is thrown
 * away and copied in again all leave text hanging in mid air that nothing owns any more. One call at
 * {@code onDisable} clears the lot.
 */
public final class Holograms {

    /** Weak, so a hologram that is dropped without being removed does not keep its world alive. */
    private static final Set<Hologram> living =
            Collections.newSetFromMap(new WeakHashMap<>());

    private Holograms() {
    }

    /**
     * @param hologram one that has just appeared
     */
    static synchronized void remember(Hologram hologram) {
        living.add(hologram);
    }

    /**
     * @param hologram one that has just gone
     */
    static synchronized void forget(Hologram hologram) {
        living.remove(hologram);
    }

    /**
     * Takes every hologram this server made out of the world.
     *
     * @return how many were removed
     */
    public static synchronized int removeAll() {
        List<Hologram> all = new ArrayList<>(living);
        living.clear();
        int removed = 0;
        for (Hologram hologram : all) {
            if (!hologram.isSpawned()) continue;
            hologram.remove();
            removed++;
        }
        return removed;
    }

    /**
     * Kills every text display in a world, whoever made it.
     * <p>
     * The blunt instrument, for a world that was written to disk with holograms still in it - a map that
     * was set up while a round was running, say. {@link #removeAll()} is the one to reach for otherwise:
     * this one cannot tell a hologram from any other text display somebody built into their map.
     *
     * @param world where to sweep
     * @return how many were removed
     */
    public static int sweep(World world) {
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof TextDisplay display)) continue;
            display.remove();
            removed++;
        }
        return removed;
    }

    /**
     * @param anchor where it belongs
     * @param lines  what it says
     * @return a hologram that is already in the world
     */
    public static Hologram show(Location anchor, net.kyori.adventure.text.Component... lines) {
        return Hologram.of(anchor, lines).spawn();
    }
}
