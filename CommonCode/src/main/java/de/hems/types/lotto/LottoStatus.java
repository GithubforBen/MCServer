package de.hems.types.lotto;

import java.io.Serializable;

/** Where the lotto stands: the round, the pot, the price of a tip and when the next draw is. */
public class LottoStatus implements Serializable {

    private static final long serialVersionUID = 4602L;

    private int round;
    private int pot;
    private int price;
    private long nextDrawAt;
    /** The draw day and time the way an admin sets and reads them, like "SONNTAG 20:00". */
    private String schedule;
    private int tickets;
    private LottoDraw lastDraw;

    public LottoStatus() {
    }

    public LottoStatus(int round, int pot, int price, long nextDrawAt, String schedule, int tickets, LottoDraw lastDraw) {
        this.round = round;
        this.pot = pot;
        this.price = price;
        this.nextDrawAt = nextDrawAt;
        this.schedule = schedule;
        this.tickets = tickets;
        this.lastDraw = lastDraw;
    }

    /**
     * @return whether it has arrived from the launcher at all
     */
    public boolean isKnown() {
        return round > 0;
    }

    public int getRound() {
        return round;
    }

    public int getPot() {
        return pot;
    }

    public int getPrice() {
        return price;
    }

    public long getNextDrawAt() {
        return nextDrawAt;
    }

    public String getSchedule() {
        return schedule;
    }

    public int getTickets() {
        return tickets;
    }

    public LottoDraw getLastDraw() {
        return lastDraw;
    }

    /**
     * @param millis a span
     * @return it in words, like "2 T 3 Std" or "14 Min"
     */
    public static String span(long millis) {
        if (millis <= 0) return "gleich";
        long minutes = millis / 60000L;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) return days + " T " + (hours % 24) + " Std";
        if (hours > 0) return hours + " Std " + (minutes % 60) + " Min";
        return Math.max(1, minutes) + " Min";
    }
}
