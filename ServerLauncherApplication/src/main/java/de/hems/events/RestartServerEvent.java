package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerRestartEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import de.hems.utils.server.ServerInstance;

public class RestartServerEvent implements EventHandler<RequestServerRestartEvent> {
    public RestartServerEvent() {
        ListenerAdapter.register(RequestServerRestartEvent.class, this);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (!(event instanceof RequestServerRestartEvent)) {
            return;
        }
        System.out.println(1);
        ServerInstance stop = Main.getInstance().getServerHandler().stop(((RequestServerRestartEvent) event).getServerName());
        System.out.println(2);
        new Thread(() -> {
            System.out.println("Thread");
            while (!Main.getInstance().getServerHandler().doesInstanceExist(((RequestServerRestartEvent) event).getServerName())) {
                System.out.println("Waiting for server to stop...");
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                Main.getInstance().getServerHandler().startNewInstance(stop.getName(), stop.getAllocatedMemoryMB(), stop.getJarFile(), stop.getPlugins());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
