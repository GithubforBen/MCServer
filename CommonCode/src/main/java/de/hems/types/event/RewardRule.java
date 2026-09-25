package de.hems.types.event;

import java.io.Serializable;
import java.util.Locale;

/**
 * One reward of an event: what is handed out, and who gets it.
 * <p>
 * The two halves are deliberately separate. The prize is the same {@link PrizeData} every event has always
 * used - money and a handful of items. What is new is the question in front of it: a placing ({@code #1}),
 * a stretch of placings ({@code Platz 4-10}, {@code ab Platz 10}), a number of kills, or simply having taken
 * part. Every rule that fits a player pays out, so the winner with five kills takes home the first prize
 * and the kill prize - they were earned for different things.
 * <p>
 * Stored as one line in the free settings of an {@link EventData}, next to the prize it carries:
 * {@code who=place:4:10;money=100;items=DIAMOND:2}. {@link PrizeData#parse} skips the {@code who} part it
 * does not know, which is what lets the prize keep its own format.
 */
public class RewardRule implements Serializable {

    private static final long serialVersionUID = 4340L;

    /** A place range that runs to the last player. */
    public static final int OPEN_END = 0;

    /** What decides whether a player gets the reward. */
    public enum Condition {
        /** A placing, or a stretch of placings. */
        PLACE("Platzierung"),
        /** At least so many kills. Only means something on an event that counts them. */
        KILLS("Kills"),
        /** Everybody who took part. */
        PARTICIPATION("Teilnahme");

        private final String title;

        Condition(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private Condition condition = Condition.PLACE;
    /** The best placing that still gets it, for {@link Condition#PLACE}. */
    private int from = 1;
    /** The worst placing that still gets it, {@link #OPEN_END} for "down to the last". */
    private int to = 1;
    /** How many kills it takes, for {@link Condition#KILLS}. */
    private int kills = 1;
    private PrizeData prize = new PrizeData();

    public RewardRule() {
    }

    /**
     * @param place the one placing that gets it
     * @param prize what it is worth
     * @return a rule for exactly that place
     */
    public static RewardRule place(int place, PrizeData prize) {
        return places(place, place, prize);
    }

    /**
     * @param from  the best placing that gets it
     * @param to    the worst, or {@link #OPEN_END}
     * @param prize what it is worth
     * @return a rule for that stretch of placings
     */
    public static RewardRule places(int from, int to, PrizeData prize) {
        RewardRule rule = new RewardRule();
        rule.condition = Condition.PLACE;
        rule.setRange(from, to);
        rule.prize = prize == null ? new PrizeData() : prize;
        return rule;
    }

    /**
     * @param kills how many kills it takes
     * @param prize what it is worth
     * @return a rule for everybody with at least that many kills
     */
    public static RewardRule kills(int kills, PrizeData prize) {
        RewardRule rule = new RewardRule();
        rule.condition = Condition.KILLS;
        rule.setKills(kills);
        rule.prize = prize == null ? new PrizeData() : prize;
        return rule;
    }

    /**
     * @param prize what it is worth
     * @return a rule for everybody who took part
     */
    public static RewardRule participation(PrizeData prize) {
        RewardRule rule = new RewardRule();
        rule.condition = Condition.PARTICIPATION;
        rule.prize = prize == null ? new PrizeData() : prize;
        return rule;
    }

    /**
     * @param standing how somebody did
     * @return whether this rule rewards them
     */
    public boolean matches(EventStanding standing) {
        if (standing == null) return false;
        return switch (condition) {
            case PARTICIPATION -> true;
            case KILLS -> standing.getKills() >= kills;
            case PLACE -> standing.isRanked()
                    && standing.getPlace() >= from
                    && (to == OPEN_END || standing.getPlace() <= to);
        };
    }

    /**
     * @return who gets it, the way a player reads it: "#1", "Platz 4-10", "ab Platz 10", "ab 5 Kills"
     */
    public String describeWho() {
        return switch (condition) {
            case PARTICIPATION -> "Teilnahme";
            case KILLS -> "ab " + kills + (kills == 1 ? " Kill" : " Kills");
            case PLACE -> {
                if (to == OPEN_END) yield from <= 1 ? "Alle Plätze" : "ab Platz " + from;
                if (from == to) yield "#" + from;
                yield "Platz " + from + "-" + to;
            }
        };
    }

    /**
     * @return the rule written out, readable in a config file
     */
    public String serialize() {
        String who = switch (condition) {
            case PARTICIPATION -> "participation";
            case KILLS -> "kills:" + kills;
            case PLACE -> "place:" + from + ":" + to;
        };
        return "who=" + who + ";" + getPrize().serialize();
    }

    /**
     * @param text a rule as {@link #serialize()} wrote it
     * @return the rule, or {@code null} if the text says nothing about who gets it
     */
    public static RewardRule parse(String text) {
        if (text == null || text.isBlank()) return null;
        String who = null;
        for (String part : text.split(";")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].trim().equalsIgnoreCase("who")) {
                who = pair[1].trim().toLowerCase(Locale.ROOT);
                break;
            }
        }
        if (who == null) return null;
        PrizeData prize = PrizeData.parse(text);
        String[] spec = who.split(":");
        try {
            return switch (spec[0]) {
                case "participation" -> participation(prize);
                case "kills" -> kills(Integer.parseInt(spec[1]), prize);
                case "place" -> places(Integer.parseInt(spec[1]),
                        spec.length > 2 ? Integer.parseInt(spec[2]) : Integer.parseInt(spec[1]), prize);
                default -> null;
            };
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // a rule whose question cannot be read cannot be answered either - better none than the wrong one
            return null;
        }
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition == null ? Condition.PLACE : condition;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    /**
     * Sets the stretch of placings, keeping it the right way round.
     *
     * @param from the best placing, at least one
     * @param to   the worst placing, or {@link #OPEN_END}
     */
    public void setRange(int from, int to) {
        this.from = Math.max(1, from);
        this.to = to <= OPEN_END ? OPEN_END : Math.max(this.from, to);
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(1, kills);
    }

    public PrizeData getPrize() {
        if (prize == null) prize = new PrizeData();
        return prize;
    }

    public void setPrize(PrizeData prize) {
        this.prize = prize == null ? new PrizeData() : prize;
    }

    /**
     * @return a copy that can be edited without touching this one
     */
    public RewardRule copy() {
        RewardRule copy = parse(serialize());
        return copy == null ? new RewardRule() : copy;
    }
}
