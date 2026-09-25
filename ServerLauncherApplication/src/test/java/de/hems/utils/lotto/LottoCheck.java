package de.hems.utils.lotto;

import de.hems.types.lotto.LottoDraw;
import de.hems.types.lotto.LottoTicket;
import de.hems.utils.money.MoneyStore;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Checks buying, drawing, sharing the pot and the draw date.
 * <p>
 * Like {@code RewardCheck}, a class with a main - see there for how to run it. Exits non-zero when
 * something is wrong. Without a network, "Could not announce" lines are expected.
 */
public final class LottoCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        rules();
        buyAndDraw();
        rollover();
        schedule();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static File temp(String name) throws Exception {
        File file = File.createTempFile(name, ".yml");
        file.delete();
        return file;
    }

    private static void rules() {
        check("a tip is sorted", LottoTicket.format(LottoTicket.normalize(new int[]{9, 2, 15, 4})), "2 · 4 · 9 · 15");
        check("a number twice is no tip", LottoTicket.normalize(new int[]{1, 1, 2, 3}), null);
        check("16 is too high", LottoTicket.normalize(new int[]{1, 2, 3, 16}), null);
        check("0 is too low", LottoTicket.normalize(new int[]{0, 2, 3, 4}), null);
        check("three numbers are not enough", LottoTicket.normalize(new int[]{1, 2, 3}), null);
    }

    private static void buyAndDraw() throws Exception {
        MoneyStore money = new MoneyStore(temp("money"), null);
        LottoService lotto = new LottoService(new LottoStore(temp("lotto")), money);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        money.change(a.toString(), 1000, false);
        money.change(b.toString(), 150, false);

        check("buying without enough bits fails",
                lotto.buy(b, "B", List.of(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7, 8})).error() != null, true);
        check("and costs nothing", money.get(b.toString()), 150);
        check("a broken tip fails the whole purchase",
                lotto.buy(a, "A", List.of(new int[]{1, 2, 3, 4}, new int[]{1, 1, 2, 3})).error() != null, true);
        check("and costs nothing either", money.get(a.toString()), 1000);

        check("A buys two", lotto.buy(a, "A", List.of(new int[]{1, 2, 3, 4}, new int[]{4, 3, 2, 1})).error(), null);
        check("B buys one", lotto.buy(b, "B", List.of(new int[]{1, 2, 3, 4})).error(), null);
        check("A paid 200", money.get(a.toString()), 800);
        check("everything goes into the pot", lotto.status().getPot(), 300);
        check("A has two tips", lotto.ticketsOf(a).size(), 2);

        LottoDraw draw = lotto.draw(new int[]{4, 1, 3, 2}).value();
        check("three winning tips", draw.getWinners().size(), 3);
        check("the pot is shared per tip", draw.getPayoutEach(), 100);
        check("A gets two shares", money.get(a.toString()), 1000);
        check("B gets one", money.get(b.toString()), 150);
        check("A is counted twice", draw.winsOf(a), 2);
        check("the pot is empty", lotto.status().getPot(), 0);
        check("the next round has started", lotto.status().getRound(), 2);
        check("with no tips", lotto.tickets().size(), 0);
        check("a round without tips cannot be drawn", lotto.draw(null).error() != null, true);
    }

    private static void rollover() throws Exception {
        MoneyStore money = new MoneyStore(temp("money"), null);
        File file = temp("lotto");
        LottoService lotto = new LottoService(new LottoStore(file), money);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        money.change(a.toString(), 1000, false);
        money.change(b.toString(), 1000, false);
        lotto.buy(a, "A", List.of(new int[]{1, 2, 3, 4}));
        LottoDraw miss = lotto.draw(new int[]{5, 6, 7, 8}).value();
        check("nobody wins", miss.getWinners().size(), 0);
        check("the pot stays", lotto.status().getPot(), 100);

        lotto.buy(a, "A", List.of(new int[]{5, 6, 7, 8}));
        lotto.buy(b, "B", List.of(new int[]{5, 6, 7, 8}, new int[]{9, 10, 11, 12}));
        check("the pot survives a restart", new LottoService(new LottoStore(file), money).status().getPot(), 400);
        check("and the tips", new LottoService(new LottoStore(file), money).tickets().size(), 3);

        lotto.setPrice(1);
        lotto.buy(a, "A", List.of(new int[]{1, 5, 9, 13}));
        check("a new price counts from then on", lotto.status().getPot(), 401);
        LottoDraw hit = lotto.draw(new int[]{5, 6, 7, 8}).value();
        check("two winners of the grown pot", hit.getPayoutEach(), 200);
        check("what does not divide stays", lotto.status().getPot(), 1);
        check("the history survives a restart",
                new LottoService(new LottoStore(file), money).history().size(), 2);
        check("with the winners", new LottoService(new LottoStore(file), money).history().getFirst().getWinners().size(), 2);
    }

    private static void schedule() throws Exception {
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        // Wednesday 2026-09-23, 12:00 in Berlin
        Instant wednesday = ZonedDateTime.of(2026, 9, 23, 12, 0, 0, 0, berlin).toInstant();
        LottoService lotto = new LottoService(new LottoStore(temp("lotto")), new MoneyStore(temp("money"), null),
                Clock.fixed(wednesday, berlin));
        ZonedDateTime next = Instant.ofEpochMilli(lotto.status().getNextDrawAt()).atZone(berlin);
        check("the first draw is next sunday", next.toLocalDate().toString(), "2026-09-27");
        check("at eight", next.toLocalTime().toString(), "20:00");
        check("a broken date is refused", lotto.setSchedule("irgendwann") != null, true);
        check("a new date is taken", lotto.setSchedule("mittwoch 18:30"), null);
        next = Instant.ofEpochMilli(lotto.status().getNextDrawAt()).atZone(berlin);
        check("today, since it has not been yet", next.toLocalDate().toString(), "2026-09-23");
        lotto.setSchedule("mittwoch 11:00");
        next = Instant.ofEpochMilli(lotto.status().getNextDrawAt()).atZone(berlin);
        check("next week, since today's is over", next.toLocalDate().toString(), "2026-09-30");
        check("it reads back the way it is shown", lotto.status().getSchedule(), "MITTWOCH 11:00");

        // due, but nobody tipped: the date moves on without a draw
        Instant late = ZonedDateTime.of(2026, 9, 30, 11, 5, 0, 0, berlin).toInstant();
        File file = temp("lotto");
        LottoService due = new LottoService(new LottoStore(file), new MoneyStore(temp("money"), null),
                Clock.fixed(wednesday, berlin));
        due.setSchedule("mittwoch 11:00");
        LottoService later = new LottoService(new LottoStore(file), new MoneyStore(temp("money"), null),
                Clock.fixed(late, berlin));
        later.drawIfDue();
        check("an empty round is not drawn", later.history().size(), 0);
        check("but moves on a week", Instant.ofEpochMilli(later.status().getNextDrawAt()).atZone(berlin)
                .toLocalDate().toString(), "2026-10-07");
    }

    private static void check(String what, Object actual, Object expected) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
            return;
        }
        failed++;
        System.out.println("FAIL " + what + ": expected " + expected + ", got " + actual);
    }
}
