import com.benschnorr.communication.ListenerAdapter;
import java.util.UUID;
public class PayRecv {
    public static void main(String[] a) throws Exception {
        new ListenerAdapter(ListenerAdapter.ServerName.of("PAYRECV", -1));
        Class.forName("com.benschnorr.paper.PayingPlayers");
        UUID u = UUID.fromString(a[0]);
        long start = System.currentTimeMillis();
        boolean before = com.benschnorr.paper.PayingPlayers.isPaying(u);
        System.out.println("vorher: " + before);
        while (System.currentTimeMillis() - start < 20000) {
            if (com.benschnorr.paper.PayingPlayers.isPaying(u) != before) {
                System.out.println("nachher: " + com.benschnorr.paper.PayingPlayers.isPaying(u) + " nach " + (System.currentTimeMillis() - start) + " ms");
                System.exit(0);
            }
            Thread.sleep(50);
        }
        System.out.println("keine Aenderung in 20 s");
        System.exit(0);
    }
}
