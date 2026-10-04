import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.RequestRunsEvent;
import de.hems.communication.events.event.SaveRunEvent;
import de.hems.types.ServerTemplate;
import de.hems.types.event.RunData;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Drives the runs of a speedrun event without a client: what the launcher does with a run that is over,
 * and what a run server does when its run is called off somewhere else.
 * <pre>
 * closed &lt;event&gt;         a failed run must not be opened again by a late save
 * start &lt;event&gt;          saves a run and starts its server, prints the run and the server
 * state &lt;run&gt;            how the launcher holds a run
 * abort &lt;run&gt;            calls a run off, the way the event panel does
 * servers                  which servers are up
 * </pre>
 */
public class RunProbe {

    public static void main(String[] args) throws Exception {
        new ListenerAdapter(ListenerAdapter.ServerName.of("PROBE", -1));
        Thread.sleep(3000);
        switch (args[0]) {
            case "closed" -> {
                RunData run = new RunData(UUID.fromString(args[1]), Set.of(UUID.randomUUID()));
                run.finish(RunData.State.FAILED);
                ListenerAdapter.sendListeners(new SaveRunEvent(run));
                Thread.sleep(1000);
                // the clock of a run server that has not heard of the end yet
                run.setState(RunData.State.RUNNING);
                run.setFinishedAt(0L);
                ListenerAdapter.sendListeners(new SaveRunEvent(run));
                Thread.sleep(1000);
                System.out.println(run.getId() + " is " + find(run.getId()).getState() + " (expected FAILED)");
            }
            case "start" -> {
                RunData run = new RunData(UUID.fromString(args[1]), Set.of(UUID.randomUUID()));
                String short1 = args[1].substring(0, 8), short2 = run.getId().toString().substring(0, 8);
                run.setServerName(ServerApi.freeName("RUN_" + short1 + "_" + short2));
                ListenerAdapter.sendListeners(new SaveRunEvent(run));
                ServerApi.createServer(run.getServerName(), ServerTemplate.EVENT);
                System.out.println(run.getId() + " " + run.getServerName());
            }
            case "state" -> {
                RunData run = find(UUID.fromString(args[1]));
                System.out.println(run == null ? "gone" : run.getState() + " on " + run.getServerName()
                        + ", " + run.getElapsedTicksRaw() + " ticks");
            }
            case "abort" -> {
                RunData run = find(UUID.fromString(args[1]));
                run.finish(RunData.State.ABANDONED);
                ListenerAdapter.sendListeners(new SaveRunEvent(run));
                Thread.sleep(1000);
                System.out.println(run.getId() + " is " + find(run.getId()).getState());
            }
            case "servers" -> {
                for (var server : ServerApi.listServers()) {
                    System.out.println(server.name + " " + server.phase + (server.online ? " online" : ""));
                }
            }
            default -> System.out.println("closed <event> | start <event> | state <run> | abort <run> | servers");
        }
        System.exit(0);
    }

    @SuppressWarnings("unchecked")
    private static RunData find(UUID id) {
        var answer = ListenerAdapter.ask(new RequestRunsEvent(), Duration.ofSeconds(5));
        if (answer == null) throw new IllegalStateException("the launcher does not answer");
        for (RunData run : (List<RunData>) answer.getData()) {
            if (id.equals(run.getId())) return run;
        }
        return null;
    }
}
