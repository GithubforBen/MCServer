package com.benschnorr.communication.events.team;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.team.BackpackData;

import java.io.Serializable;

/**
 * Writes a team backpack back to the launcher.
 * <p>
 * Carries the revision the backpack was opened at. If somebody on another server saved in between, the
 * launcher refuses the write rather than letting one set of changes disappear without anybody noticing.
 */
public class SaveBackpackEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4109L;

    private BackpackData backpack;

    public SaveBackpackEvent(BackpackData backpack) {
        super(ListenerAdapter.ServerName.HOST);
        this.backpack = backpack;
    }

    public SaveBackpackEvent() {
    }

    public BackpackData getBackpack() {
        return backpack;
    }
}
