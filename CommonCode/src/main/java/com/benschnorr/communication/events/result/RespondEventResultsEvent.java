package com.benschnorr.communication.events.result;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.event.EventResultData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** The result lines of one event. */
public class RespondEventResultsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4346L;

    public RespondEventResultsEvent(ListenerAdapter.ServerName receiver, ArrayList<EventResultData> rows,
                                    UUID requestId) {
        super(receiver, rows, requestId);
    }

    public RespondEventResultsEvent() {
    }
}
