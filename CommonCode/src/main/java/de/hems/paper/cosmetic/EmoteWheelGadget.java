package de.hems.paper.cosmetic;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * A handful of gestures, out of a menu.
 * <p>
 * Without an animation system a gesture is a sound, a puff of particles and a line everybody nearby can
 * read - so that is exactly what these are, and the menu says as much rather than promising a dance the
 * client cannot play. It is honest and it costs nothing, which is the right trade for a cosmetic.
 */
public class EmoteWheelGadget implements Gadget, Listener {

    /** How long before the next gesture, in ticks. */
    private static final int DEFAULT_COOLDOWN_TICKS = 40;
    /** How far away a gesture is heard and read, in blocks. */
    private static final double RANGE = 16.0d;

    /** One gesture: what it is called, what it looks like, and what everybody nearby is told. */
    private record Emote(String name, Material icon, String said, Particle particle, Sound sound) {
    }

    private static final List<Emote> EMOTES = List.of(
            new Emote("Winken", Material.FEATHER, "winkt", Particle.END_ROD, Sound.ENTITY_VILLAGER_YES),
            new Emote("Jubeln", Material.FIREWORK_ROCKET, "jubelt", Particle.HAPPY_VILLAGER,
                    Sound.ENTITY_PLAYER_LEVELUP),
            new Emote("Verbeugen", Material.LEATHER_HELMET, "verbeugt sich", Particle.CLOUD,
                    Sound.BLOCK_NOTE_BLOCK_HARP),
            new Emote("Lachen", Material.PUFFERFISH, "lacht", Particle.NOTE,
                    Sound.ENTITY_VILLAGER_CELEBRATE));

    @Override
    public String getId() {
        return Cosmetics.GADGET_EMOTES;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.NAME_TAG, getId(), "Emotes", "Rechtsklick: Menü");
    }

    @Override
    public @Nullable String hint() {
        return "Emotes: Rechtsklick öffnet das Menü mit den Gesten.";
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
        CustomInventory.show(player, menu());
    }

    private CustomInventory menu() {
        CustomInventory menu = new CustomInventory(9, "Emotes", null);
        menu.fillPlaceHolder();
        // the four in the middle, so the row reads as a choice rather than as an inventory
        int[] places = {2, 3, 5, 6};
        for (int i = 0; i < EMOTES.size() && i < places.length; i++) {
            Emote emote = EMOTES.get(i);
            menu.setItem(places[i], new ItemApi(emote.icon(), ChatColor.LIGHT_PURPLE + emote.name(),
                            List.of(ChatColor.GRAY + "Alle in der Nähe sehen es")).build(),
                    new SimpleItemAction(event -> play((Player) event.getWhoClicked(), emote)));
        }
        return menu;
    }

    private void play(Player player, Emote emote) {
        player.closeInventory();
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;
        if (player.hasCooldown(Material.NAME_TAG)) return;
        player.setCooldown(Material.NAME_TAG, Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS)));

        Location at = player.getLocation().add(0.0d, 1.0d, 0.0d);
        player.getWorld().spawnParticle(emote.particle(), at, 20, 0.4d, 0.4d, 0.4d, 0.02d);
        player.getWorld().playSound(at, emote.sound(), 0.7f, 1.2f);
        String line = ChatColor.LIGHT_PURPLE + player.getName() + " " + emote.said() + ".";
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(at) <= RANGE * RANGE) nearby.sendMessage(line);
        }
    }
}
