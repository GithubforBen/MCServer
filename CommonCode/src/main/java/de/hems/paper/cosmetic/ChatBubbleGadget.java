package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * What its owner just said, over their head for a few seconds.
 * <p>
 * The text rides its owner rather than being carried after them: a display that is a passenger moves with
 * the player the client already knows about, which is smoother than anything a loop can do and survives
 * the player sprinting, riding and falling without a single teleport.
 * <p>
 * Chat arrives off the main thread, so the bubble is put up from a task on it. What is shown is the plain
 * text of the message and never its formatting - a bubble is not a second chat, and colour codes over
 * somebody's head are how a bubble becomes an advertisement.
 */
public class ChatBubbleGadget implements Gadget, Listener {

    /** How long a bubble stays, in ticks. */
    private static final int DEFAULT_DURATION_TICKS = 100;
    /** How many characters of a message are shown. */
    private static final int MAX_LENGTH = 48;
    /** How high above its owner's head the text sits, in blocks. */
    private static final float HEIGHT = 0.6f;

    private final Plugin plugin;
    private final GadgetEntities bubbles = new GadgetEntities();

    public ChatBubbleGadget(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return Cosmetics.GADGET_CHAT_BUBBLE;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public @Nullable String hint() {
        return "Chat-Blase: was du schreibst, steht kurz über deinem Kopf.";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String said = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (said.isEmpty()) return;
        String shown = said.length() <= MAX_LENGTH ? said : said.substring(0, MAX_LENGTH - 1) + "…";
        Bukkit.getScheduler().runTask(plugin, () -> show(player, shown));
    }

    /**
     * Puts one bubble up, replacing whatever was there.
     */
    private void show(Player player, String said) {
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null || !player.isOnline()) return;
        bubbles.remove(player);

        Entity spawned = player.getWorld().spawnEntity(player.getLocation(), EntityType.TEXT_DISPLAY);
        if (!(spawned instanceof TextDisplay display)) {
            spawned.remove();
            return;
        }
        display.text(Component.text(said));
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(false);
        display.setShadowed(false);
        Transformation shape = display.getTransformation();
        shape.getTranslation().set(0.0f, HEIGHT, 0.0f);
        display.setTransformation(shape);
        bubbles.keep(player, display);
        player.addPassenger(display);

        int life = Math.max(20, gadget.getNumber(Cosmetics.SETTING_DURATION_TICKS,
                DEFAULT_DURATION_TICKS));
        java.util.UUID shownId = display.getUniqueId();
        // this bubble's timer takes down this bubble. Somebody who says a second thing has a second one
        // up by then, and a timer that took down whatever is current would cut that one short
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player still = Bukkit.getPlayer(player.getUniqueId());
            if (still == null) return;
            Entity current = bubbles.of(still);
            if (current != null && current.getUniqueId().equals(shownId)) bubbles.remove(still);
        }, life);
    }

    @Override
    public void cleanUp(Player player) {
        bubbles.remove(player);
    }
}
