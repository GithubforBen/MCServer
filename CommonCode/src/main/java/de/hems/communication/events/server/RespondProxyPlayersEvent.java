package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Who the proxy currently has on which server, the answer to a {@link RequestProxyPlayersEvent}.
 */
public class RespondProxyPlayersEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 1941L;

    public RespondProxyPlayersEvent(ListenerAdapter.ServerName receiver,
                                    HashMap<String, ArrayList<String>> playersPerServer, UUID requestId) {
        super(receiver, playersPerServer, requestId);
    }

    public RespondProxyPlayersEvent() {
    }

    /**
     * @return the player names per server, keyed by the upper case server name
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> getPlayersPerServer() {
        Map<String, List<String>> found = new HashMap<>();
        if (!(getData() instanceof Map<?, ?> raw)) return found;
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String server) || !(entry.getValue() instanceof List<?> names)) continue;
            found.put(server.toUpperCase(Locale.ROOT), (List<String>) names);
        }
        return found;
    }
}
