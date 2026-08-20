package de.schnorrenbergers.bedwars.addon;

import de.schnorrenbergers.bedwars.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * The usual shape of an addon: a listener that is registered while it is on.
 * <p>
 * Switching off is {@link HandlerList#unregisterAll(Listener)} and nothing else, which is exactly the
 * point - an addon that needs more than that to disappear is doing something it should not.
 */
public abstract class ListeningAddon implements Addon, Listener {

    private final Plugin plugin;
    private boolean listening;

    protected ListeningAddon(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public final void enable(Game game) {
        if (listening) return;
        listening = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        onEnable(game);
    }

    @Override
    public final void disable(Game game) {
        if (!listening) return;
        listening = false;
        HandlerList.unregisterAll(this);
        onDisable(game);
    }

    /**
     * Runs after the listener is registered, for setting up whatever else the addon needs.
     *
     * @param game the round
     */
    protected void onEnable(Game game) {
    }

    /**
     * Runs after the listener is gone, for taking that away again.
     *
     * @param game the round
     */
    protected void onDisable(Game game) {
    }

    protected Plugin getPlugin() {
        return plugin;
    }

    public boolean isListening() {
        return listening;
    }
}
