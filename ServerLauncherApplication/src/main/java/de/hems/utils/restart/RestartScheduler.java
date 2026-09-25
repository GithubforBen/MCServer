package de.hems.utils.restart;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.restart.RequestRestartStatusEvent;
import de.hems.communication.events.restart.RespondRestartEvent;
import de.hems.communication.events.restart.RestartUpdatedEvent;
import de.hems.communication.events.restart.ScheduleRestartEvent;
import de.hems.types.restart.RestartMode;
import de.hems.types.restart.RestartStatus;
import de.hems.utils.server.ServerInstance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Restarts the whole network at a set time - to bring an update in, or just to start clean.
 * <p>
 * Scheduled from any server with {@code /neustart}. Every server counts down on its own from the time it is
 * told. When the time comes:
 * <ol>
 *     <li>every server sends its players off with a message,</li>
 *     <li>every server is stopped, and the launcher <b>waits until each java process is really gone</b> - a
 *     stopping server closes its port long before it has finished saving its worlds, and starting the
 *     network again on top of that is how worlds get locked or half written,</li>
 *     <li>a server that has not finished after three minutes is ended the hard way,</li>
 *     <li>the launcher saves its configuration and exits with the code of the mode.</li>
 * </ol>
 * What comes next is up to {@code run.sh}: start again, pull and build first, or stay off. It writes how an
 * update went to {@code update-result.txt}, which is shown here after the next start.
 */
public class RestartScheduler {

    /** The longest a restart may be scheduled ahead, in minutes. */
    public static final int MAX_MINUTES = 24 * 60;
    /** What run.sh writes about the last update. */
    private static final File RESULT_FILE = new File("./update-result.txt");
    /** How long players get to read the message before the servers are stopped. */
    private static final long KICK_GRACE_MS = 5_000L;
    /** How long a server may take to save and stop before it is ended the hard way. */
    private static final long STOP_TIMEOUT_MS = 3 * 60_000L;

    private final Timer timer = new Timer("network-restart", true);
    private volatile RestartStatus status;
    private TimerTask pending;
    private final String lastUpdate;

    public RestartScheduler() {
        this.lastUpdate = readLastUpdate();
        this.status = new RestartStatus(0L, null, null, lastUpdate);
        if (lastUpdate != null) System.out.println("Last update: " + lastUpdate);
        ListenerAdapter.register(ScheduleRestartEvent.class, event -> onSchedule((ScheduleRestartEvent) event));
        ListenerAdapter.register(RequestRestartStatusEvent.class, event -> {
            RequestRestartStatusEvent request = (RequestRestartStatusEvent) event;
            ListenerAdapter.sendListeners(new RespondRestartEvent(request.getSender(), status, null,
                    request.getEventId()));
        });
    }

    private static String readLastUpdate() {
        try {
            return RESULT_FILE.isFile() ? Files.readString(RESULT_FILE.toPath(), StandardCharsets.UTF_8).trim() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private void onSchedule(ScheduleRestartEvent request) throws Exception {
        String error = request.getMode() == null
                ? cancel(request.getRequestedBy())
                : schedule(request.getMinutes(), request.getMode(), request.getRequestedBy());
        ListenerAdapter.sendListeners(new RespondRestartEvent(request.getSender(), status, error,
                request.getEventId()));
    }

    /**
     * Schedules a restart, replacing one that is already scheduled.
     *
     * @param minutes     how long until it happens, zero for right away
     * @param mode        what happens
     * @param requestedBy who asked
     * @return why it was refused, or {@code null}
     */
    public synchronized String schedule(int minutes, RestartMode mode, String requestedBy) {
        if (minutes < 0 || minutes > MAX_MINUTES) {
            return "Zwischen 0 und " + MAX_MINUTES + " Minuten.";
        }
        if (pending != null) pending.cancel();
        long at = System.currentTimeMillis() + minutes * 60_000L;
        status = new RestartStatus(at, mode, requestedBy, lastUpdate);
        pending = new TimerTask() {
            @Override
            public void run() {
                execute();
            }
        };
        timer.schedule(pending, Math.max(0L, at - System.currentTimeMillis()));
        System.out.println(mode.getTitle() + " of the network scheduled by " + requestedBy + " in " + minutes
                + " minutes.");
        announce(false);
        return null;
    }

    /**
     * Calls a scheduled restart off.
     *
     * @param requestedBy who asked
     * @return why it was refused, or {@code null}
     */
    public synchronized String cancel(String requestedBy) {
        if (!status.isScheduled()) return "Es ist kein Neustart geplant.";
        if (pending != null) pending.cancel();
        pending = null;
        System.out.println("The scheduled " + status.getMode().getTitle() + " was called off by " + requestedBy + ".");
        status = new RestartStatus(0L, null, null, lastUpdate);
        announce(false);
        return null;
    }

    public RestartStatus getStatus() {
        return status;
    }

    private void announce(boolean now) {
        try {
            ListenerAdapter.sendListeners(new RestartUpdatedEvent(status, now));
        } catch (Exception e) {
            System.out.println("Could not announce the restart: " + e.getMessage());
        }
    }

    /**
     * The restart itself: players off, servers stopped and waited for, configuration saved, exit.
     */
    private void execute() {
        RestartMode mode;
        synchronized (this) {
            mode = status.getMode();
            if (mode == null) return;
        }
        System.out.println(mode.getTitle() + " of the network - sending everybody off.");
        announce(true);
        sleep(KICK_GRACE_MS);

        // the list is taken before anything stops: the handler forgets a server the moment its port closes,
        // which is long before it has finished saving
        List<ServerInstance> running = new ArrayList<>(Main.getInstance().getServerHandler().getInstances());
        for (ServerInstance instance : running) {
            try {
                instance.stop();
            } catch (IOException e) {
                System.out.println("Could not stop " + instance.getName() + ": " + e.getMessage());
            }
        }
        waitForAll(running);

        try {
            Main.getInstance().getConfiguration().save();
        } catch (RuntimeException e) {
            System.out.println("Could not save the configuration: " + e.getMessage());
        }
        System.out.println("Every server is down - exiting with " + mode.getExitCode() + " (" + mode.getTitle()
                + "). run.sh takes it from here.");
        System.exit(mode.getExitCode());
    }

    /**
     * Waits until every server's process is gone, ending the ones that take too long.
     *
     * @param servers the servers that were stopped
     */
    private static void waitForAll(List<ServerInstance> servers) {
        long deadline = System.currentTimeMillis() + STOP_TIMEOUT_MS;
        List<ServerInstance> left = new ArrayList<>(servers);
        while (!left.isEmpty() && System.currentTimeMillis() < deadline) {
            left.removeIf(instance -> !instance.isProcessRunning());
            if (!left.isEmpty()) sleep(1000L);
        }
        for (ServerInstance instance : left) {
            System.out.println(instance.getName() + " did not stop within three minutes - ending it.");
            instance.kill();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
