package de.hems.types.restart;

import java.io.Serializable;
import java.util.Locale;

/**
 * What happens when a scheduled restart of the network comes due.
 * <p>
 * The launcher does the first half - kick, save, stop every server and wait until each of them is really
 * gone - and then ends with an exit code that tells {@code run.sh} which of these to do next.
 */
public enum RestartMode implements Serializable {

    /** Start again on the code that is there. */
    RESTART("Neustart", "neustart", 10),
    /** Fetch the newest code with {@code git pull}, build it, start again. Falls back to the old code. */
    UPDATE("Update", "update", 11),
    /** Stay off. */
    SHUTDOWN("Herunterfahren", "aus", 0);

    private final String title;
    private final String word;
    private final int exitCode;

    RestartMode(String title, String word, int exitCode) {
        this.title = title;
        this.word = word;
        this.exitCode = exitCode;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @return what it is called in {@code /neustart}
     */
    public String getWord() {
        return word;
    }

    /**
     * @return the exit code the launcher ends with, which {@code run.sh} reads
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * @param word what was typed
     * @return the mode, or {@code null} if there is none by that name
     */
    public static RestartMode byWord(String word) {
        if (word == null) return null;
        for (RestartMode mode : values()) {
            if (mode.word.equals(word.toLowerCase(Locale.ROOT)) || mode.name().equalsIgnoreCase(word)) {
                return mode;
            }
        }
        return null;
    }
}
