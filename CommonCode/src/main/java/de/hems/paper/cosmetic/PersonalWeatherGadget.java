package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Its owner's own sky.
 * <p>
 * Everything here happens in one client: the server's time and weather are untouched, nobody else sees
 * any of it, and a player standing next to somebody wearing this is standing in the same rain they always
 * were. Which is what makes it the one gadget on this list that cannot affect a game - it changes a
 * picture and not the world the picture is of.
 */
public class PersonalWeatherGadget implements Gadget, Listener {

    /** The skies it cycles through, in order. */
    private enum Sky {

        NORMAL("wie überall", 0L, null),
        NOON("Mittagssonne", 6000L, WeatherType.CLEAR),
        NIGHT("Sternenhimmel", 18000L, WeatherType.CLEAR),
        RAIN("Regen", 6000L, WeatherType.DOWNFALL);

        private final String displayName;
        private final long time;
        private final WeatherType weather;

        Sky(String displayName, long time, WeatherType weather) {
            this.displayName = displayName;
            this.time = time;
            this.weather = weather;
        }
    }

    /** Which sky each wearer is currently under. */
    private final Map<UUID, Sky> skies = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return Cosmetics.GADGET_WEATHER;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.CLOCK, getId(), "Eigenes Wetter", "Rechtsklick: Himmel wechseln");
    }

    @Override
    public @Nullable String hint() {
        return "Eigenes Wetter: Rechtsklick wechselt deinen Himmel - nur deinen.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!GadgetItems.is(event.getItem(), getId())) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (Gadgets.settingsFor(player, getId()) == null) return;

        Sky[] skyline = Sky.values();
        Sky next = skyline[(skies.getOrDefault(player.getUniqueId(), Sky.NORMAL).ordinal() + 1)
                % skyline.length];
        skies.put(player.getUniqueId(), next);
        apply(player, next);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Dein Himmel: " + ChatColor.WHITE + next.displayName);
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
    }

    @Override
    public void cleanUp(Player player) {
        skies.remove(player.getUniqueId());
        apply(player, Sky.NORMAL);
    }

    private void apply(Player player, Sky sky) {
        if (sky == Sky.NORMAL) {
            player.resetPlayerTime();
            player.resetPlayerWeather();
            return;
        }
        // absolute rather than relative: a fixed sky is the point, and a relative one would drift with
        // the world's own clock until it is something nobody chose
        player.setPlayerTime(sky.time, false);
        if (sky.weather != null) player.setPlayerWeather(sky.weather);
    }
}
