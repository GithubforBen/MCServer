package de.schnorrenbergers.poker;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.ServerIdentity;
import de.hems.paper.admin.PlayerAdminHandler;
import de.hems.paper.commands.EventCommand;
import de.hems.paper.commands.LobbyCommand;
import de.hems.paper.commands.ServerManagerCommand;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.hems.paper.event.EventService;
import de.hems.paper.money.MoneyService;
import de.hems.paper.poker.PokerStatsService;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.event.PokerEventSettings;
import de.schnorrenbergers.poker.command.PokerCommand;
import de.schnorrenbergers.poker.listener.TableListener;
import de.schnorrenbergers.poker.world.CasinoLayout;
import de.schnorrenbergers.poker.world.CasinoWorld;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The plugin a casino server carries.
 * <p>
 * A casino exists for one poker night. It is put up by the lobby a few minutes before the night starts, it
 * finds out which night it is dealing by looking its own name up in the calendar, and it is switched off and
 * thrown away when the night is settled.
 * <p>
 * The order in {@link #onEnable} is not free. The network comes first, because without it there is no money
 * and no event; the event comes before the world, because how many tables to build is a setting of the
 * event; and the tables come last, because they need both.
 * <p>
 * <b>What happens when this server dies.</b> Nothing is lost. Every chip in front of every player is
 * reported to the launcher as it changes, and the launcher hands those stacks back when it settles the
 * night. So a crash costs the evening, never the money.
 */
public final class PokerPlugin extends JavaPlugin {

    private static PokerPlugin instance;
    private CasinoLayout layout;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        new CustomInventoryListener(this);
        ServerConnector.register(this);
        // the way out has to work even when nothing else does
        registerCommand("warp", new WarpCommand());
        registerCommand("lobby", new LobbyCommand());
        registerCommand("servermanger", new ServerManagerCommand());

        String self;
        try {
            self = ServerIdentity.of(this, "POKER").toString();
            new ListenerAdapter(ServerIdentity.of(this, "POKER"));
        } catch (Exception e) {
            // without the network there is no money, and a poker table without money is a table where
            // somebody is going to be told their bits vanished. Better to have no table
            getLogger().severe("No network connection (" + e.getMessage()
                    + "). No table is opened - poker is played for real bits and those live on the host.");
            return;
        }

        new PlayerAdminHandler(this);
        EventService.init(this);
        MoneyService.init(this);
        PokerStatsService.init(this);
        registerCommand("events", new EventCommand());

        // which night this is, and what it is played for. Blocking on purpose: the stakes have to be known
        // before the first player can sit down
        CasinoContext.load(self);
        PokerEventSettings settings = CasinoContext.getSettings();

        layout = new CasinoLayout(this);
        World world = CasinoWorld.load(this, layout, settings.getTables(), settings.getSeats());
        if (world == null) {
            getLogger().severe("The casino could not be built - no table is opened.");
            return;
        }

        Casino.open(this, layout, settings);
        new TableListener(this, layout);
        registerCommand("poker", new PokerCommand(this));

        getLogger().info("The casino is open: " + settings.getTables() + " tables, "
                + settings.getSeats() + " seats each, buy-in " + settings.getBuyIn()
                + ", house " + settings.getRakeText() + ".");
    }

    @Override
    public void onDisable() {
        // everybody is paid out before the jar closes. The launcher would hand the stacks back anyway when
        // the night is settled, but in the ordinary case - the night ends, the server stops - nobody should
        // have to wait for that to see their own money
        try {
            Casino.close();
        } catch (Exception e) {
            getLogger().severe("The tables could not be settled cleanly (" + e.getMessage()
                    + ") - the host still holds every open stack and hands them back.");
        }
        if (layout != null) layout.save();
        // while the jar is still open, see ListenerAdapter.disconnect()
        ListenerAdapter.disconnect();
    }

    private void registerCommand(String commandName, Object command) {
        PluginCommand registered = getCommand(commandName);
        if (registered == null) {
            getLogger().warning("The command /" + commandName + " is not declared in plugin.yml.");
            return;
        }
        registered.setExecutor((CommandExecutor) command);
        if (command instanceof TabCompleter completer) registered.setTabCompleter(completer);
    }

    public static PokerPlugin getInstance() {
        return instance;
    }

    public CasinoLayout getLayout() {
        return layout;
    }
}
