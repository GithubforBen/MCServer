package de.hems.paper.cosmetic;

import org.bukkit.plugin.Plugin;

/**
 * Everything a game server has to switch on to make the cosmetics work.
 * <p>
 * One call rather than four, because the four are not independent: a server that registers the win effects
 * but forgets the trails has players who bought something that silently does nothing, and nothing in the
 * menu would say so. Adding a kind of cosmetic must not mean editing three plugins.
 * <p>
 * {@link CosmeticService#init(Plugin)} is separate on purpose - it is the network connection, and a server
 * without one still wants its menus to open and say that nothing is loaded yet.
 */
public final class CosmeticEffects {

    private static boolean initialized;

    private CosmeticEffects() {
    }

    /**
     * Registers every effect this build has code for and starts what has to run.
     *
     * @param plugin the plugin they belong to
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized || plugin == null) return;
        initialized = true;

        WinEffects.register(new RocketWinEffect());
        WinEffects.register(new InkWinEffect());
        WinEffects.register(new StormWinEffect());
        WinEffects.register(new BeaconWinEffect());

        KillEffects.register(new LightningKillEffect());
        KillEffects.register(new SoulsKillEffect());
        KillEffects.register(new BlastKillEffect());

        Trails.register(new FlameTrail());
        Trails.register(new StardustTrail());
        Trails.register(new NotesTrail());

        Gadgets.register(plugin, new EndlessPearlGadget(plugin));
        Gadgets.register(plugin, new GrappleGadget());

        new CosmeticSafetyListener(plugin);
        new CosmeticKillListener(plugin);
        Trails.start(plugin);
    }
}
