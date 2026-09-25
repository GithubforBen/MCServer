package de.hems.utils.server;

import de.hems.files.FileTrees;
import java.io.File;
import java.io.IOException;

/**
 * The casino, carried from one poker night to the next.
 * <p>
 * A poker night gets a fresh server, and a fresh server means a fresh world - which would be fine if the
 * casino were generated content, and it is not meant to stay that way. It is built once in code and then
 * belongs to whoever builds on it, so it has to outlive the server it was built on. It does that here: the
 * room is kept in {@code ./poker-world} next to the launcher and copied onto every casino that is created
 * from then on.
 * <p>
 * The copy back out is not done here - it is done by the casino itself, with {@code /poker karte speichern},
 * because only somebody standing in the room knows when it is finished.
 * <p>
 * Nothing is overwritten. A casino server whose copy somebody has been editing keeps the edit; the folder
 * here is a source, not a master.
 */
public class CasinoMap {

    /** Where the casino lives between nights, next to the launcher. */
    public static final String DIRECTORY = "./poker-world";
    /** And what it is called on a casino server, which is where the plugin looks for it. */
    private static final String TARGET = "poker-world";

    private final File directory;

    public CasinoMap() {
        this(new File(DIRECTORY));
    }

    public CasinoMap(File directory) {
        this.directory = directory;
    }

    /**
     * @return whether there is a casino to hand out yet
     */
    public boolean exists() {
        return new File(directory, "level.dat").isFile();
    }

    /**
     * Puts the casino onto one server, if there is one and it is not there already.
     *
     * @param serverDirectory the server to install into
     */
    public void installInto(File serverDirectory) {
        if (!exists()) {
            // the first poker night of a network has nothing to copy, and that is not a problem: the
            // plugin builds a casino and writes it out here, and every night after this one gets that one
            return;
        }
        File target = new File(serverDirectory, TARGET);
        if (new File(target, "level.dat").isFile()) return;
        try {
            FileTrees.copy(directory.toPath(), target.toPath(), FileTrees.WORLD_IDENTITY);
            // a copied world that brings its lock and its player data along argues with the server it
            // lands in, so those stay behind
            FileTrees.stripPlayers(target);
            System.out.println("Installed the casino on " + serverDirectory.getName() + ".");
        } catch (IOException e) {
            // a casino that cannot be copied is a casino that gets built again, not a server that fails
            // to start
            System.out.println("Could not install the casino on " + serverDirectory.getName()
                    + ": " + e.getMessage() + " - a fresh one will be built there.");
        }
    }

}
