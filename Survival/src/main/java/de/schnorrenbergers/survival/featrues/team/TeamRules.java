package de.schnorrenbergers.survival.featrues.team;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The rules every team on this server plays by, backed by {@code ./configs/team.yml}.
 * <p>
 * These are the limits the server sets. What a team decides for itself - who may invite, whether the
 * backpack is open to everyone - lives in the team's own settings and is edited in the team manager. Here
 * are the bounds those settings have to stay inside, plus the numbers behind claiming.
 */
public class TeamRules {

    private final File file;
    private final YamlConfiguration config;

    private int maxMembersCap;
    private int minNameLength;
    private int maxNameLength;
    private int maxTagLength;
    private int claimBaseCost;
    private double claimGrowth;
    private int claimMaxCost;
    private int maxClaimsPerTeam;
    private boolean allowRename;
    private boolean allowDisband;
    private boolean allowPublicJoin;
    private int homeCooldownSeconds;
    private int homeWarmupSeconds;

    public TeamRules() {
        file = new File("./configs/team.yml");
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    /**
     * Reads the file, writing back everything that was missing so the file documents itself.
     */
    public final void load() {
        maxMembersCap = get("members.maximum", 8,
                "The largest a team may ever be. A team can set itself a lower limit in its manager.");
        minNameLength = get("name.minimum-length", 3, "The shortest a team name may be.");
        maxNameLength = get("name.maximum-length", 16, "The longest a team name may be.");
        maxTagLength = get("name.maximum-tag-length", 5, "The longest a team tag may be.");

        claimBaseCost = get("claims.base-cost", 50, "What the first claimed chunk costs.");
        claimGrowth = get("claims.growth", 1.1d,
                "How much more each further chunk costs. 1.1 means ten percent per chunk.");
        claimMaxCost = get("claims.maximum-cost", 1000, "The most a single chunk can ever cost.");
        maxClaimsPerTeam = get("claims.maximum-per-team", 0,
                "How many chunks one team may own. 0 means no limit.");

        allowRename = get("permissions.allow-rename", true, "Whether a leader may rename their team.");
        allowDisband = get("permissions.allow-disband", true, "Whether a leader may disband their team.");
        allowPublicJoin = get("permissions.allow-public-join", true,
                "Whether teams may open themselves for anybody to join.");

        homeCooldownSeconds = get("home.cooldown-seconds", 60,
                "How long a player has to wait between two uses of the team home.");
        homeWarmupSeconds = get("home.warmup-seconds", 3,
                "How long a player has to stand still before the teleport happens. 0 disables the wait.");
        save();
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String path, T fallback, String... comments) {
        if (!config.contains(path)) {
            config.set(path, fallback);
            if (comments.length > 0) config.setComments(path, List.of(comments));
            return fallback;
        }
        if (fallback instanceof Integer) return (T) Integer.valueOf(config.getInt(path));
        if (fallback instanceof Double) return (T) Double.valueOf(config.getDouble(path));
        if (fallback instanceof Boolean) return (T) Boolean.valueOf(config.getBoolean(path));
        Object value = config.get(path);
        return value == null ? fallback : (T) value;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            Survival.getInstance().getLogger().warning("Could not save team.yml: " + e.getMessage());
        }
    }

    /**
     * What the next chunk costs a team. Every chunk a team already owns makes the next one dearer, so a
     * team cannot cheaply fence off half the world.
     *
     * @param ownedChunks how many chunks the team has already
     * @return the price of the next one
     */
    public int claimCost(int ownedChunks) {
        double cost = claimBaseCost * Math.pow(claimGrowth, Math.max(0, ownedChunks));
        if (cost > claimMaxCost || Double.isInfinite(cost)) return claimMaxCost;
        return (int) Math.round(cost);
    }

    /**
     * @param name the name a player typed
     * @return what is wrong with it, or {@code null} if it is fine
     */
    public String validateName(String name) {
        if (name == null || name.isBlank()) return "Der Teamname fehlt.";
        String trimmed = name.trim();
        if (trimmed.length() < minNameLength) {
            return "Der Teamname muss mindestens " + minNameLength + " Zeichen lang sein.";
        }
        if (trimmed.length() > maxNameLength) {
            return "Der Teamname darf höchstens " + maxNameLength + " Zeichen lang sein.";
        }
        if (!trimmed.matches("[A-Za-z0-9_-]+")) {
            return "Der Teamname darf nur Buchstaben, Zahlen, _ und - enthalten.";
        }
        return null;
    }

    public int getMaxMembersCap() {
        return maxMembersCap;
    }

    public int getMaxTagLength() {
        return maxTagLength;
    }

    public int getMaxClaimsPerTeam() {
        return maxClaimsPerTeam;
    }

    public boolean isAllowRename() {
        return allowRename;
    }

    public boolean isAllowDisband() {
        return allowDisband;
    }

    public boolean isAllowPublicJoin() {
        return allowPublicJoin;
    }

    public int getHomeCooldownSeconds() {
        return homeCooldownSeconds;
    }

    public int getHomeWarmupSeconds() {
        return homeWarmupSeconds;
    }
}
