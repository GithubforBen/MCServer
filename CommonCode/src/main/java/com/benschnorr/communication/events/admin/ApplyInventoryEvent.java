package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.admin.InventoryData;

import java.io.Serializable;

/**
 * Writes a container back after it was edited in the browser.
 */
public class ApplyInventoryEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 3105L;

    private InventoryData inventory;
    /** Who changed it, so the change can be written into the log. */
    private String editor;

    public ApplyInventoryEvent(InventoryData inventory, String editor) {
        super(ListenerAdapter.ServerName.ALL);
        this.inventory = inventory;
        this.editor = editor;
    }

    public ApplyInventoryEvent() {
    }

    public InventoryData getInventory() {
        return inventory;
    }

    public String getEditor() {
        return editor;
    }
}
