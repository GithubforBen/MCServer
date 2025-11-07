package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import de.hems.communication.events.configs.RequestDataFromConfigEvent;
import de.hems.communication.events.configs.RespondDataFromConfigEvent;
import org.bukkit.configuration.file.YamlConfiguration;

public class RespondDataEvent implements EventHandler<RequestDataFromConfigEvent> {

    public RespondDataEvent() {
        ListenerAdapter.register(RequestDataFromConfigEvent.class, this);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        RequestDataFromConfigEvent e = (RequestDataFromConfigEvent) event;
        if (config.contains(e.getKey())) {
            RespondDataFromConfigEvent respondDataFromConfigEvent = new RespondDataFromConfigEvent(
                    e.getSender(),
                    config.get(e.getKey()),
                    e.getKey(),
                    e.getEventId()
            );
            ListenerAdapter.sendListeners(respondDataFromConfigEvent);
            return;
        }
        ListenerAdapter.sendListeners(new RespondDataFromConfigEvent(
                e.getSender(),
                null,
                e.getKey(),
                e.getEventId()));
    }
}
