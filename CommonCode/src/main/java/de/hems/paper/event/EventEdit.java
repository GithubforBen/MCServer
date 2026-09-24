package de.hems.paper.event;

import de.hems.types.event.EventData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * What a settings or rewards panel is editing, and where a change goes.
 * <p>
 * The same panels serve two situations. While an event is being created it is a draft: a change is made on
 * the draft and nothing reaches the network until "Anlegen" is pressed. Once it exists, every change is
 * saved right away, so closing the panel with escape loses nothing. The panels do not have to know which of
 * the two they are in - they describe the change, and this decides what happens to it.
 */
public abstract class EventEdit {

    private final Consumer<Player> back;

    private EventEdit(Consumer<Player> back) {
        this.back = back;
    }

    /**
     * @param draft the event being created, changed in place
     * @param back  what to open when the panel is left
     * @return an edit that keeps everything on the draft
     */
    public static EventEdit draft(EventData draft, Consumer<Player> back) {
        return new EventEdit(back) {
            @Override
            public EventData current() {
                return draft;
            }

            @Override
            public void change(Player player, Consumer<EventData> change, Runnable reopen) {
                change.accept(draft);
                reopen.run();
            }

            @Override
            public boolean isDraft() {
                return true;
            }
        };
    }

    /**
     * @param event the event as it is stored
     * @param back  what to open when the panel is left
     * @return an edit that saves every change straight away
     */
    public static EventEdit live(EventData event, Consumer<Player> back) {
        return new EventEdit(back) {
            private EventData current = event;

            @Override
            public EventData current() {
                return current;
            }

            @Override
            public void change(Player player, Consumer<EventData> change, Runnable reopen) {
                EventData edited = current.copy();
                change.accept(edited);
                EventService.saveAsync(edited, false, result -> {
                    if (!result.successful()) {
                        player.sendMessage(ChatColor.RED + "❌ " + result.message());
                        return;
                    }
                    current = result.event();
                    reopen.run();
                });
            }

            @Override
            public boolean isDraft() {
                return false;
            }
        };
    }

    /**
     * @return the event as it stands now
     */
    public abstract EventData current();

    /**
     * Makes a change and opens the panel again once it has gone through.
     *
     * @param player who made it, told if it fails
     * @param change what to change
     * @param reopen what to open afterwards
     */
    public abstract void change(Player player, Consumer<EventData> change, Runnable reopen);

    /**
     * @return whether the event only exists as a draft so far
     */
    public abstract boolean isDraft();

    /**
     * Leaves the panel.
     *
     * @param player who is leaving
     */
    public void back(Player player) {
        back.accept(player);
    }
}
