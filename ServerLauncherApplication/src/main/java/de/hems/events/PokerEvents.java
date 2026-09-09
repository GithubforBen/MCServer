package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.poker.PokerStatsUpdatedEvent;
import de.hems.communication.events.poker.RequestPokerStatsEvent;
import de.hems.communication.events.poker.RespondPokerStatsEvent;
import de.hems.communication.events.poker.SavePokerStatsEvent;
import de.hems.types.poker.PokerStatsData;
import de.hems.utils.poker.PokerStatsStore;

import java.util.ArrayList;

/**
 * Serves the record of the poker nights.
 * <p>
 * The same shape as {@link EventEvents}: the launcher is the only node that writes, and after every write
 * the new state goes out to everybody, so the ranking board in the lobby moves with the table.
 */
public class PokerEvents {

    private final PokerStatsStore stats;

    public PokerEvents(PokerStatsStore stats) {
        this.stats = stats;
        ListenerAdapter.register(RequestPokerStatsEvent.class,
                event -> onRequest((RequestPokerStatsEvent) event));
        ListenerAdapter.register(SavePokerStatsEvent.class,
                event -> onSave((SavePokerStatsEvent) event));
    }

    private void onRequest(RequestPokerStatsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondPokerStatsEvent(
                request.getSender(), new ArrayList<>(stats.getRows()), request.getEventId()));
    }

    private void onSave(SavePokerStatsEvent request) {
        PokerStatsData stored = stats.put(request.getStats());
        // a refused write is a message from a server that has fallen behind. Announcing it would push the
        // stale numbers out to everybody, which is the one thing the refusal was there to prevent
        if (stored == null) return;
        announce(stored);
    }

    /**
     * Tells the network about a row, so every server and the website follow along.
     *
     * @param row the row that changed
     */
    public void announce(PokerStatsData row) {
        if (row == null) return;
        try {
            ListenerAdapter.sendListeners(new PokerStatsUpdatedEvent(
                    row.getEventId(), row.getPlayerId(), row));
        } catch (Exception e) {
            System.out.println("Could not announce the poker row of " + row.getPlayerName()
                    + ": " + e.getMessage());
        }
    }
}
