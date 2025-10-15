package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.Event;
import de.hems.communication.events.EventHandler;
import de.hems.communication.events.RequestDataFromConfigEvent;
import de.hems.communication.events.RespondDataFromConfigEvent;
import org.bukkit.configuration.file.YamlConfiguration;

public class RespondDataEvent implements EventHandler<RequestDataFromConfigEvent> {

    public RespondDataEvent() {
        ListenerAdapter.register(RequestDataFromConfigEvent.class, this);
    }

    @Override
    public void onEvent(Event event) {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        RequestDataFromConfigEvent e = (RequestDataFromConfigEvent) event;
        if (config.contains(e.getKey())) {
            RespondDataFromConfigEvent respondDataFromConfigEvent = new RespondDataFromConfigEvent(
                    Main.getInstance().getListenerAdapter().getName(),
                    e.getSender(),
                    config.get(e.getKey()),
                    e.getKey(),
                    e.getEventId()
            );
            ListenerAdapter.excecuteListeners(respondDataFromConfigEvent);
            return;
        }
        ListenerAdapter.excecuteListeners(new RespondDataFromConfigEvent(Main.getInstance().getListenerAdapter().getName(),
                e.getSender(),
                null,
                e.getKey(),
                e.getEventId()));
    }
}
