package de.hems.paper.util;

import de.hems.paper.PaperContext;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks a player for a piece of text in chat, e.g. the name of a new server, and hands the answer back on
 * the main thread.
 */
public final class ChatPrompt implements Listener {

    private static final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private ChatPrompt() {
    }

    /**
     * Asks the player for text. The next chat message they send is consumed as the answer instead of being
     * broadcast.
     *
     * @param player   the player to ask
     * @param question what the player should type, shown in chat
     * @param answer   run on the main thread with the typed text
     */
    public static void ask(Player player, String question, Consumer<String> answer) {
        register();
        pending.put(player.getUniqueId(), answer);
        player.closeInventory();
        player.sendMessage(ChatColor.AQUA + question);
        player.sendMessage(ChatColor.GRAY + "Schreibe 'abbrechen' um abzubrechen.");
    }

    /**
     * @param player the player to check
     * @return whether that player is currently being asked something
     */
    public static boolean isWaiting(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public static void cancel(Player player) {
        pending.remove(player.getUniqueId());
    }

    private static synchronized void register() {
        if (registered || !PaperContext.hasPlugin()) return;
        Bukkit.getPluginManager().registerEvents(new ChatPrompt(), PaperContext.getPlugin());
        registered = true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Consumer<String> consumer = pending.remove(event.getPlayer().getUniqueId());
        if (consumer == null) return;
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player player = event.getPlayer();
        if (message.equalsIgnoreCase("abbrechen") || message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.GRAY + "Abgebrochen.");
            return;
        }
        PaperContext.sync(() -> consumer.accept(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
