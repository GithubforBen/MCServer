package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;

import com.benschnorr.types.item.ItemCatalog;

import java.io.Serializable;
import java.util.UUID;

/**
 * What items can be made of on one server: its materials, enchantments and attributes, as an
 * {@link ItemCatalog}.
 */
public class RespondMaterialsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 3112L;

    public RespondMaterialsEvent(ListenerAdapter.ServerName receiver, ItemCatalog catalog, UUID requestId) {
        super(receiver, catalog, requestId);
    }

    public RespondMaterialsEvent() {
    }
}
