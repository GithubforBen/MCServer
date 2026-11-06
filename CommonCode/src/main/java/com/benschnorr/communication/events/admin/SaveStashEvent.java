package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.admin.StashData;

import java.io.Serializable;

/**
 * Writes the admin stash back.
 * <p>
 * Carries the revision it was read at, so a browser and a player standing in front of the chest cannot
 * quietly overwrite one another.
 */
public class SaveStashEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 5004L;

    private StashData stash;
    /** Who changed it, for the log. */
    private String editor;

    public SaveStashEvent(StashData stash, String editor) {
        super(ListenerAdapter.ServerName.HOST);
        this.stash = stash;
        this.editor = editor;
    }

    public SaveStashEvent() {
    }

    public StashData getStash() {
        return stash;
    }

    public String getEditor() {
        return editor;
    }
}
