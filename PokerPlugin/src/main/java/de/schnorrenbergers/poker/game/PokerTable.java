package de.schnorrenbergers.poker.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One table, and every rule of Texas Hold'em that decides who owes what and who gets it.
 * <p>
 * Deliberately free of anything from bukkit. Nothing here draws, teleports or messages - it says what
 * happened through {@link TableEvents} and lets the world follow. That is not tidiness for its own sake:
 * these are the rules that move real bits, and rules that can be run and checked without a server around
 * them are rules that can be trusted.
 * <p>
 * Time is passed in rather than read, for the same reason: a table is driven by {@link #tick(long)} from
 * the server's scheduler, and a test can drive it a minute at a time.
 * <p>
 * <b>The three rules that are usually got wrong</b>, and where they live here:
 * <ul>
 *   <li>The big blind has an option. Posting a blind is not a decision, so the big blind still gets asked
 *       even when everybody only called - see {@link #beginBettingRound}.</li>
 *   <li>An all-in that is smaller than a full raise does not reopen the betting. Whoever had already acted
 *       owes the difference and may not raise off the back of it - see {@link #applyAggression}.</li>
 *   <li>Somebody all-in for less can only win what they could have lost. Everything past it is a second
 *       pot - see {@link Pot#build}.</li>
 * </ul>
 */
public final class PokerTable {

    /** How long the table waits between hands, so people can see what happened. */
    private static final long BREAK_MILLIS = 5_000L;
    /** How long the cards stay up at showdown before the next hand. */
    private static final long SHOWDOWN_MILLIS = 7_000L;
    /**
     * How many times in a row somebody may run out of time before they are sat out.
     * <p>
     * One is a phone call. Three in a row is somebody who is not there, and somebody who is not there loses
     * a blind every orbit - real bits, out of a stack they are not watching.
     */
    private static final int TIMEOUTS_BEFORE_SITTING_OUT = 3;

    private final int id;
    private final TableRules rules;
    private final TableEvents events;
    private final PokerPlayer[] seats;

    private final List<Card> community = new ArrayList<>(5);
    private Deck deck;

    private boolean handRunning;
    private int handNumber;
    private Street street = Street.PREFLOP;
    private int buttonSeat = -1;
    private int actingSeat = -1;

    /** The highest anybody is committed for on this street. */
    private int currentBet;
    /** The smallest legal raise, as a total on this street. */
    private int minRaiseTo;
    /** The size of the last full raise, which is what the next one has to be at least. */
    private int lastRaiseSize;

    /** When the player to act runs out of time, or when the break is over. */
    private long deadline;
    /** When the next hand may start. */
    private long nextHandAt;

    /** How often in a row somebody has let the clock run out. */
    private final java.util.Map<UUID, Integer> timeouts = new java.util.HashMap<>();
    /** Who asked to leave while they were all-in and is let go once the hand is settled. */
    private final Set<PokerPlayer> leaving = new LinkedHashSet<>();

    /** What was dealt out last hand, kept so the table can still be looked at during the break. */
    private final List<ShowdownEntry> lastShowdown = new ArrayList<>();
    private int lastPot;

    /**
     * @param id     which table this is, for names and messages
     * @param seats  how many seats it has
     * @param rules  what it is dealt under
     * @param events where to report to
     */
    public PokerTable(int id, int seats, TableRules rules, TableEvents events) {
        this.id = id;
        this.rules = rules;
        this.events = events == null ? new TableEvents() {
        } : events;
        this.seats = new PokerPlayer[Math.max(2, seats)];
    }

    /* ------------------------------------------------------------------ seats */

    public int getId() {
        return id;
    }

    public TableRules getRules() {
        return rules;
    }

    public int getSeatCount() {
        return seats.length;
    }

    public PokerPlayer seatAt(int seat) {
        return seat < 0 || seat >= seats.length ? null : seats[seat];
    }

    /**
     * @return the seat somebody is in, or {@code -1}
     */
    public int seatOf(PokerPlayer player) {
        for (int seat = 0; seat < seats.length; seat++) {
            if (seats[seat] == player) return seat;
        }
        return -1;
    }

    public PokerPlayer find(UUID playerId) {
        for (PokerPlayer player : seats) {
            if (player != null && player.getId().equals(playerId)) return player;
        }
        return null;
    }

    /**
     * @return everybody sitting here, in seat order, without the empty seats
     */
    public List<PokerPlayer> getPlayers() {
        List<PokerPlayer> players = new ArrayList<>();
        for (PokerPlayer player : seats) {
            if (player != null) players.add(player);
        }
        return players;
    }

    public int getFreeSeats() {
        int free = 0;
        for (PokerPlayer player : seats) {
            if (player == null) free++;
        }
        return free;
    }

    /**
     * Sits somebody down.
     *
     * @param player who is sitting down
     * @param seat   which seat, or {@code -1} for the first free one
     * @return the seat they got, or {@code -1} if there was none
     */
    public int sitDown(PokerPlayer player, int seat) {
        if (player == null) return -1;
        if (seat >= 0) {
            if (seat >= seats.length || seats[seat] != null) return -1;
            seats[seat] = player;
            events.onStateChanged(this);
            return seat;
        }
        for (int free = 0; free < seats.length; free++) {
            if (seats[free] != null) continue;
            seats[free] = player;
            events.onStateChanged(this);
            return free;
        }
        return -1;
    }

    /**
     * Takes somebody off the table, or arranges for it as soon as it is possible.
     * <p>
     * Standing up mid-hand is a fold, and the chips already in the middle stay there - anything else would
     * let somebody take a bet back by leaving. But a player who is all-in cannot fold their way out: they
     * have no decisions left and their chips are already in a pot they can still win. Folding them would
     * quietly hand that pot to somebody else and cost them real bits for pressing "leave". So they stay
     * until the hand is over and are let go the moment it is, with whatever they won.
     * <p>
     * The chips are not returned here. They leave through {@link TableEvents#onPlayerLeft}, which is the
     * only path out of a table - one path instead of two is one place where bits can go missing instead of
     * two.
     *
     * @param player who is leaving
     * @return whether they are gone already, {@code false} when they have to wait for the hand to finish
     */
    public boolean standUp(PokerPlayer player) {
        int seat = seatOf(player);
        if (seat < 0) return false;
        if (handRunning && player.isContesting() && player.isAllIn()) {
            leaving.add(player);
            player.setSittingOut(true);
            events.onStateChanged(this);
            return false;
        }
        if (handRunning && player.isContesting()) {
            boolean wasTheirTurn = actingSeat == seat;
            player.fold();
            player.setActed(true);
            if (wasTheirTurn) {
                events.onActionTaken(this, player, Action.fold());
                // the seat has to be empty before the hand moves on, or the next step would offer a turn
                // to somebody who has already walked away
                release(seat, player);
                advance();
                return true;
            }
        }
        release(seat, player);
        return true;
    }

    /**
     * Empties a seat and hands the chips back through the one door there is.
     */
    private void release(int seat, PokerPlayer player) {
        seats[seat] = null;
        leaving.remove(player);
        int chips = player.getChips();
        player.setChips(0);
        player.endHand();
        timeouts.remove(player.getId());
        events.onPlayerLeft(this, player, chips);
        events.onStateChanged(this);
    }

    /* ------------------------------------------------------------------ state */

    public boolean isHandRunning() {
        return handRunning;
    }

    public int getHandNumber() {
        return handNumber;
    }

    public Street getStreet() {
        return street;
    }

    public List<Card> getCommunity() {
        return List.copyOf(community);
    }

    public int getButtonSeat() {
        return buttonSeat;
    }

    public int getActingSeat() {
        return actingSeat;
    }

    public PokerPlayer getActing() {
        return seatAt(actingSeat);
    }

    public long getDeadline() {
        return deadline;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    /**
     * @return everything that has gone in this hand, the current street included
     */
    public int getPot() {
        int pot = 0;
        for (PokerPlayer player : seats) {
            if (player != null) pot += player.getCommittedTotal();
        }
        return pot;
    }

    /**
     * @return what was in the middle when the last hand ended, so the table can still show it in the break
     */
    public int getLastPot() {
        return lastPot;
    }

    public List<ShowdownEntry> getLastShowdown() {
        return List.copyOf(lastShowdown);
    }

    /**
     * @param player somebody at the table
     * @return what it costs them to stay in
     */
    public int toCall(PokerPlayer player) {
        if (player == null) return 0;
        return Math.max(0, Math.min(currentBet - player.getCommitted(), player.getChips()));
    }

    /**
     * @param player somebody at the table
     * @return the smallest total they may raise to, or {@code 0} when they may not raise
     */
    public int minRaiseTo(PokerPlayer player) {
        if (player == null || !player.mayRaise()) return 0;
        int most = player.getCommitted() + player.getChips();
        // somebody who cannot reach the minimum can still put everything in, and that is the raise they
        // are allowed to make
        return Math.min(minRaiseTo, most);
    }

    /**
     * @param player somebody at the table
     * @return the largest total they can put in, which is no limit and therefore everything they have
     */
    public int maxRaiseTo(PokerPlayer player) {
        return player == null ? 0 : player.getCommitted() + player.getChips();
    }

    /* ------------------------------------------------------------------ the clock */

    /**
     * Drives the table. Called from the server's scheduler, several times a second.
     *
     * @param now the current time in epoch millis
     */
    public void tick(long now) {
        if (!handRunning) {
            if (now >= nextHandAt) startHand(now);
            return;
        }
        if (actingSeat < 0) return;
        if (now < deadline) return;
        timeUp(now);
    }

    /**
     * Nobody decided in time.
     * <p>
     * Checking is free, so a player who could check does; anybody else folds. Both are the decision that
     * costs them nothing they had not already given up, which is the only fair thing to do on somebody's
     * behalf.
     */
    private void timeUp(long now) {
        PokerPlayer player = getActing();
        if (player == null) {
            advance();
            return;
        }
        int strikes = timeouts.merge(player.getId(), 1, Integer::sum);
        act(player, toCall(player) == 0 ? Action.check() : Action.fold(), now);
        if (strikes >= TIMEOUTS_BEFORE_SITTING_OUT && !player.isSittingOut()) {
            player.setSittingOut(true);
            events.onStateChanged(this);
        }
    }

    /* ------------------------------------------------------------------ hands */

    /**
     * @return whether there are enough people with chips to deal
     */
    public boolean canStart() {
        return readyPlayers().size() >= 2;
    }

    private List<PokerPlayer> readyPlayers() {
        List<PokerPlayer> ready = new ArrayList<>();
        for (PokerPlayer player : seats) {
            if (player != null && player.isReady(rules.getBigBlind())) ready.add(player);
        }
        return ready;
    }

    /**
     * Deals a hand, if there is anybody to deal to.
     *
     * @param now the current time
     */
    public void startHand(long now) {
        if (handRunning) return;
        List<PokerPlayer> ready = readyPlayers();
        if (ready.size() < 2) {
            // nothing to do but wait, and waiting quietly beats trying again every tick
            nextHandAt = now + BREAK_MILLIS;
            return;
        }

        handRunning = true;
        handNumber++;
        street = Street.PREFLOP;
        community.clear();
        lastShowdown.clear();
        deck = new Deck();

        for (PokerPlayer player : seats) {
            if (player == null) continue;
            if (ready.contains(player)) {
                player.beginHand();
            } else {
                player.endHand();
            }
        }

        buttonSeat = nextOccupied(buttonSeat, true);
        dealHoleCards();
        postBlinds();
        events.onHandStarted(this, handNumber);
        beginBettingRound(now, firstToActPreflop());
    }

    private void dealHoleCards() {
        // two rounds of one card each, the way a dealer does it. It changes nothing and it is what people
        // expect to see when the cards come out one at a time
        for (int round = 0; round < 2; round++) {
            int seat = buttonSeat;
            for (int step = 0; step < seats.length; step++) {
                seat = nextOccupied(seat, true);
                PokerPlayer player = seats[seat];
                if (player == null || !player.isInHand()) continue;
                if (round == 0) {
                    player.getHole().add(deck.draw());
                } else {
                    player.getHole().add(deck.draw());
                }
                if (seat == buttonSeat) break;
            }
        }
        for (PokerPlayer player : seats) {
            if (player != null && player.isInHand()) events.onHoleCards(this, player);
        }
    }

    private void postBlinds() {
        List<Integer> order = inHandSeatsFromButton();
        int small;
        int big;
        if (order.size() == 2) {
            // heads up the button is the small blind and acts first before the flop and last after it. It
            // reads wrong the first time and it is how the game is played. The order runs from the button,
            // so the button is the last entry and the other player is the first
            small = order.get(order.size() - 1);
            big = order.get(0);
        } else {
            // everywhere else the blinds are simply the two seats left of the button, which is what the
            // order already starts with
            small = order.get(0);
            big = order.get(1);
        }
        post(seats[small], rules.getSmallBlind(), false);
        post(seats[big], rules.getBigBlind(), true);
        currentBet = rules.getBigBlind();
        lastRaiseSize = rules.getBigBlind();
        minRaiseTo = rules.getBigBlind() * 2;
    }

    private void post(PokerPlayer player, int amount, boolean big) {
        int paid = player.commit(amount);
        events.onBlindPosted(this, player, paid, big);
    }

    /**
     * @return the seat that acts first before the flop
     */
    private int firstToActPreflop() {
        List<Integer> order = inHandSeatsFromButton();
        // heads up that is the button, which is the small blind and the last entry. Otherwise it is the
        // seat after the big blind, and with three players that wraps back round to the button
        if (order.size() == 2) return order.get(order.size() - 1);
        return order.get(2 % order.size());
    }

    /**
     * @return the seat that acts first after the flop - the first live seat left of the button
     */
    private int firstToActPostflop() {
        List<Integer> order = inHandSeatsFromButton();
        for (int seat : order) {
            PokerPlayer player = seats[seat];
            if (player != null && player.canAct()) return seat;
        }
        return -1;
    }

    /**
     * @return the seats that are in this hand, starting left of the button
     */
    private List<Integer> inHandSeatsFromButton() {
        List<Integer> order = new ArrayList<>();
        int seat = buttonSeat;
        for (int step = 0; step < seats.length; step++) {
            seat = (seat + 1) % seats.length;
            PokerPlayer player = seats[seat];
            if (player != null && player.isInHand()) order.add(seat);
        }
        // the button itself belongs at the end, because everything is measured from it
        return order;
    }

    /**
     * @param from      where to start looking
     * @param mustBeInHand whether the seat has to be in the current hand
     * @return the next occupied seat after {@code from}, wrapping around
     */
    private int nextOccupied(int from, boolean mustBeInHand) {
        int seat = from;
        for (int step = 0; step < seats.length; step++) {
            seat = (seat + 1) % seats.length;
            PokerPlayer player = seats[seat];
            if (player == null) continue;
            if (mustBeInHand && !player.isInHand()) continue;
            return seat;
        }
        return from;
    }

    /* ------------------------------------------------------------------ betting */

    /**
     * Opens a betting round.
     * <p>
     * Everybody starts as not having acted, the big blind included. That is the option: they put chips in
     * because the rules made them, not because they decided anything, so a round where everybody merely
     * called still comes back to them.
     */
    private void beginBettingRound(long now, int firstSeat) {
        for (PokerPlayer player : seats) {
            if (player == null) continue;
            player.setActed(false);
            player.setMayRaise(true);
        }
        actingSeat = firstSeat;
        if (actingSeat < 0 || seats[actingSeat] == null || !seats[actingSeat].canAct()) {
            actingSeat = nextToAct(firstSeat < 0 ? buttonSeat : firstSeat);
        }
        if (actingSeat < 0) {
            // nobody can act - everybody left is all-in, so the rest of the board is simply dealt
            runOutBoard(now);
            return;
        }
        openTurn(now);
    }

    private void openTurn(long now) {
        PokerPlayer player = getActing();
        if (player == null) {
            advance();
            return;
        }
        deadline = now + rules.getActionSeconds() * 1000L;
        events.onTurn(this, player, toCall(player), minRaiseTo(player), deadline);
        events.onStateChanged(this);
    }

    /**
     * @param from the seat that just acted
     * @return the next seat that still owes a decision, or {@code -1} when the round is over
     */
    private int nextToAct(int from) {
        int seat = from;
        for (int step = 0; step < seats.length; step++) {
            seat = (seat + 1) % seats.length;
            PokerPlayer player = seats[seat];
            if (player == null || !player.canAct()) continue;
            if (!player.hasActed() || player.getCommitted() < currentBet) return seat;
        }
        return -1;
    }

    /**
     * Takes a decision.
     *
     * @param player who is deciding
     * @param action what they decided
     * @return whether it was legal and was applied
     */
    public boolean act(PokerPlayer player, Action action) {
        return act(player, action, System.currentTimeMillis());
    }

    /**
     * Takes a decision at a given moment.
     *
     * @param player who is deciding
     * @param action what they decided
     * @param now    the current time
     * @return whether it was legal and was applied
     */
    public boolean act(PokerPlayer player, Action action, long now) {
        if (!handRunning || player == null || action == null) return false;
        if (getActing() != player) return false;
        if (!player.canAct()) return false;

        int toCall = toCall(player);
        switch (action.type()) {
            case FOLD -> {
                player.fold();
                player.setActed(true);
            }
            case CHECK -> {
                if (toCall > 0) return false;
                player.setActed(true);
            }
            case CALL -> {
                if (toCall <= 0) return false;
                player.commit(toCall);
                player.setActed(true);
            }
            case BET, RAISE, ALL_IN -> {
                if (!applyAggression(player, action)) return false;
            }
        }
        // a decision made in time clears the strikes: somebody who is back is back
        if (action.type() != ActionType.FOLD || toCall > 0) timeouts.remove(player.getId());
        events.onActionTaken(this, player, action);
        advance(now);
        return true;
    }

    /**
     * Money going in on top of what is already there.
     * <p>
     * The one subtlety is the short all-in. Somebody with less than a full raise left may still put it all
     * in - they are never forced to fold because their stack is awkward - but it does not reopen the
     * betting: whoever had already acted owes the difference and may not raise again off the back of it.
     * Without that, a player with one chip left could be used to re-open a round for somebody else.
     *
     * @return whether the raise was legal
     */
    private boolean applyAggression(PokerPlayer player, Action action) {
        int most = maxRaiseTo(player);
        int target = action.type() == ActionType.ALL_IN ? most : action.amount();
        if (target > most) return false;
        if (target <= currentBet) {
            // not a raise at all. An all-in for less than the current bet is a call for everything they
            // have, which is legal; anything else is a mistake
            if (action.type() != ActionType.ALL_IN) return false;
            player.commit(player.getChips());
            player.setActed(true);
            return true;
        }
        boolean allIn = target == most;
        if (!allIn) {
            if (!player.mayRaise()) return false;
            if (target < minRaiseTo) return false;
        }

        int raiseSize = target - currentBet;
        player.commit(target - player.getCommitted());
        player.setActed(true);

        boolean full = raiseSize >= lastRaiseSize;
        currentBet = target;
        if (full) {
            lastRaiseSize = raiseSize;
            minRaiseTo = currentBet + raiseSize;
            for (PokerPlayer other : seats) {
                if (other == null || other == player || !other.canAct()) continue;
                other.setActed(false);
                other.setMayRaise(true);
            }
        } else {
            // a short all-in: the price goes up, the round does not open again
            minRaiseTo = currentBet + lastRaiseSize;
            for (PokerPlayer other : seats) {
                if (other == null || other == player || !other.canAct()) continue;
                if (other.hasActed()) {
                    other.setActed(false);
                    other.setMayRaise(false);
                }
            }
        }
        return true;
    }

    private void advance() {
        advance(System.currentTimeMillis());
    }

    /**
     * Works out what happens after a decision: the hand is over, the street is over, or it is somebody
     * else's turn.
     */
    private void advance(long now) {
        List<PokerPlayer> contesting = contesting();
        if (contesting.size() <= 1) {
            finishHand(now, false);
            return;
        }
        int next = nextToAct(actingSeat);
        if (next >= 0) {
            actingSeat = next;
            openTurn(now);
            return;
        }
        endStreet(now);
    }

    /**
     * Closes a betting round and moves the hand on.
     */
    private void endStreet(long now) {
        for (PokerPlayer player : seats) {
            if (player != null) player.endStreet();
        }
        currentBet = 0;
        lastRaiseSize = rules.getBigBlind();
        minRaiseTo = rules.getBigBlind();
        actingSeat = -1;

        if (street == Street.RIVER) {
            finishHand(now, true);
            return;
        }
        // nobody left who can put chips in means the rest of the board is a formality, so it is turned over
        // in one go rather than asking three all-in players to check four times
        if (playersWhoCanStillAct() < 2) {
            runOutBoard(now);
            return;
        }
        dealNextStreet();
        beginBettingRound(now, firstToActPostflop());
    }

    private int playersWhoCanStillAct() {
        int count = 0;
        for (PokerPlayer player : seats) {
            if (player != null && player.canAct()) count++;
        }
        return count;
    }

    private void dealNextStreet() {
        street = street.next();
        deck.burn();
        int wanted = street.getCommunityCards();
        while (community.size() < wanted) community.add(deck.draw());
        events.onStreetDealt(this, street);
        events.onStateChanged(this);
    }

    /**
     * Turns the rest of the board over when there is nothing left to bet.
     */
    private void runOutBoard(long now) {
        while (street != Street.RIVER) {
            dealNextStreet();
        }
        finishHand(now, true);
    }

    /* ------------------------------------------------------------------ the end of a hand */

    private List<PokerPlayer> contesting() {
        List<PokerPlayer> live = new ArrayList<>();
        for (PokerPlayer player : seats) {
            if (player != null && player.isContesting()) live.add(player);
        }
        return live;
    }

    /**
     * Hands the pots over and closes the hand.
     *
     * @param showdown whether it went to a showdown, which is also what decides whether the house takes
     *                 anything: a hand nobody contested past the blinds is not raked
     */
    private void finishHand(long now, boolean showdown) {
        List<PokerPlayer> dealt = new ArrayList<>();
        for (PokerPlayer player : seats) {
            if (player != null && player.isInHand()) dealt.add(player);
        }
        lastPot = getPot();

        List<Pot> pots = Pot.build(dealt);
        boolean contested = showdown && street != Street.PREFLOP;

        if (showdown) {
            buildShowdown(dealt);
            if (!lastShowdown.isEmpty()) events.onShowdown(this, List.copyOf(lastShowdown));
        }

        for (Pot pot : pots) {
            awardPot(pot, showdown, contested);
        }

        for (PokerPlayer player : seats) {
            if (player == null) continue;
            if (player.isInHand() && player.getChips() == 0) events.onSeatBroke(this, player);
            player.endHand();
        }

        handRunning = false;
        actingSeat = -1;
        currentBet = 0;
        nextHandAt = now + (showdown ? SHOWDOWN_MILLIS : BREAK_MILLIS);
        events.onHandEnded(this);

        // whoever asked to leave while they were all-in has been waiting for exactly this moment, and they
        // wait with whatever the hand paid them
        for (PokerPlayer waiting : new ArrayList<>(leaving)) {
            int seat = seatOf(waiting);
            if (seat >= 0) release(seat, waiting);
        }
        events.onStateChanged(this);
    }

    private void buildShowdown(List<PokerPlayer> dealt) {
        lastShowdown.clear();
        for (PokerPlayer player : dealt) {
            if (!player.isContesting()) continue;
            List<Card> seven = new ArrayList<>(community);
            seven.addAll(player.getHole());
            if (seven.size() < 5) continue;
            lastShowdown.add(new ShowdownEntry(player, List.copyOf(player.getHole()),
                    HandEvaluator.evaluate(seven)));
        }
        lastShowdown.sort(Comparator.comparing(ShowdownEntry::value).reversed());
    }

    /**
     * Hands one pot to whoever won it, minus what the house keeps.
     * <p>
     * A split is a split down to the chip: what does not divide goes to the first winner left of the
     * button, which is where a live dealer puts it too.
     */
    private void awardPot(Pot pot, boolean showdown, boolean contested) {
        Set<PokerPlayer> eligible = pot.getEligible();
        if (eligible.isEmpty()) return;

        int rake = rules.getRake().rakeOf(pot.getAmount(), contested);
        int payable = pot.getAmount() - rake;

        List<PokerPlayer> winners = new ArrayList<>();
        HandValue best = null;
        if (!showdown || eligible.size() == 1) {
            winners.addAll(eligible);
        } else {
            for (ShowdownEntry entry : lastShowdown) {
                if (!eligible.contains(entry.player())) continue;
                if (best == null || entry.value().compareTo(best) > 0) {
                    best = entry.value();
                    winners.clear();
                    winners.add(entry.player());
                } else if (entry.value().compareTo(best) == 0) {
                    winners.add(entry.player());
                }
            }
            if (winners.isEmpty()) winners.addAll(eligible);
        }

        int share = payable / winners.size();
        int odd = payable - share * winners.size();
        boolean rakeReported = false;
        for (PokerPlayer winner : orderFromButton(winners)) {
            int amount = share;
            if (odd > 0) {
                amount++;
                odd--;
            }
            winner.addChips(amount);
            // the rake is a fact about the pot, not about a winner, so it is said once however many people
            // split it - otherwise a split pot reports a house cut of zero and the books stop adding up
            events.onPotAwarded(this, winner, amount, rakeReported ? 0 : rake, best);
            rakeReported = true;
        }
    }

    /**
     * @param players some of the players at the table
     * @return them in the order they sit, starting left of the button - which is the order odd chips are
     *         handed out in
     */
    private List<PokerPlayer> orderFromButton(List<PokerPlayer> players) {
        List<PokerPlayer> ordered = new ArrayList<>();
        Set<PokerPlayer> left = new LinkedHashSet<>(players);
        int seat = buttonSeat;
        for (int step = 0; step < seats.length && !left.isEmpty(); step++) {
            seat = (seat + 1) % seats.length;
            PokerPlayer player = seats[seat];
            if (player != null && left.remove(player)) ordered.add(player);
        }
        ordered.addAll(left);
        return ordered;
    }

    /**
     * One player's cards at a showdown.
     *
     * @param player who it is
     * @param hole   what they held
     * @param value  what it was worth
     */
    public record ShowdownEntry(PokerPlayer player, List<Card> hole, HandValue value) {
    }
}
