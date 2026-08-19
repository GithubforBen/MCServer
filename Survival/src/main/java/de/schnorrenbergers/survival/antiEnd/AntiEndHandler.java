package de.schnorrenbergers.survival.antiEnd;

import de.hems.paper.event.EventService;
import de.hems.types.event.EventType;
import de.schnorrenbergers.survival.Survival;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Decides whether the End is open.
 * <p>
 * This used to be a config flag flipped by hand with {@code /end-allow}. The End is now opened by an event
 * of type {@link EventType#END}: once that event starts, the End stays open for good. The old flag is still
 * read as an override, so a network that already opened the End does not close it again on update.
 */
public class AntiEndHandler {

    /**
     * @return whether players may enter the End
     */
    public static boolean allowEnd() {
        // the event is the normal way in; the flag only survives for networks that opened it before
        if (EventService.hasHappened(EventType.END)) return true;
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        return config.getBoolean("allow-end", false);
    }

    /**
     * @param allow whether the End should be open regardless of any event
     */
    public static void setAllowEnd(boolean allow) {
        YamlConfiguration config = Survival.getInstance().getMoneyConfig().getConfig();
        config.set("allow-end", allow);
        Survival.getInstance().getMoneyConfig().save();
    }
}
