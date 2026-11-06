package com.benschnorr.communication.events.team;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.team.TeamData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Every team the launcher knows. */
public class RespondTeamsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4102L;

    public RespondTeamsEvent(ListenerAdapter.ServerName receiver, ArrayList<TeamData> teams, UUID requestId) {
        super(receiver, teams, requestId);
    }

    public RespondTeamsEvent() {
    }
}
