package de.hems.types.event;

import de.hems.types.poker.PokerFormat;

/**
 * The knobs of a poker night, read out of the free settings an {@link EventData} carries.
 * <p>
 * Same idea as {@link BedwarsEventSettings}: the launcher stores an event as a map of strings and has no
 * idea what any of them mean, and this is the one place that does. Everything an admin can decide about a
 * poker night goes through here, so a new knob is a constant, a getter and a setter rather than a new
 * column somewhere.
 * <p>
 * <b>Chips are bits, one for one.</b> There is no exchange rate, on purpose: a rate means every player at
 * the table has to do arithmetic to know what a raise costs them, and the first time somebody works it out
 * wrong they lose real money. So a 200 chip pot is 200 bits, minus what the house keeps.
 */
public final class PokerEventSettings {

    /** Cash game or tournament. */
    public static final String FORMAT = "poker.format";
    /** The server the night is played on, written when it is put up. */
    public static final String SERVER = "poker.server";
    /** What the house keeps of every pot, in parts per thousand - 30 is three percent. */
    public static final String RAKE_PERMILLE = "poker.rake-permille";
    /** The most the house keeps of a single pot, counted in big blinds. Zero means no ceiling. */
    public static final String RAKE_CAP_BB = "poker.rake-cap-bb";
    /** What one buy-in costs, in bits, and therefore how many chips it is worth. */
    public static final String BUY_IN = "poker.buy-in";
    /** The small blind in chips. The big blind is twice this. */
    public static final String SMALL_BLIND = "poker.small-blind";
    /** How many seats one table has. */
    public static final String SEATS = "poker.seats";
    /** How many tables the casino holds. */
    public static final String TABLES = "poker.tables";
    /** Whether players may put bots into an empty seat. */
    public static final String BOTS = "poker.bots";
    /** What putting a bot down costs on top of its stack, in bits. */
    public static final String BOT_FEE = "poker.bot-fee";
    /** How many hands somebody has to play before the ranking counts them. */
    public static final String MIN_HANDS = "poker.min-hands";
    /** How many bits somebody has to have bought in for, in total, before the ranking counts them. */
    public static final String MIN_VOLUME = "poker.min-volume";
    /** How often the blinds go up in a tournament, in minutes. */
    public static final String BLIND_UP_MINUTES = "poker.blind-up-minutes";

    /** Three percent, which is what a house normally takes. */
    public static final int DEFAULT_RAKE_PERMILLE = 30;
    /** Ten percent, and there is no honest reason to go past it. */
    public static final int MAX_RAKE_PERMILLE = 100;
    /** The default ceiling, in big blinds. Without one, a single huge pot pays the house more than the evening. */
    public static final int DEFAULT_RAKE_CAP_BB = 50;
    public static final int DEFAULT_BUY_IN = 1000;
    public static final int DEFAULT_SMALL_BLIND = 10;
    public static final int DEFAULT_SEATS = 8;
    public static final int MIN_SEATS = 2;
    /** Nine is what fits round a table without the seats overlapping, and it is where real tables stop too. */
    public static final int MAX_SEATS = 9;
    public static final int DEFAULT_TABLES = 3;
    public static final int MAX_TABLES = 8;
    public static final int DEFAULT_BOT_FEE = 100;
    public static final int DEFAULT_BLIND_UP_MINUTES = 12;

    /**
     * How many hands count as having played, rather than having sat down once and got lucky.
     * <p>
     * The ranking is a ratio, and a ratio has a hole in it: buy in for the minimum, win one hand, stand up,
     * and you are on 2.0 while somebody who played all night for a real profit is on 1.4. Both bars below
     * are what closes it. They are settings rather than constants because the right number depends on how
     * long the evening runs.
     */
    public static final int DEFAULT_MIN_HANDS = 20;

    /** And how much had to go through their hands, as a multiple of one buy-in. */
    public static final int DEFAULT_MIN_VOLUME_BUY_INS = 3;

    private final EventData event;

    public PokerEventSettings(EventData event) {
        this.event = event;
    }

    /**
     * Writes the defaults onto an event that has none yet, so a freshly created poker night is playable
     * without an admin having touched a single knob.
     */
    public void applyDefaults() {
        if (event.getSetting(FORMAT, null) == null) setFormat(PokerFormat.CASH);
        if (event.getSetting(RAKE_PERMILLE, null) == null) setRakePermille(DEFAULT_RAKE_PERMILLE);
        if (event.getSetting(RAKE_CAP_BB, null) == null) setRakeCapBigBlinds(DEFAULT_RAKE_CAP_BB);
        if (event.getSetting(BUY_IN, null) == null) setBuyIn(DEFAULT_BUY_IN);
        if (event.getSetting(SMALL_BLIND, null) == null) setSmallBlind(DEFAULT_SMALL_BLIND);
        if (event.getSetting(SEATS, null) == null) setSeats(DEFAULT_SEATS);
        if (event.getSetting(TABLES, null) == null) setTables(DEFAULT_TABLES);
        if (event.getSetting(BOTS, null) == null) setBotsAllowed(true);
        if (event.getSetting(BOT_FEE, null) == null) setBotFee(DEFAULT_BOT_FEE);
        if (event.getSetting(MIN_HANDS, null) == null) setMinHands(DEFAULT_MIN_HANDS);
        if (event.getSetting(MIN_VOLUME, null) == null) {
            setMinVolume(DEFAULT_MIN_VOLUME_BUY_INS * getBuyIn());
        }
        if (event.getSetting(BLIND_UP_MINUTES, null) == null) {
            setBlindUpMinutes(DEFAULT_BLIND_UP_MINUTES);
        }
    }

    public PokerFormat getFormat() {
        return PokerFormat.byName(event.getSetting(FORMAT, null), PokerFormat.CASH);
    }

    public void setFormat(PokerFormat format) {
        event.setSetting(FORMAT, (format == null ? PokerFormat.CASH : format).name());
    }

    /**
     * @return the server the night runs on, or {@code null} while it has not been put up yet
     */
    public String getServer() {
        String server = event.getSetting(SERVER, "");
        return server == null || server.isBlank() ? null : server;
    }

    public void setServer(String server) {
        event.setSetting(SERVER, server == null ? "" : server);
    }

    /**
     * @return what the house keeps of a pot, in parts per thousand
     */
    public int getRakePermille() {
        return clamp(event.getNumber(RAKE_PERMILLE, DEFAULT_RAKE_PERMILLE), 0, MAX_RAKE_PERMILLE);
    }

    public void setRakePermille(int permille) {
        event.setSetting(RAKE_PERMILLE, String.valueOf(clamp(permille, 0, MAX_RAKE_PERMILLE)));
    }

    /**
     * @return the rake written the way it is spoken about, like "3%" or "2,5%"
     */
    public String getRakeText() {
        int permille = getRakePermille();
        if (permille % 10 == 0) return (permille / 10) + "%";
        return (permille / 10) + "," + (permille % 10) + "%";
    }

    public int getRakeCapBigBlinds() {
        return Math.max(0, event.getNumber(RAKE_CAP_BB, DEFAULT_RAKE_CAP_BB));
    }

    public void setRakeCapBigBlinds(int bigBlinds) {
        event.setSetting(RAKE_CAP_BB, String.valueOf(Math.max(0, bigBlinds)));
    }

    /**
     * What the house keeps of one pot.
     * <p>
     * Rounded down, and never taken from a pot nobody contested: a hand where everybody folded to the blinds
     * cost nobody anything, and taking a cut of it is how a table bleeds dry without anybody noticing.
     *
     * @param pot     the size of the pot in chips
     * @param contested whether the hand went past the first betting round with more than one player left in
     * @return what the house takes
     */
    public int rakeOf(int pot, boolean contested) {
        if (pot <= 0 || !contested) return 0;
        long cut = (long) pot * getRakePermille() / 1000L;
        int cap = getRakeCapBigBlinds() * getBigBlind();
        if (cap > 0) cut = Math.min(cut, cap);
        // never take more than the pot, however the numbers are set
        return (int) Math.min(cut, pot);
    }

    public int getBuyIn() {
        return Math.max(1, event.getNumber(BUY_IN, DEFAULT_BUY_IN));
    }

    public void setBuyIn(int buyIn) {
        event.setSetting(BUY_IN, String.valueOf(Math.max(1, buyIn)));
    }

    public int getSmallBlind() {
        return Math.max(1, event.getNumber(SMALL_BLIND, DEFAULT_SMALL_BLIND));
    }

    public void setSmallBlind(int smallBlind) {
        event.setSetting(SMALL_BLIND, String.valueOf(Math.max(1, smallBlind)));
    }

    public int getBigBlind() {
        return getSmallBlind() * 2;
    }

    public int getSeats() {
        return clamp(event.getNumber(SEATS, DEFAULT_SEATS), MIN_SEATS, MAX_SEATS);
    }

    public void setSeats(int seats) {
        event.setSetting(SEATS, String.valueOf(clamp(seats, MIN_SEATS, MAX_SEATS)));
    }

    public int getTables() {
        return clamp(event.getNumber(TABLES, DEFAULT_TABLES), 1, MAX_TABLES);
    }

    public void setTables(int tables) {
        event.setSetting(TABLES, String.valueOf(clamp(tables, 1, MAX_TABLES)));
    }

    public boolean isBotsAllowed() {
        return event.getFlag(BOTS, true);
    }

    public void setBotsAllowed(boolean allowed) {
        event.setSetting(BOTS, String.valueOf(allowed));
    }

    public int getBotFee() {
        return Math.max(0, event.getNumber(BOT_FEE, DEFAULT_BOT_FEE));
    }

    public void setBotFee(int fee) {
        event.setSetting(BOT_FEE, String.valueOf(Math.max(0, fee)));
    }

    public int getMinHands() {
        return Math.max(0, event.getNumber(MIN_HANDS, DEFAULT_MIN_HANDS));
    }

    public void setMinHands(int hands) {
        event.setSetting(MIN_HANDS, String.valueOf(Math.max(0, hands)));
    }

    public int getMinVolume() {
        return Math.max(0, event.getNumber(MIN_VOLUME, DEFAULT_MIN_VOLUME_BUY_INS * getBuyIn()));
    }

    public void setMinVolume(int volume) {
        event.setSetting(MIN_VOLUME, String.valueOf(Math.max(0, volume)));
    }

    public int getBlindUpMinutes() {
        return Math.max(1, event.getNumber(BLIND_UP_MINUTES, DEFAULT_BLIND_UP_MINUTES));
    }

    public void setBlindUpMinutes(int minutes) {
        event.setSetting(BLIND_UP_MINUTES, String.valueOf(Math.max(1, minutes)));
    }

    /**
     * @return how many people the casino seats in total, which is what the lobby warns about before it
     *         drags a full server across
     */
    public int getCapacity() {
        return getSeats() * getTables();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
