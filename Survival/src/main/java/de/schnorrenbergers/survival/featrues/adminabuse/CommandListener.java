package de.schnorrenbergers.survival.featrues.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.adminabuse.RequestAdminAbuseEvent;
import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

//A Listener which listens to all executed commands. When the command is a command which requires admin privileges a Adminaction is created and the user is given a hint to Legitimize it
public class CommandListener implements Listener {

    public CommandListener() {
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
    }

    @EventHandler
    public void onCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) throws Exception {
        if (!event.getPlayer().isOp()) {
            return;
        }
        for (String flag : flags()) {
            if (event.getMessage().startsWith("/" + flag)) {
                ListenerAdapter.sendListeners(new RequestAdminAbuseEvent(ListenerAdapter.ServerName.HOST, event.getMessage(), event.getPlayer().getName()));
                event.getPlayer().sendMessage("Authenticate this command with /legitamise");
            }
        }
    }

    @EventHandler
    public void onCommand(org.bukkit.event.player.PlayerGameModeChangeEvent event) throws Exception {
        ListenerAdapter.sendListeners(new RequestAdminAbuseEvent(ListenerAdapter.ServerName.HOST, "Game mode change to " + event.getNewGameMode().toString(), event.getPlayer().getName()));
        event.getPlayer().sendMessage("Authenticate this command with /legitamise");
    }

    //returns a List with all Admin abuse commands
    public List<String> flags() {
        List<String> list = new ArrayList<String>();
        list.add("gamemode");
        list.add("op");
        list.add("give");
        list.add("tp");
        list.add("fill");
        list.add("setblock");
        list.add("ban");
        list.add("kick");
        list.add("ban-ip");
        list.add("clear");
        list.add("clone");
        list.add("damage");
        list.add("kill");
        list.add("deop");
        list.add("difficulty");
        list.add("effect");
        list.add("enchant");
        list.add("execute");
        list.add("experience");
        list.add("gamerule");
        list.add("item");
        list.add("locate");
        list.add("pardon");
        list.add("place");
        list.add("spectate");
        list.add("xp");
        list.add("worldborder");
        list.add("weather");
        list.add("time");
        list.add("teleport");
        list.add("tag");
        list.add("summon");
        list.add("admin");
        return list;
    }
}

