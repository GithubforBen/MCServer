package de.schnorrenbergers.survival.featrues.chunklimiter;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.configs.RequestDataFromConfigEvent;
import org.bukkit.entity.Player;

import java.util.List;

public class PayingPlayer {
    private static List<?> players;
    private static long time = 0;
    public synchronized static boolean isPaying(Player player) throws Exception {
        if (System.currentTimeMillis() - time > 1000*30) {
            System.out.println("Reload time-:" +time);
            RequestDataFromConfigEvent event = new RequestDataFromConfigEvent("paying-players");
            ListenerAdapter.sendListeners(event);
            Object data = ListenerAdapter.waitForEvent(event.getEventId()).getData();
            if (data == null) return false;
            if (!(data instanceof List<?>)) return false;
            players = (List<?>) data;
            time = System.currentTimeMillis();
        }
        if (players.isEmpty()) return false;
        if (!(players.getFirst() instanceof String)) return false;
        return players.contains(player.getUniqueId().toString());
    }
}
