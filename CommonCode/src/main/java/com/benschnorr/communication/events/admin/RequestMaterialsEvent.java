package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Asks a server which materials, enchantments and attributes exist.
 * <p>
 * The launcher has the paper api on its classpath but never boots a server, so bukkit's material registry
 * is not initialised there and {@code Material.values()} cannot be asked anything useful. The servers do
 * have a live registry, so the item editor gets its lists from them.
 */
public class RequestMaterialsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 3110L;

    public RequestMaterialsEvent() {
        super(ListenerAdapter.ServerName.ALL);
    }
}
