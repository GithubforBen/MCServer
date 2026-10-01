import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.discord.ConfirmAccountLinkEvent;
import java.time.Duration;
import java.util.UUID;

public class ClusterProbe {
    public static void main(String[] a) throws Exception {
        new ListenerAdapter(ListenerAdapter.ServerName.of("PROBE", -1));
        Thread.sleep(3000);
        long t = System.currentTimeMillis();
        var servers = de.hems.api.ServerApi.listServers();
        System.out.println("listServers: " + servers.length + " in " + (System.currentTimeMillis() - t) + " ms");
        for (var s : servers) System.out.println("  " + s.name + " " + s.port);
        t = System.currentTimeMillis();
        var r = ListenerAdapter.ask(new ConfirmAccountLinkEvent(UUID.randomUUID(), "x", "ABC123"), Duration.ofSeconds(5));
        System.out.println("verify: " + (r == null ? "KEINE ANTWORT" : r.getClass().getSimpleName()) + " in " + (System.currentTimeMillis() - t) + " ms");
        System.exit(0);
    }
}
