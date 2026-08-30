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
    private int generatorRadius;
    private int shopRadius;
    private boolean resourcesToKiller;
    private int respawnProtectionSeconds;
    private int enderPearlCooldownSeconds;
    private java.util.List<String> blastProof;
    private java.util.List<String> fireballProof;
    private double fireballDamageCap;
    private float fireballPower;
    private float tntPower;
    private boolean keepPlayingWhenOffline;
    private boolean statsEnabled;
    private String statsDirectory;

    public GameSettings() {
        file = new ConfigFile("game.yml");
        load();
    }

    /**
     * Reads the file, filling in and documenting every value that is missing.
     */
    public void load() {
        // from disk, not from the copy in memory: a reload that re-parses what it already had would
        // write its own stale values back over the edit that caused the reload
        file.reload();
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
        respawnProtectionSeconds = Math.max(0, file.get("respawn-protection-seconds", 3,
                "How long somebody who just respawned cannot be hurt.",
                "Without it, a player camping a spawn kills everybody the moment they appear."));
        endReturnSeconds = Math.max(0, file.get("end.return-seconds", 15,
                "How long the winners get to celebrate before everybody is sent back to the lobby."));
        emptyShutdownSeconds = Math.max(0, file.get("end.empty-shutdown-seconds", 60,
                "How long this server stays up with nobody on it before it stops itself.",
                "0 turns the self shutdown off, which is what you want while setting maps up."));
        generatorRadius = Math.max(0, file.get("build.generator-radius", 2,
                "How close to a diamond or emerald generator nobody may build.",
                "Without this, a generator can simply be walled in."));
        shopRadius = Math.max(0, file.get("build.shop-radius", 2,
                "How close to a shop keeper nobody may build."));
        resourcesToKiller = file.get("death.resources-to-killer", true,
                "Whether the resources somebody carried go to whoever killed them.",
                "Off means they are lost, which makes hunting people down pointless.");
        enderPearlCooldownSeconds = Math.max(0, file.get("ender-pearl-cooldown-seconds", 5,
                "How long after an ender pearl the next one may be thrown.",
                "0 turns it off. Without it a pearl is not an escape but a way of travelling."));
        keepPlayingWhenOffline = file.get("death.keep-place-when-offline", true,
                "Whether somebody who leaves while their bed still stands keeps their place in the round",
                "and can come back into it. With no bed left, leaving is always final.");
        blastProof = file.raw().getStringList("explosions.blast-proof");
        if (blastProof.isEmpty()) {
            blastProof = java.util.List.of("GLASS", "STAINED_GLASS", "GLASS_PANE");
            file.set("explosions.blast-proof", blastProof);
            file.raw().setComments("explosions", java.util.List.of(
                    "What survives an explosion, and how hard the two of them hit.",
                    "The entries are matched against the end of a block's name, so 'GLASS' covers every",
                    "colour of stained glass as well - which is what makes the blast-proof glass of the",
                    "shop worth its twelve iron."));
        }
        fireballProof = file.raw().getStringList("explosions.fireball-proof");
        if (fireballProof.isEmpty()) {
            fireballProof = java.util.List.of("END_STONE");
            file.set("explosions.fireball-proof", fireballProof);
        }
        fireballPower = (float) Math.max(0.5d, file.get("explosions.fireball-power", 2.5d,
                "How big the hole a fireball makes is."));
        tntPower = (float) Math.max(0.5d, file.get("explosions.tnt-power", 6.0d,
                "And how big the one tnt makes is. Vanilla tnt is 4."));
        fireballDamageCap = Math.max(0.0d, file.get("explosions.fireball-damage-cap", 5.0d,
                "The most a single fireball may take off a player, in half hearts.",
                "The blast still knocks them about - it just does not delete them."));

        statsEnabled = file.get("stats.enabled", true,
                "Whether the numbers of the round are written down when it ends.");
        statsDirectory = file.get("stats.directory", "./stats",
                "Where that file goes. One file per round, which is all a server ever plays.");
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

    public int getRespawnProtectionSeconds() {
        return respawnProtectionSeconds;
    }

    /**
     * @return how long a player has to wait between two ender pearls, 0 when they never do
     */
    public int getEnderPearlCooldownSeconds() {
        return enderPearlCooldownSeconds;
    }

    /**
     * @return whether leaving with a bed still standing keeps somebody in the round
     */
    public boolean isKeepPlayingWhenOffline() {
        return keepPlayingWhenOffline;
    }

    public boolean isStatsEnabled() {
        return statsEnabled;
    }

    public String getStatsDirectory() {
        return statsDirectory;
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

    /**
     * @param material a block that an explosion wants to take
     * @return whether it survives any explosion at all
     */
    public boolean isBlastProof(org.bukkit.Material material) {
        return matches(blastProof, material);
    }

    /**
     * @param material a block that a fireball wants to take
     * @return whether it survives a fireball, though not necessarily tnt
     */
    public boolean isFireballProof(org.bukkit.Material material) {
        return matches(fireballProof, material);
    }

    /**
     * @param names    what the config listed
     * @param material the block
     * @return whether the block's name ends with any of them
     */
    private static boolean matches(java.util.List<String> names, org.bukkit.Material material) {
        String name = material.name();
        for (String entry : names) {
            if (!entry.isBlank() && name.endsWith(entry.toUpperCase(java.util.Locale.ROOT))) return true;
        }
        return false;
    }

    public float getFireballPower() {
        return fireballPower;
    }

    public float getTntPower() {
        return tntPower;
    }

    /**
     * @return the most a fireball may take off one player, in half hearts
     */
    public double getFireballDamageCap() {
        return fireballDamageCap;
    }

    public int getGeneratorRadius() {
        return generatorRadius;
    }

    public int getShopRadius() {
        return shopRadius;
    }

    public boolean isResourcesToKiller() {
        return resourcesToKiller;
    }

    public ConfigFile getFile() {
        return file;
    }
}
