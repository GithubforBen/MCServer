package de.hems.types.lotto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** What came out of one draw: the numbers, who won and how much. */
public class LottoDraw implements Serializable {

    private static final long serialVersionUID = 4601L;

    private int round;
    private int[] numbers;
    private long drawnAt;
    /** What was in the pot before it was paid out. */
    private int pot;
    private int tickets;
    /** One entry per winning tip - a player with two winning tips is in here twice and gets two shares. */
    private ArrayList<String> winners = new ArrayList<>();
    /** The accounts of {@link #winners}, in the same order. */
    private ArrayList<UUID> winnerIds = new ArrayList<>();
    private int payoutEach;

    public LottoDraw() {
    }

    public LottoDraw(int round, int[] numbers, long drawnAt, int pot, int tickets, List<String> winners,
                     List<UUID> winnerIds, int payoutEach) {
        this.round = round;
        this.numbers = numbers.clone();
        this.drawnAt = drawnAt;
        this.pot = pot;
        this.tickets = tickets;
        this.winners = new ArrayList<>(winners);
        this.winnerIds = new ArrayList<>(winnerIds);
        this.payoutEach = payoutEach;
    }

    public int getRound() {
        return round;
    }

    public int[] getNumbers() {
        return numbers.clone();
    }

    public long getDrawnAt() {
        return drawnAt;
    }

    public int getPot() {
        return pot;
    }

    public int getTickets() {
        return tickets;
    }

    public List<String> getWinners() {
        return winners;
    }

    public List<UUID> getWinnerIds() {
        return winnerIds;
    }

    /**
     * @param player an account
     * @return how many winning tips it had
     */
    public int winsOf(UUID player) {
        int wins = 0;
        for (UUID id : winnerIds) {
            if (id.equals(player)) wins++;
        }
        return wins;
    }

    public int getPayoutEach() {
        return payoutEach;
    }
}
