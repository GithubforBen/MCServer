package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * The materials one server knows, as plain names.
 */
public class RespondMaterialsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 3111L;

    public RespondMaterialsEvent(ListenerAdapter.ServerName receiver, ArrayList<String> materials,
                                 UUID requestId) {
        super(receiver, materials, requestId);
    }

    public RespondMaterialsEvent() {
    }
}
