package de.hems.utils.poker;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.events.PokerEvents;
import de.hems.types.event.AwardData;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.PokerEventSettings;
import de.hems.types.event.PrizeData;
import de.hems.types.money.BalanceResult;
import de.hems.types.poker.PokerStatsData;
import de.hems.utils.event.AwardStore;
import de.hems.utils.money.MoneyStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Closes a poker night: hands back what was still on the tables, then pays out the ranking.
 * <p>
 * The two halves are not the same kind of thing and are deliberately done in that order. The chips are the
 * players' own money - they paid bits for them - so they go back whatever happened to the event, including
 * when an admin called it off. The prizes are a reward, and a night that was called off never happened.
 * <p>
 * Handing the stacks back here rather than on the casino server is what makes a poker night survivable. The
 * server holding the tables can die - a crash, a machine that runs out of memory, an admin who stops the
 * wrong thing - and the money is not on it: it is in the launcher's record of who had how much in front of
 * them, written on every hand, and this is what turns that record back into bits.
 */
public class PokerSettlement {

    private final PokerStatsStore stats;
    private final MoneyStore money;
    private final AwardStore awards;
    private final PokerEvents announcer;

    public PokerSettlement(PokerStatsStore stats, MoneyStore money, AwardStore awards, PokerEvents announcer) {
        this.stats = stats;
        this.money = money;
        this.awards = awards;
        this.announcer = announcer;
    }

    /**
     * Settles one poker night.
     *
     * @param event the night that is over
     */
    public void settle(EventData event) {
        if (event == null) return;
        int handedBack = payOutOpenStacks(event);
        if (event.getState() != EventState.CANCELLED) {
            awardPlaces(event);
            awardParticipation(event);
        }
        stopCasino(event);
        System.out.println("Settled poker night " + event.getName() + " - " + handedBack
                + " bits were still on the tables and went back to their owners.");
    }

    /**
     * Turns what is still sitting in front of somebody back into bits.
     * <p>
     * The row is only cleared once the credit went through. A stack that could not be paid out stays on the
     * row and is offered again the next time the night is settled, which is worth more than a tidy file:
     * the alternative is a player whose money quietly stopped existing.
     *
     * @param event the night
     * @return how many bits went back
     */
    private int payOutOpenStacks(EventData event) {
        int total = 0;
        List<PokerStatsData> paid = new ArrayList<>();
        for (PokerStatsData row : stats.getRowsOf(event.getId())) {
            int open = row.getOpenStack();
            if (open <= 0) continue;
            BalanceResult result = money.change(row.getPlayerId().toString(), open, false);
            if (!result.isSuccessful()) {
                System.out.println("Could not hand " + open + " bits back to " + row.getPlayerName()
                        + " after " + event.getName() + ": " + result.getMessage()
                        + " - the stack stays on the row and is offered again.");
                continue;
            }
            total += open;
            paid.add(row);
        }
        if (paid.isEmpty()) return 0;
        // the store does the bookkeeping half: the stack moves from "still at risk" to "cashed out", which
        // is also what stops a second settlement paying the same chips out again
        for (PokerStatsData row : stats.clearOpenStacks(event.getId())) {
            announcer.announce(row);
        }
        return total;
    }

    /**
     * Puts the prizes of the first three places aside.
     *
     * @param event the night
     */
    private void awardPlaces(EventData event) {
        PokerEventSettings settings = new PokerEventSettings(event);
        List<PokerStatsData> ranking = ranking(event, settings);
        for (int place = 1; place <= PrizeData.PLACES && place <= ranking.size(); place++) {
            PrizeData prize = PrizeData.ofPlace(event, place);
            if (prize.isEmpty()) continue;
            awards.put(new AwardData(ranking.get(place - 1).getPlayerId(), event, place, prize));
        }
    }

    /**
     * Gives everybody who really played their prize.
     * <p>
     * "Really played" is the same bar the ranking uses. Without it the participation prize is a reason to
     * sit down for one hand, and a poker night where the cheapest way to earn is not to play poker is not
     * a poker night.
     *
     * @param event the night
     */
    private void awardParticipation(EventData event) {
        PrizeData prize = PrizeData.ofParticipation(event);
        if (prize.isEmpty()) return;
        PokerEventSettings settings = new PokerEventSettings(event);
        for (PokerStatsData row : ranking(event, settings)) {
            awards.put(new AwardData(row.getPlayerId(), event, AwardData.PARTICIPATION, prize));
        }
    }

    /**
     * @param event    the night
     * @param settings its knobs
     * @return the rows that cleared both bars, best ratio first
     */
    public List<PokerStatsData> ranking(EventData event, PokerEventSettings settings) {
        List<PokerStatsData> ranked = new ArrayList<>();
        for (PokerStatsData row : stats.getRowsOf(event.getId())) {
            if (row.qualifies(settings.getMinHands(), settings.getMinVolume())) ranked.add(row);
        }
        ranked.sort(PokerStatsData.rankingOrder());
        return ranked;
    }

    /**
     * Switches the casino off. Its world is thrown away with the server directory the same way a bedwars
     * round's is - a casino that has been settled has nothing left in it that anybody wants.
     *
     * @param event the night
     */
    private void stopCasino(EventData event) {
        String server = new PokerEventSettings(event).getServer();
        if (server == null) return;
        try {
            ListenerAdapter.ServerName name = ListenerAdapter.ServerName.valueOf(server);
            if (Main.getInstance().getServerHandler().doesInstanceExist(name)) {
                Main.getInstance().getServerHandler().stop(name);
            }
        } catch (Exception e) {
            System.out.println("Could not stop the casino server " + server + ": " + e.getMessage());
        }
    }

    /**
     * Forgets a night whose event was deleted - after handing back whatever was still on its tables.
     * <p>
     * Deleting a running poker night is a button an admin has, and without this it would be the most
     * expensive button in the network: every chip in front of every player was paid for with bits, and
     * dropping the rows would make all of it stop existing. So the stacks go back first and the rows go
     * only once they have.
     *
     * @param eventId the night
     */
    public void discard(UUID eventId) {
        int handedBack = 0;
        for (PokerStatsData row : stats.getRowsOf(eventId)) {
            int open = row.getOpenStack();
            if (open <= 0) continue;
            BalanceResult result = money.change(row.getPlayerId().toString(), open, false);
            if (!result.isSuccessful()) {
                System.out.println("Could not hand " + open + " bits back to " + row.getPlayerName()
                        + " while deleting a poker night: " + result.getMessage()
                        + " - the row is kept so nothing is lost.");
                // keeping the row is the whole point: a row that is still there can be paid out by hand,
                // a row that has been deleted is a player who is simply out of money
                return;
            }
            handedBack += open;
        }
        int removed = stats.discard(eventId);
        if (removed > 0) {
            System.out.println("Removed " + removed + " poker rows of a deleted event, " + handedBack
                    + " bits went back to their owners first.");
        }
    }
}
