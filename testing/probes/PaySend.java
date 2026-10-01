import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.configs.PayingPlayersChangedEvent;
import java.util.List;
public class PaySend {
    public static void main(String[] a) throws Exception {
        new ListenerAdapter(ListenerAdapter.ServerName.of("PAYSEND", -1));
        Thread.sleep(3000);
        ListenerAdapter.sendListeners(new PayingPlayersChangedEvent(List.of(a[0])));
        System.out.println("gesendet");
        Thread.sleep(2000);
        System.exit(0);
    }
}
