package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerRestartEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import de.hems.utils.server.ServerInstance;

/**
 * Stops a server and starts it again with the settings it was running with.
 */
public class RestartServerEvent implements EventHandler<RequestServerRestartEvent> {

    /** How long the restart waits for the old process to release its port. */
    private static final long SHUTDOWN_TIMEOUT_MS = 120_000L;

    public RestartServerEvent() {
        ListenerAdapter.register(RequestServerRestartEvent.class, this);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (!(event instanceof RequestServerRestartEvent request)) {
            return;
        }
        ServerInstance stopped = Main.getInstance().getServerHandler().stop(request.getServerName());
        new Thread(() -> {
            long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MS;
            // wait until the old server is really gone, otherwise the port is still taken
            while (Main.getInstance().getServerHandler().doesInstanceExist(request.getServerName())) {
                if (System.currentTimeMillis() > deadline) {
                    System.out.println(request.getServerName() + " did not stop in time - not restarting it.");
                    return;
                }
                System.out.println("Waiting for " + request.getServerName() + " to stop...");
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            try {
                Main.getInstance().getServerHandler().startNewInstance(stopped.getName(), stopped.getAllocatedMemoryMB(),
                        stopped.getJarFile(), stopped.getPlugins(), stopped.getTemplate());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
