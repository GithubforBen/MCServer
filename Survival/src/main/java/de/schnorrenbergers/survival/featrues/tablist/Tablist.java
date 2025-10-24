package de.schnorrenbergers.survival.featrues.tablist;

import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class Tablist {
    public Tablist() {
        Bukkit.getScheduler().runTaskTimer(
                Survival.getInstance(),
                new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            player.sendPlayerListHeader(Component.text("-------Survival: " + Math.round(Bukkit.getServer().getTPS()[0]) + "-------"));
                            player.sendPlayerListFooter(Component.text(MoneyHandler.getMoney(player.getUniqueId() )+ "$"));
                        });
                    }
                }, 0L, 20L);
    }
}
