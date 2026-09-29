package de.hems.utils.money;

import java.io.File;
import java.util.UUID;

/**
 * Checks that a team's money follows a rename and goes to the leader when the team is disbanded.
 * <p>
 * Like {@code RewardCheck}, a class with a main - see there for how to run it. Exits non-zero when
 * something is wrong.
 */
public final class MoneyMoveCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File file = File.createTempFile("money", ".yml");
        file.delete();
        MoneyStore money = new MoneyStore(file, null);
        String leader = UUID.randomUUID().toString();
        money.change("Alpha", 500, false);
        money.change(leader, 20, false);

        MoneyStore.Transfer rename = money.moveAll("Alpha", "Beta");
        check("a rename moves everything", rename.amount(), 500);
        check("the new name holds it", money.get("Beta"), 500);
        check("the old name is empty", money.get("Alpha"), 0);
        check("and gone from the file",
                new MoneyStore(file, null).all().containsKey("Alpha"), false);

        MoneyStore.Transfer disband = money.moveAll("Beta", leader);
        check("disbanding hands it to the leader", disband.toNow(), 520);
        check("the leader has it", money.get(leader), 520);
        check("a new team of the old name starts at nothing", money.get("Beta"), 0);
        check("which survives a restart", new MoneyStore(file, null).get(leader), 520);

        MoneyStore.Transfer empty = money.moveAll("Nobody", leader);
        check("an empty account moves nothing", empty.amount(), 0);
        check("and leaves the receiver alone", money.get(leader), 520);

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String what, Object actual, Object expected) {
        if (expected.equals(actual)) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + what + " - expected " + expected + ", got " + actual);
        }
    }
}
