package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import de.hems.types.item.ItemCatalog;

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
