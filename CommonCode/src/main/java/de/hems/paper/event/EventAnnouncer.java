package de.hems.paper.event;

import de.hems.types.event.EventData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * What a player is told about the events: a line when they join and a line in the tab list.
 * <p>
 * Both read the local copy in {@link EventService}, so neither touches the network.
 */
public final class EventAnnouncer {

    private EventAnnouncer() {
    }

    /**
     * Tells a player what is going on right now and what is coming.
     *
     * @param player who just joined
     */
    public static void sendJoinMessage(Player player) {
        List<EventData> running = EventService.getRunning();
        EventData next = EventService.getNext();
        if (running.isEmpty() && next == null) return;

        player.sendMessage(Component.empty());
        for (EventData event : running) {
            player.sendMessage(Component.text("● ", NamedTextColor.GREEN)
                    .append(Component.text(event.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" läuft gerade - noch "
                            + EventData.format(event.getTimeUntilEnd()), NamedTextColor.GRAY)));
        }
        if (next != null) {
            player.sendMessage(Component.text("● ", NamedTextColor.YELLOW)
                    .append(Component.text(next.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" startet in "
                            + EventData.format(next.getTimeUntilStart()), NamedTextColor.GRAY)));
        }
        player.sendMessage(Component.text("[Events ansehen]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/events"))
                .hoverEvent(HoverEvent.showText(Component.text("Öffnet den Eventkalender")))); 
        player.sendMessage(Component.empty());
    }

    /**
     * The line for the tab list.
     *
     * @return what is running or coming next, or {@code null} when there is nothing to say
     */
    public static Component tabLine() {
        List<EventData> running = EventService.getRunning();
        if (!running.isEmpty()) {
            EventData event = running.getFirst();
            return Component.text(event.getName() + " läuft - noch "
                    + EventData.format(event.getTimeUntilEnd()), NamedTextColor.GREEN);
        }
        EventData next = EventService.getNext();
        if (next == null) return null;
        return Component.text("Nächstes Event: " + next.getName() + " in "
                + EventData.format(next.getTimeUntilStart()), NamedTextColor.YELLOW);
    }
}
