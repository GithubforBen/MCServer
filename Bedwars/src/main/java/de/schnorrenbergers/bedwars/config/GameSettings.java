package de.schnorrenbergers.bedwars.config;

import de.schnorrenbergers.bedwars.util.ConfigFile;

/**
 * The knobs of the round itself, out of {@code game.yml}.
 * <p>
 * A server hosts exactly one round, so these are the settings of this server: which mode it plays, which
 * map it uses, and the timings around the round. Anything that belongs to a map lives in the map file
 * instead, and anything an event decides overrides what is written here.
 */
public final class GameSettings {

    private final ConfigFile file;

    private String mode;
    private String map;
    private int minimumPlayers;
    private int maximumPlayers;
    private int lobbyCountdownSeconds;
    private int fullLobbyCountdownSeconds;
    private int respawnSeconds;
    private int endReturnSeconds;
    private int emptyShutdownSeconds;
    private boolean stopServerWhenDone;

    public GameSettings() {
        file = new ConfigFile("game.yml");
        load();
    }

    /**
     * Reads the file, filling in and documenting every value that is missing.
     */
    public void load() {
        mode = file.get("mode", ModeSettings.DEFAULT_MODE,
                "Which mode of modes.yml this server plays.",
                "An event that starts this server overrides it.");
        map = file.get("map", "",
                "Which map to play. Empty means: pick one at random that fits the mode.");

        minimumPlayers = Math.max(2, file.get("players.minimum", 2,
                "How many players have to be here before the countdown starts."));
        maximumPlayers = Math.max(0, file.get("players.maximum", 0,
                "How many players are let in. 0 means: as many as the mode holds."));

        lobbyCountdownSeconds = Math.max(5, file.get("countdown.lobby-seconds", 60,
                "How long the waiting lobby counts down once there are enough players."));
        fullLobbyCountdownSeconds = Math.max(3, file.get("countdown.full-lobby-seconds", 15,
                "The countdown is shortened to this once the lobby is full."));

        respawnSeconds = Math.max(0, file.get("respawn-seconds", 5,
                "How long a player whose bed still stands waits before coming back."));
        endReturnSeconds = Math.max(0, file.get("end.return-seconds", 15,
                "How long the winners get to celebrate before everybody is sent back to the lobby."));
        emptyShutdownSeconds = Math.max(0, file.get("end.empty-shutdown-seconds", 60,
                "How long this server stays up with nobody on it before it stops itself.",
                "0 turns the self shutdown off, which is what you want while setting maps up."));
        stopServerWhenDone = file.get("end.stop-server", true,
                "Whether this server asks the launcher to stop it once the round is over.",
                "Turn it off while developing, or the server disappears under you after every test.");
        file.save();
    }

    public String getMode() {
        return mode;
    }

    public String getMap() {
        return map;
    }

    public int getMinimumPlayers() {
        return minimumPlayers;
    }

    /**
     * @param modeMaximum how many players the mode holds
     * @return how many players are let in
     */
    public int getMaximumPlayers(int modeMaximum) {
        return maximumPlayers <= 0 ? modeMaximum : Math.min(maximumPlayers, modeMaximum);
    }

    public int getLobbyCountdownSeconds() {
        return lobbyCountdownSeconds;
    }

    public int getFullLobbyCountdownSeconds() {
        return fullLobbyCountdownSeconds;
    }

    public int getRespawnSeconds() {
        return respawnSeconds;
    }

    public int getEndReturnSeconds() {
        return endReturnSeconds;
    }

    public int getEmptyShutdownSeconds() {
        return emptyShutdownSeconds;
    }

    public boolean isStopServerWhenDone() {
        return stopServerWhenDone;
    }

    public ConfigFile getFile() {
        return file;
    }
}
