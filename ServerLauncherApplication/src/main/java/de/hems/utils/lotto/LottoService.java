package de.hems.utils.lotto;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.lotto.LottoUpdatedEvent;
import de.hems.communication.events.money.BalanceUpdatedEvent;
import de.hems.types.lotto.LottoDraw;
import de.hems.types.lotto.LottoStatus;
import de.hems.types.lotto.LottoTicket;
import de.hems.types.money.BalanceResult;
import de.hems.utils.money.MoneyStore;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The lotto: 4 from 15, a tip costs bits, and whoever has all four numbers takes the pot.
 * <p>
 * Every tip goes into the pot in full. If nobody hits all four, the pot stays and grows into the next
 * round; if several do, it is shared per winning tip, and what does not divide stays in the pot. The draw
 * happens here, at a set day and time, because the launcher is the one part of the network that is always
 * running - a draw that is due while it was off happens as soon as it is back.
 * <p>
 * Buying is one step under one lock: check the tips, take the money, write the tips down. The money is
 * the launcher's as well, so there is no second place where it could go wrong halfway.
 */
public class LottoService {

    /** How many tips one purchase may hold, so a typo in a number is not a fortune. */
    public static final int MAX_TIPS_PER_PURCHASE = 20;

    private static final Map<String, DayOfWeek> DAYS = Map.ofEntries(
            Map.entry("MONTAG", DayOfWeek.MONDAY), Map.entry("DIENSTAG", DayOfWeek.TUESDAY),
            Map.entry("MITTWOCH", DayOfWeek.WEDNESDAY), Map.entry("DONNERSTAG", DayOfWeek.THURSDAY),
            Map.entry("FREITAG", DayOfWeek.FRIDAY), Map.entry("SAMSTAG", DayOfWeek.SATURDAY),
            Map.entry("SONNTAG", DayOfWeek.SUNDAY));

    private final LottoStore store;
    private final MoneyStore money;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private ScheduledExecutorService timer;

    public LottoService(LottoStore store, MoneyStore money) {
        this(store, money, Clock.systemUTC());
    }

    public LottoService(LottoStore store, MoneyStore money, Clock clock) {
        this.store = store;
        this.money = money;
        this.clock = clock;
        synchronized (this) {
            if (store.getNextDrawAt() <= 0) {
                store.setNextDrawAt(nextDraw(clock.millis()));
                store.save();
            }
        }
    }

    /** What went wrong, for the player - or nothing. */
    public record Result<T>(T value, String error) {
        static <T> Result<T> ok(T value) {
            return new Result<>(value, null);
        }

        static <T> Result<T> fail(String error) {
            return new Result<>(null, error);
        }
    }

    /**
     * Starts watching the clock for the draw.
     */
    public void start() {
        timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "lotto-draw");
            thread.setDaemon(true);
            return thread;
        });
        timer.scheduleAtFixedRate(() -> {
            try {
                drawIfDue();
            } catch (RuntimeException e) {
                System.out.println("The lotto draw failed: " + e.getMessage());
            }
        }, 10, 15, TimeUnit.SECONDS);
    }

    public void stop() {
        if (timer != null) timer.shutdownNow();
    }

    // ---- reading ---------------------------------------------------------------------------------------------

    public synchronized LottoStatus status() {
        List<LottoDraw> history = store.getHistory();
        return new LottoStatus(store.getRound(), store.getPot(), store.getPrice(), store.getNextDrawAt(),
                store.getSchedule(), store.getTickets().size(), history.isEmpty() ? null : history.getFirst());
    }

    /**
     * @param player an account
     * @return its tips in the round that is running
     */
    public synchronized List<LottoTicket> ticketsOf(UUID player) {
        List<LottoTicket> mine = new ArrayList<>();
        for (LottoTicket ticket : store.getTickets()) {
            if (ticket.getPlayer().equals(player)) mine.add(ticket);
        }
        return mine;
    }

    public synchronized List<LottoTicket> tickets() {
        return store.getTickets();
    }

    public synchronized List<LottoDraw> history() {
        return store.getHistory();
    }

    // ---- buying ----------------------------------------------------------------------------------------------

    /**
     * Buys tips.
     *
     * @param player the account that pays
     * @param name   its name, for the list of winners
     * @param tips   the tips
     * @return the player's tips of this round afterwards, or why not
     */
    public Result<List<LottoTicket>> buy(UUID player, String name, List<int[]> tips) {
        BalanceResult paid;
        synchronized (this) {
            if (player == null) return Result.fail("Kein Spieler angegeben.");
            if (tips == null || tips.isEmpty()) return Result.fail("Kein Tipp angegeben.");
            if (tips.size() > MAX_TIPS_PER_PURCHASE) {
                return Result.fail("Höchstens " + MAX_TIPS_PER_PURCHASE + " Tipps auf einmal.");
            }
            List<int[]> checked = new ArrayList<>();
            for (int[] tip : tips) {
                int[] normalized = LottoTicket.normalize(tip);
                if (normalized == null) {
                    return Result.fail("Ein Tipp sind " + LottoTicket.NUMBERS + " verschiedene Zahlen von 1 bis "
                            + LottoTicket.HIGHEST + ".");
                }
                checked.add(normalized);
            }
            long cost = (long) store.getPrice() * checked.size();
            if (cost > Integer.MAX_VALUE) return Result.fail("Das ist zu viel auf einmal.");
            paid = money.change(player.toString(), (int) -cost, true);
            if (!paid.isSuccessful()) return Result.fail(paid.getMessage());
            long now = clock.millis();
            for (int[] tip : checked) {
                store.addTicket(new LottoTicket(store.nextTicketId(), store.getRound(), player, name, tip, now));
            }
            store.setPot((int) Math.min(Integer.MAX_VALUE, (long) store.getPot() + cost));
            store.save();
            System.out.println("Lotto: " + name + " bought " + checked.size() + " tips for " + cost + " bits");
        }
        announceBalance(paid);
        announce(null);
        return Result.ok(ticketsOf(player));
    }

    // ---- drawing ---------------------------------------------------------------------------------------------

    /**
     * Draws if it is time. A round without a single tip is not drawn - it would only say that nobody won
     * - it just moves on to the next date.
     */
    public void drawIfDue() {
        boolean draw;
        synchronized (this) {
            if (clock.millis() < store.getNextDrawAt()) return;
            draw = !store.getTickets().isEmpty();
            if (!draw) {
                store.setNextDrawAt(nextDraw(clock.millis()));
                store.save();
            }
        }
        // checked again under the lock: an admin may have drawn in between
        if (draw) draw(null, true);
        else announce(null);
    }

    /**
     * Draws now.
     *
     * @param numbers the numbers to draw, or {@code null} for random ones - only tests choose them
     * @return what came out, or why not
     */
    public Result<LottoDraw> draw(int[] numbers) {
        return draw(numbers, false);
    }

    private Result<LottoDraw> draw(int[] numbers, boolean onlyIfDue) {
        LottoDraw draw;
        List<BalanceResult> payouts = new ArrayList<>();
        synchronized (this) {
            if (onlyIfDue && clock.millis() < store.getNextDrawAt()) return Result.fail("Noch nicht Zeit.");
            List<LottoTicket> tickets = store.getTickets();
            if (tickets.isEmpty()) return Result.fail("In dieser Runde hat noch niemand getippt.");
            int[] drawn = numbers != null ? LottoTicket.normalize(numbers) : LottoTicket.random(random);
            if (drawn == null) return Result.fail("Das sind keine gültigen Lottozahlen.");
            List<LottoTicket> winning = new ArrayList<>();
            for (LottoTicket ticket : tickets) {
                if (ticket.wins(drawn)) winning.add(ticket);
            }
            int pot = store.getPot();
            int each = winning.isEmpty() ? 0 : pot / winning.size();
            List<String> names = new ArrayList<>();
            List<UUID> ids = new ArrayList<>();
            for (LottoTicket ticket : winning) {
                names.add(ticket.getPlayerName());
                ids.add(ticket.getPlayer());
                if (each > 0) payouts.add(money.change(ticket.getPlayer().toString(), each, false));
            }
            long now = clock.millis();
            draw = new LottoDraw(store.getRound(), drawn, now, pot, tickets.size(), names, ids, each);
            store.addDraw(draw);
            // nobody hit it: the whole pot goes on; somebody did: only what did not divide stays
            store.setPot(pot - each * winning.size());
            store.setRound(store.getRound() + 1);
            store.clearTickets();
            store.setNextDrawAt(nextDraw(now));
            store.save();
            System.out.println("Lotto round " + draw.getRound() + ": " + LottoTicket.format(drawn) + ", "
                    + winning.size() + " winning tips of " + tickets.size() + ", " + each + " bits each");
        }
        for (BalanceResult payout : payouts) announceBalance(payout);
        announce(draw);
        return Result.ok(draw);
    }

    // ---- settings --------------------------------------------------------------------------------------------

    /**
     * @param price what a tip costs from now on; tips already bought stay as they are
     * @return why not, or {@code null}
     */
    public String setPrice(int price) {
        if (price < 1) return "Ein Tipp muss mindestens 1 Bit kosten.";
        synchronized (this) {
            store.setPrice(price);
            store.save();
        }
        announce(null);
        return null;
    }

    /**
     * @param schedule day and time, like "sonntag 20:00"
     * @return why not, or {@code null}
     */
    public String setSchedule(String schedule) {
        Schedule parsed = parse(schedule);
        if (parsed == null) return "So geht das: <Wochentag> <HH:mm>, z.B. \"sonntag 20:00\".";
        synchronized (this) {
            store.setSchedule(parsed.text());
            store.setNextDrawAt(nextDraw(clock.millis()));
            store.save();
        }
        announce(null);
        return null;
    }

    private record Schedule(DayOfWeek day, LocalTime time, String text) {
    }

    private static Schedule parse(String text) {
        if (text == null) return null;
        String[] parts = text.trim().toUpperCase(Locale.ROOT).split("\\s+");
        if (parts.length != 2) return null;
        DayOfWeek day = DAYS.get(parts[0]);
        if (day == null) return null;
        try {
            LocalTime time = LocalTime.parse(parts[1].length() == 4 ? "0" + parts[1] : parts[1]);
            return new Schedule(day, time, parts[0] + " " + time);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * @param after a moment
     * @return the first draw date after it
     */
    long nextDraw(long after) {
        Schedule schedule = parse(store.getSchedule());
        if (schedule == null) schedule = parse("SONNTAG 20:00");
        ZoneId zone;
        try {
            zone = ZoneId.of(store.getZone());
        } catch (RuntimeException e) {
            zone = ZoneId.of("Europe/Berlin");
        }
        ZonedDateTime from = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(after), zone);
        ZonedDateTime next = from.with(TemporalAdjusters.nextOrSame(schedule.day()))
                .with(schedule.time()).withSecond(0).withNano(0);
        if (!next.toInstant().isAfter(from.toInstant())) {
            next = from.plusDays(1).with(TemporalAdjusters.nextOrSame(schedule.day()))
                    .with(schedule.time()).withSecond(0).withNano(0);
        }
        return next.toInstant().toEpochMilli();
    }

    // ---- telling the network ---------------------------------------------------------------------------------

    private void announce(LottoDraw draw) {
        try {
            ListenerAdapter.sendListeners(new LottoUpdatedEvent(status(), draw));
        } catch (Exception e) {
            System.out.println("Could not announce the lotto: " + e.getMessage());
        }
    }

    private static void announceBalance(BalanceResult result) {
        if (result == null || !result.isSuccessful()) return;
        try {
            ListenerAdapter.sendListeners(new BalanceUpdatedEvent(result.getHolder(), result.getBalance()));
        } catch (Exception e) {
            System.out.println("Could not announce a balance: " + e.getMessage());
        }
    }
}
