package de.hems.communication.events.result;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.event.EventResultData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes lines of an event's result on the launcher.
 * <p>
 * Fire and forget, like a run: a death in the arena must never wait for the network. Several lines travel
 * together because a death changes two of them - the one who fell and the one who got the kill.
 */
public class SaveEventResultsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4344L;

    private ArrayList<EventResultData> rows;
    /** Set when the game is over, so the launcher knows it may settle the event now. */
    private java.util.UUID finishedEvent;

    public SaveEventResultsEvent(List<EventResultData> rows) {
        this(rows, null);
    }

    /**
     * @param rows          the lines that changed, may be empty
     * @param finishedEvent the event whose game is over with this, or {@code null} while it still runs
     */
    public SaveEventResultsEvent(List<EventResultData> rows, java.util.UUID finishedEvent) {
        super(ListenerAdapter.ServerName.HOST);
        this.rows = new ArrayList<>(rows);
        this.finishedEvent = finishedEvent;
    }

    public SaveEventResultsEvent() {
    }

    /**
     * @return the event whose game is over, or {@code null}
     */
    public java.util.UUID getFinishedEvent() {
        return finishedEvent;
    }

    public List<EventResultData> getRows() {
        return rows == null ? List.of() : rows;
    }
}
