package com.benschnorr.events;

import com.benschnorr.Main;
import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventHandler;
import com.benschnorr.communication.events.configs.RequestDataFromConfigEvent;
import com.benschnorr.communication.events.configs.RespondDataFromConfigEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Properties;

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
