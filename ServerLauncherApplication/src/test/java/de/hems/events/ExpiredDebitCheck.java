package de.hems.events;

import de.hems.communication.events.money.ChangeBalanceEvent;
import de.hems.types.money.BalanceResult;
import de.hems.utils.money.MoneyStore;

import java.io.File;

/**
 * Checks that the launcher drops a debit whose deadline has passed and moves no bits.
 * <p>
 * Like {@code RewardCheck}, a class with a main. Exits non-zero when something is wrong.
 */
// spec: shop-buyback#host-answers-too-late
public final class ExpiredDebitCheck {

    private static int failed;

    public static void main(String[] args) throws Exception {
        File file = File.createTempFile("money", ".yml");
        file.delete();
        MoneyStore money = new MoneyStore(file, null);
        money.change("Alpha", 500, false);

        ChangeBalanceEvent open = new ChangeBalanceEvent("Alpha", -100, true, "t");
        ChangeBalanceEvent timed = new ChangeBalanceEvent("Alpha", -100, true, "t", 1000L);
        check("no deadline never expires", open.isExpired(Long.MAX_VALUE), false);
        check("before the deadline", timed.isExpired(999L), false);
        check("exactly at the deadline", timed.isExpired(1000L), false);
        check("after the deadline", timed.isExpired(1001L), true);

        check("in time is not refused", MoneyEvents.refuseExpired(money, timed, 500L) == null, true);
        BalanceResult late = MoneyEvents.refuseExpired(money, timed, 2000L);
        check("too late is refused", late != null && !late.isSuccessful(), true);
        check("and carries the balance", late == null ? -1 : late.getBalance(), 500);
        check("no bits moved", money.get("Alpha"), 500);

        if (failed > 0) {
            System.out.println(failed + " failed");
            System.exit(1);
        }
        System.out.println("ok");
    }

    private static void check(String what, Object actual, Object expected) {
        boolean ok = expected.equals(actual);
        if (!ok) failed++;
        System.out.println((ok ? "ok   " : "FAIL ") + what + (ok ? "" : " (got " + actual + ")"));
    }
}
