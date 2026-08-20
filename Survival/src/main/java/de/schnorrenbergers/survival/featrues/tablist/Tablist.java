package de.schnorrenbergers.survival.featrues.tablist;

import de.hems.paper.event.EventAnnouncer;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

/**
 * Header and footer of the tab list. The footer carries the money and, underneath it, what the events are
 * doing - that is where a player looks when they want to know how long they still have.
 */
public class Tablist {

    public Tablist() {
        Bukkit.getScheduler().runTaskTimer(Survival.getInstance(), Tablist::update, 0L, 20L);
    }

    private static void update() {
        Component events = EventAnnouncer.tabLine();
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendPlayerListHeader(Component.text(
                    "-------Survival: " + Math.round(Bukkit.getServer().getTPS()[0]) + "-------"));
            Component footer = Component.text(MoneyHandler.getMoney(player.getUniqueId()) + "$",
                    NamedTextColor.WHITE);
            if (events != null) {
                footer = footer.append(Component.newline()).append(events);
            }
            player.sendPlayerListFooter(footer);
        });
    }
}
