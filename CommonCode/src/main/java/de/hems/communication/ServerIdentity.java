package de.hems.communication;

import java.io.File;

/**
 * Finds out which server the plugin is running on.
 * <p>
 * The launcher starts every server with {@code -Dmcserver.name=<NAME>}, so even servers that were created
 * on the fly know their own name and can join the network under it. If that property is missing the folder
 * name is used, and only then the fallback the plugin passes in.
 */
public final class ServerIdentity {

    /** The system property the launcher sets on every server it starts. */
    public static final String PROPERTY = "mcserver.name";

    private ServerIdentity() {
    }

    /**
     * @param fallback the name to use if nothing else is known, e.g. {@code ServerName.LOBBY}
     * @return the name this server runs under
     */
    public static ListenerAdapter.ServerName resolve(ListenerAdapter.ServerName fallback) {
        String property = System.getProperty(PROPERTY);
        if (property != null && !property.isBlank()) {
            return ListenerAdapter.ServerName.valueOf(property);
        }
        try {
            String directory = new File(".").getAbsoluteFile().getParentFile().getName();
            if (directory != null && !directory.isBlank()) {
                return ListenerAdapter.ServerName.valueOf(directory);
            }
        } catch (Exception ignored) {
            // not started by the launcher, fall through
        }
        return fallback;
    }
}
