package de.schnorrenbergers.bedwars.config;

import de.schnorrenbergers.bedwars.util.ConfigFile;

import java.util.EnumMap;
import java.util.Map;

/**
 * Which of the switches are on, out of {@code features.yml}.
 * <p>
 * Written to the file the moment one is flipped, so a server that is restarted comes back the way it was
 * left - an admin who turned the locator bar off should not have to do it again after every crash.
 */
public final class FeatureSettings {

    /** Which defaults the file was last brought up to, see {@link #load}. */
    private static final String VERSION = "defaults-version";

    private final ConfigFile file;
    private final Map<Feature, Boolean> state = new EnumMap<>(Feature.class);

    public FeatureSettings() {
        file = new ConfigFile("features.yml");
        load();
    }

    /**
     * Reads the file, writing in and explaining every switch that is missing.
     */
    public void load() {
        file.reload();
        file.section("features",
                "The switches of this server. /bw admin is the same list as a menu, and flipping one",
                "there writes it back in here.");
        state.clear();
        for (Feature feature : Feature.values()) {
            state.put(feature, file.get("features." + feature.getKey(), feature.isDefault(),
                    feature.getDescription()));
        }
        // 1.8 combat used to be off by default, and every file written back then says so - which is the
        // old default written down, not a decision. It is switched on once; turned off after that, it stays off
        if (file.read(VERSION, 1) < 2) {
            state.put(Feature.OLD_PVP, true);
            file.set("features." + Feature.OLD_PVP.getKey(), true);
            file.set(VERSION, 2);
        }
        file.save();
    }

    /**
     * @param feature the switch
     * @return whether it is on
     */
    public boolean is(Feature feature) {
        return state.getOrDefault(feature, feature.isDefault());
    }

    /**
     * Flips one switch and writes it down.
     *
     * @param feature the switch
     * @param on      what it should be
     */
    public void set(Feature feature, boolean on) {
        state.put(feature, on);
        file.set("features." + feature.getKey(), on);
        file.save();
    }

    /**
     * @param feature the switch
     * @return what it is now, after being flipped
     */
    public boolean toggle(Feature feature) {
        boolean on = !is(feature);
        set(feature, on);
        return on;
    }
}
