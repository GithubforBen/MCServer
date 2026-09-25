package de.hems.utils.server;

import de.hems.files.FileTrees;
import java.io.File;
import java.io.IOException;

/**
 * A world somebody prepared by hand, copied onto every new server of one kind.
 * <p>
 * The general form of what {@link CasinoMap} does for the casino: a folder next to the launcher holds the
 * map, and each fresh server of that kind gets its own copy of it before its first start. A new event that
 * plays on a built map needs one line in {@link PaperConfigurator} and a folder, nothing else.
 * <p>
 * Nothing is overwritten: a server that already has the world keeps it. And a missing folder is not an
 * error - the server then generates a world of its own, which is the right fallback for a game that can be
 * played anywhere.
 */
public class PreparedMap {

    private final File source;
    private final String target;

    /**
     * @param source where the map lies next to the launcher, like {@code ./hungergames-world}
     * @param target what the world folder is called on the server, {@code world} for the main world
     */
    public PreparedMap(String source, String target) {
        this.source = new File(source);
        this.target = target;
    }

    /**
     * @return whether there is a map to hand out
     */
    public boolean exists() {
        return new File(source, "level.dat").isFile();
    }

    /**
     * Puts the map onto one server, if there is one and the server has no world of that name yet.
     *
     * @param serverDirectory the server to install into
     * @return whether a map was copied
     */
    public boolean installInto(File serverDirectory) {
        if (!exists()) return false;
        File world = new File(serverDirectory, target);
        if (new File(world, "level.dat").isFile()) return false;
        try {
            FileTrees.copy(source.toPath(), world.toPath(), FileTrees.WORLD_IDENTITY);
            // a copied world that brings its lock and its player data along argues with the server it lands
            // in, and the players of the last game have no business in this one
            FileTrees.stripPlayers(world);
            System.out.println("Installed the map " + source.getPath() + " on " + serverDirectory.getName() + ".");
            return true;
        } catch (IOException e) {
            System.out.println("Could not install the map " + source.getPath() + " on "
                    + serverDirectory.getName() + ": " + e.getMessage() + " - a fresh world is generated.");
            return false;
        }
    }

}
