package de.schnorrenbergers.hungergames;

import de.hems.paper.NetworkPlugin;
import de.hems.paper.PluginCommands;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.ServerIdentity;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The plugin an arena carries.
 * <p>
 * An arena exists for one hunger games event. The lobby puts it up a few minutes before the event and
 * writes its name onto the event; the arena reads its rules from there, fills the map, waits for its players
 * and plays one game. The launcher switches it off and throws it away when the event is settled.
 * <p>
 * The order in {@link #onEnable} matters: the network first, because the rules come from the event; the
 * map second, because the game needs its middle.
 */
public final class HungerGamesPlugin extends JavaPlugin {

    private Game game;

    @Override
    public void onEnable() {
        String self = ServerIdentity.of(this, "HUNGER_GAMES").toString();
        if (!NetworkPlugin.connect(this, "HUNGER_GAMES")) {
            // without the network the game can still be played, it just counts for nothing
            getLogger().warning("The game can be played, but nothing is reported.");
        }

        ArenaContext.load(self);
        Loot.load(this);
        ArenaMap map = ArenaMap.load(this);

        game = new Game(this, map, ArenaContext.getSettings());
        getServer().getPluginManager().registerEvents(new GameListener(this, game), this);
        PluginCommands.register(this, "hg", new HgCommand(game));
        game.start();
        // on 26.3 allow-nether=false no longer keeps the nether from loading; nobody can get there (the
        // portals are refused), but it holds memory the arena needs. So it is unloaded once the server is up
        getServer().getScheduler().runTask(this, this::unloadOtherDimensions);
    }

    private void unloadOtherDimensions() {
        org.bukkit.World main = getServer().getWorlds().getFirst();
        for (org.bukkit.World world : new java.util.ArrayList<>(getServer().getWorlds())) {
            if (world.equals(main) || world.getEnvironment() == org.bukkit.World.Environment.NORMAL) continue;
            boolean unloaded = getServer().unloadWorld(world, false);
            getLogger().info((unloaded ? "Unloaded " : "Could not unload ") + world.getName()
                    + " - the arena is the one world.");
        }
    }

    @Override
    public void onDisable() {
        if (game != null) game.shutdown();
        // while the jar is still open, see ListenerAdapter.disconnect()
        ListenerAdapter.disconnect();
    }

}
