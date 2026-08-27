package de.hems.types;

import java.io.Serializable;

/**
 * How far a server is with starting up.
 * <p>
 * A minecraft server opens its port long before it accepts players: paper binds the socket, then loads the
 * worlds, then generates the spawn area, and only after all of that is it done. Looking at the port alone
 * therefore says "the server is up" while it is still building terrain, and everybody who is warped at that
 * moment is thrown out again by the proxy. The launcher reads the phase off the server console and sends it
 * with every {@link Server} snapshot, so the rest of the network can wait for {@link #READY} instead of
 * guessing - and can tell a player what is actually happening while they wait.
 */
public enum ServerPhase implements Serializable {

    /** The launcher accepted the server but has not started the process yet. */
    QUEUED("In der Startwarteschlange"),
    /** The process runs and paper is booting: jar, configs, plugins. */
    STARTING("Server startet"),
    /** The worlds are being loaded and the spawn area is being generated. */
    GENERATING("Terrain wird gebaut"),
    /** Paper is done and the proxy can hand players over. */
    READY("Server bereit"),
    /** A stop was asked for, no new players belong here. */
    STOPPING("Server fährt herunter"),
    /** The server is not running. */
    OFFLINE("Offline");

    private final String description;

    ServerPhase(String description) {
        this.description = description;
    }

    /**
     * @return what to tell a waiting player about this phase
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return whether the server accepts players in this phase
     */
    public boolean isReady() {
        return this == READY;
    }

    /**
     * @return whether the server is on its way up and worth waiting for
     */
    public boolean isStartingUp() {
        return this == QUEUED || this == STARTING || this == GENERATING;
    }
}
