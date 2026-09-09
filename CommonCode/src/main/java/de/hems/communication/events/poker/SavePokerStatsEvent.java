package de.hems.communication.events.poker;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.poker.PokerStatsData;

import java.io.Serializable;

/**
 * Writes one player's row of one poker night on the launcher.
 * <p>
 * The row is sent whole rather than as a difference, which is only safe because exactly one server ever
 * writes a given row: a poker night is played on one casino server, and that server owns every row of it
 * for as long as it lives. Two writers would need differences and a lock, the way the money does.
 * <p>
 * And it is not money. Bits move through {@link de.hems.paper.money.MoneyService} and nowhere else - this
 * is the record of what happened, kept so the ranking survives the casino server being switched off, and
 * so a night that ends in a crash can still hand back what was on the table.
 */
public class SavePokerStatsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4332L;

    private PokerStatsData stats;

    public SavePokerStatsEvent(PokerStatsData stats) {
        super(ListenerAdapter.ServerName.HOST);
        this.stats = stats;
    }

    public SavePokerStatsEvent() {
    }

    public PokerStatsData getStats() {
        return stats;
    }
}
