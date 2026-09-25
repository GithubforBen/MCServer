package de.schnorrenbergers.lobby.lotto;

import de.hems.paper.hologram.Hologram;
import de.hems.paper.lotto.LottoClient;
import de.hems.paper.lotto.LottoMenu;
import de.hems.types.lotto.LottoStatus;
import de.hems.types.lotto.LottoTicket;
import de.schnorrenbergers.lobby.LobbyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * The lotto stand in the lobby: a villager that opens the slip, and over it the pot and when the draw is.
 * <p>
 * An admin puts it where they stand with {@code /lotto stand}, and takes it away with
 * {@code /lotto standweg}. The villager is not saved with the world: it is put back on every start, and
 * again whenever its chunk was unloaded, so there is never a second one standing next to it.
 */
public final class LottoStand implements Listener {

    private final Plugin plugin;
    private final File file;
    private final NamespacedKey key;
    private Location location;
    private Villager villager;
    private Hologram sign;

    public LottoStand(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lotto-stand.yml");
        this.key = new NamespacedKey(plugin, "lotto-stand");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        World world = LobbyWorld.get();
        if (world != null && config.contains("x")) {
            location = new Location(world, config.getDouble("x"), config.getDouble("y"), config.getDouble("z"),
                    (float) config.getDouble("yaw"), 0f);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        LottoClient.onChange(this::updateSign);
        // puts it back when its chunk comes back, and keeps the countdown on the sign current
        Bukkit.getScheduler().runTaskTimer(plugin, this::ensure, 40L, 200L);
    }

    /**
     * Puts the stand where the admin stands.
     */
    public void place(Player player) {
        if (LobbyWorld.get() == null || !player.getWorld().equals(LobbyWorld.get())) {
            player.sendMessage(Component.text("Der Stand gehört in die Lobby-Welt.", NamedTextColor.RED));
            return;
        }
        remove();
        Location at = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        at.setYaw(player.getLocation().getYaw());
        location = at;
        YamlConfiguration config = new YamlConfiguration();
        config.set("x", at.getX());
        config.set("y", at.getY());
        config.set("z", at.getZ());
        config.set("yaw", at.getYaw());
        save(config);
        ensure();
        player.sendMessage(Component.text("✓ Der Lotto-Stand steht hier.", NamedTextColor.GREEN));
    }

    /**
     * Takes the stand away for good.
     */
    public void remove(Player player) {
        remove();
        location = null;
        save(new YamlConfiguration());
        player.sendMessage(Component.text("✓ Der Lotto-Stand ist weg.", NamedTextColor.GREEN));
    }

    private void save(YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save the lotto stand: " + e.getMessage());
        }
    }

    private void remove() {
        if (villager != null && villager.isValid()) villager.remove();
        villager = null;
        if (sign != null) sign.remove();
        sign = null;
    }

    /**
     * Makes sure the villager and the sign are there, if the stand has a place and its chunk is loaded.
     */
    private void ensure() {
        if (location == null || location.getWorld() == null) return;
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;
        if (villager == null || !villager.isValid()) {
            // one left over from before a crash would have been saved with the chunk - there is only ever one
            for (Entity entity : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
                if (entity.getPersistentDataContainer().has(key)) entity.remove();
            }
            villager = location.getWorld().spawn(location, Villager.class, spawned -> {
                spawned.setPersistent(false);
                spawned.setAI(false);
                spawned.setInvulnerable(true);
                spawned.setSilent(true);
                spawned.setCollidable(false);
                spawned.setProfession(Villager.Profession.CLERIC);
                spawned.customName(Component.text("Lotto", NamedTextColor.GOLD, TextDecoration.BOLD));
                spawned.setCustomNameVisible(false);
                spawned.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            });
        }
        if (sign == null) sign = new Hologram(location).height(2.6);
        updateSign();
    }

    private void updateSign() {
        if (sign == null) return;
        LottoStatus status = LottoClient.getStatus();
        if (!status.isKnown()) {
            sign.setLines(Component.text("LOTTO", NamedTextColor.GOLD, TextDecoration.BOLD));
        } else {
            sign.setLines(
                    Component.text("LOTTO · 4 aus 15", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("Topf: " + status.getPot() + " Bits", NamedTextColor.YELLOW),
                    Component.text("Ziehung in " + LottoStatus.span(status.getNextDrawAt() - System.currentTimeMillis()),
                            NamedTextColor.GRAY),
                    status.getLastDraw() == null ? Component.text("Klick mich zum Tippen", NamedTextColor.GREEN)
                            : Component.text("Zuletzt: " + LottoTicket.format(status.getLastDraw().getNumbers()),
                            NamedTextColor.GRAY));
        }
        sign.spawn();
    }

    private boolean isStand(Entity entity) {
        return entity.getPersistentDataContainer().has(key);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(PlayerInteractEntityEvent event) {
        if (!isStand(event.getRightClicked())) return;
        // no trading screen, the slip instead
        event.setCancelled(true);
        // the event comes once per hand
        if (event.getHand() != EquipmentSlot.HAND) return;
        LottoMenu.open(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (isStand(event.getEntity())) event.setCancelled(true);
    }
}
