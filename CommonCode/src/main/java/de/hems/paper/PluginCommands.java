package de.hems.paper;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hooks a command up to the plugin that declares it.
 * <p>
 * Every plugin of the network used to carry its own copy of this, and two of them cast every command to a
 * tab completer and trusted plugin.yml blindly - one command without either would have thrown out of
 * {@code onEnable} and left the plugin half started. Here a missing declaration is a warning, and tab
 * completion is used when the command offers it.
 */
public final class PluginCommands {

    private PluginCommands() {
    }

    /**
     * @param plugin  the plugin whose plugin.yml declares the command
     * @param name    the command, without the slash
     * @param command the executor, and tab completer if it is one
     */
    public static void register(JavaPlugin plugin, String name, CommandExecutor command) {
        PluginCommand registered = plugin.getCommand(name);
        if (registered == null) {
            plugin.getLogger().warning("The command /" + name + " is not declared in plugin.yml.");
            return;
        }
        registered.setExecutor(command);
        if (command instanceof TabCompleter completer) registered.setTabCompleter(completer);
    }
}
