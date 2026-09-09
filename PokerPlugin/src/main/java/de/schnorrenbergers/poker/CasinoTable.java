package de.schnorrenbergers.poker;

import de.hems.types.event.PokerEventSettings;
import de.schnorrenbergers.poker.bank.Bank;
import de.schnorrenbergers.poker.bot.BotBrain;
import de.schnorrenbergers.poker.bot.BotVibe;
import de.schnorrenbergers.poker.game.Action;
import de.schnorrenbergers.poker.game.Card;
import de.schnorrenbergers.poker.game.HandValue;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import de.schnorrenbergers.poker.game.RakePolicy;
import de.schnorrenbergers.poker.game.Street;
import de.schnorrenbergers.poker.game.TableEvents;
import de.schnorrenbergers.poker.game.TableRules;
import de.schnorrenbergers.poker.ui.CardArt;
import de.schnorrenbergers.poker.ui.TableView;
import de.schnorrenbergers.poker.ui.TurnControls;
import de.schnorrenbergers.poker.world.TableSpot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * One table in the world: the rules, the chairs people really sit on, the money, and what everybody sees.
 * <p>
 * This is the only place the three halves meet. {@link PokerTable} knows the rules and nothing else,
 * {@link TableView} draws and knows nothing, {@link Bank} moves bits and knows nothing about poker. What is
 * here is the wiring: somebody clicks a chair, bits become chips, the rules deal, the view redraws, and
 * when they stand up the chips become bits again.
 */
public final class CasinoTable implements TableEvents {

    /** How long a bot waits before it has decided, so it does not answer instantly. */
    private static final long BOT_MIN_THINK_MS = 500L;
    /** How high above the chair somebody sitting ends up. */
    private static final double SIT_HEIGHT = 0.35d;

    private final Plugin plugin;
    private final PokerEventSettings settings;
    private final TableSpot spot;
    private final PokerTable rules;
    private final TableView view;
    private final World world;

    /** The invisible stand somebody is really riding when they "sit on a chair". */
    private final Map<Integer, ArmorStand> chairs = new HashMap<>();
    /** The brain behind every bot at this table. */
    private final Map<PokerPlayer, BotBrain> brains = new LinkedHashMap<>();
    /** When a bot is allowed to answer, so it looks like it thought about it. */
    private final Map<PokerPlayer, Long> botThinkingUntil = new HashMap<>();
    /** Who owns which bot, so its chips go home to the right account. */
    private final Map<PokerPlayer, UUID> botOwners = new HashMap<>();
    private final Random random = new Random();

    public CasinoTable(Plugin plugin, World world, TableSpot spot, PokerEventSettings settings) {
        this.plugin = plugin;
        this.world = world;
        this.spot = spot;
        this.settings = settings;
        RakePolicy rake = settings::rakeOf;
        this.rules = new PokerTable(spot.getIndex(), settings.getSeats(),
                new TableRules(settings.getSmallBlind(), Casino.ACTION_SECONDS,
                        !settings.getFormat().allowsCashOut(), rake), this);
        this.view = new TableView(plugin, spot, settings.getSeats());
        this.view.spawn(world);
        this.view.redraw(rules);
    }

    /* ------------------------------------------------------------------ the clock */

    /**
     * Drives the table. Called every tick group from {@link Casino}.
     *
     * @param now the current time
     */
    public void tick(long now) {
        rules.tick(now);
        driveBots(now);
        view.animate(rules);
        showClock();
    }

    /**
     * Lets a bot answer once it has thought for long enough.
     */
    private void driveBots(long now) {
        PokerPlayer acting = rules.getActing();
        if (acting == null || !acting.isBot()) return;
        Long until = botThinkingUntil.get(acting);
        if (until == null || now < until) return;
        botThinkingUntil.remove(acting);
        BotBrain brain = brains.get(acting);
        if (brain == null) {
            rules.act(acting, rules.toCall(acting) > 0 ? Action.fold() : Action.check(), now);
            return;
        }
        Action decision = brain.decide(rules, acting);
        if (!rules.act(acting, decision, now)) {
            // a decision the rules refuse is a bug in the brain, and the table must not be left standing
            // there waiting for it. The safe move is the one that costs nothing
            plugin.getLogger().warning("A bot tried something illegal (" + decision.type()
                    + " " + decision.amount() + ") and was made to "
                    + (rules.toCall(acting) > 0 ? "fold" : "check") + ".");
            rules.act(acting, rules.toCall(acting) > 0 ? Action.fold() : Action.check(), now);
        }
    }

    /**
     * Shows whoever has to decide how long they still have, on their action bar.
     */
    private void showClock() {
        PokerPlayer acting = rules.getActing();
        if (acting == null || acting.isBot()) return;
        Player player = Bukkit.getPlayer(acting.getId());
        if (player == null) return;
        long left = Math.max(0, rules.getDeadline() - System.currentTimeMillis());
        int seconds = (int) Math.ceil(left / 1000d);
        NamedTextColor colour = seconds <= 5 ? NamedTextColor.RED
                : seconds <= 10 ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        player.sendActionBar(Component.text("Du bist dran - " + seconds + "s", colour));
    }

    /* ------------------------------------------------------------------ sitting down */

    public PokerTable getRules() {
        return rules;
    }

    public TableSpot getSpot() {
        return spot;
    }

    public int getIndex() {
        return spot.getIndex();
    }

    public boolean hasRoom() {
        return rules.getFreeSeats() > 0;
    }

    /**
     * @param seat which chair
     * @return whether anybody is in it
     */
    public boolean isTaken(int seat) {
        return rules.seatAt(seat) != null;
    }

    /**
     * @param location somewhere in the world
     * @return the chair nearest to it, or {@code -1} if it is nowhere near this table
     */
    public int seatNear(Location location) {
        int nearest = -1;
        double best = 2.4d * 2.4d;
        for (int seat = 0; seat < settings.getSeats(); seat++) {
            double distance = spot.seat(world, seat, spot.getCentreY()).distanceSquared(location);
            if (distance < best) {
                best = distance;
                nearest = seat;
            }
        }
        return nearest;
    }

    /**
     * Sits somebody down, once their bits have really become chips.
     *
     * @param player who is sitting down
     * @param seat   which chair, or {@code -1} for the first free one
     * @param chips  what they bought in for
     * @return whether they got a seat
     */
    public boolean seat(Player player, int seat, int chips) {
        PokerPlayer sitting = new PokerPlayer(player.getUniqueId(), player.getName(),
                player.getUniqueId(), false, chips);
        int got = rules.sitDown(sitting, seat);
        if (got < 0) return false;
        placeOnChair(player, got);
        reportStacks();
        view.redraw(rules);
        player.sendMessage(Component.text("Du sitzt an Tisch " + (spot.getIndex() + 1)
                + " mit " + chips + " Chips.", NamedTextColor.GREEN));
        TurnControls.giveIdle(player);
        return true;
    }

    /**
     * Puts a bot into a seat. Its chips were paid for by whoever asked for it.
     *
     * @param owner     who pays and who gets whatever is left
     * @param ownerName their name, for the record
     * @param chips     what it plays with
     * @return whether it found a seat
     */
    public boolean seatBot(UUID owner, String ownerName, int chips) {
        String name = "Bot " + (char) ('A' + random.nextInt(26)) + random.nextInt(90, 100);
        PokerPlayer bot = new PokerPlayer(UUID.randomUUID(), name, owner, true, chips);
        int seat = rules.sitDown(bot, -1);
        if (seat < 0) return false;
        // drawn against the bots already here rather than on its own: six independent draws land close
        // together more often than people expect, and six bots that play alike are one bot
        List<BotVibe> here = new ArrayList<>();
        for (BotBrain existing : brains.values()) here.add(existing.getVibe());
        BotBrain brain = new BotBrain(random, BotVibe.unlike(random, here));
        brains.put(bot, brain);
        botOwners.put(bot, owner);
        plugin.getLogger().info("A bot sat down at table " + (spot.getIndex() + 1)
                + " for " + nameOfOwner(bot) + " (" + brain.getVibe() + ")");
        reportStacks();
        view.redraw(rules);
        return true;
    }

    /**
     * @param owner an account
     * @return how many bots that account has at this table
     */
    public int botsOf(UUID owner) {
        int count = 0;
        for (Map.Entry<PokerPlayer, UUID> entry : botOwners.entrySet()) {
            if (entry.getValue().equals(owner) && rules.seatOf(entry.getKey()) >= 0) count++;
        }
        return count;
    }

    /**
     * Takes somebody off the table and turns their chips back into bits.
     *
     * @param playerId who is leaving
     * @return what to tell them
     */
    public String leave(UUID playerId) {
        PokerPlayer player = rules.find(playerId);
        if (player == null) return "Du sitzt hier nicht.";
        boolean gone = rules.standUp(player);
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            unsit(online);
            TurnControls.clear(online);
        }
        view.redraw(rules);
        return gone
                ? "Du stehst auf - deine Chips sind wieder Bits."
                : "Du bist noch in der Hand. Sobald sie vorbei ist, stehst du auf.";
    }

    /**
     * Takes every bot of one owner off the table.
     *
     * @param owner whose bots
     * @return how many went
     */
    public int removeBotsOf(UUID owner) {
        int removed = 0;
        for (PokerPlayer bot : new ArrayList<>(botOwners.keySet())) {
            if (!owner.equals(botOwners.get(bot))) continue;
            if (rules.seatOf(bot) < 0) continue;
            rules.standUp(bot);
            removed++;
        }
        view.redraw(rules);
        return removed;
    }

    /**
     * Puts somebody on the chair itself - an invisible stand they ride, because there is no sitting in the
     * game and the nearest thing is being a passenger.
     */
    private void placeOnChair(Player player, int seat) {
        Location chair = spot.seat(world, seat, spot.getCentreY() - 1 + SIT_HEIGHT);
        ArmorStand stand = chairs.get(seat);
        if (stand == null || !stand.isValid()) {
            stand = world.spawn(chair, ArmorStand.class, entity -> {
                entity.setVisible(false);
                entity.setGravity(false);
                entity.setInvulnerable(true);
                entity.setMarker(false);
                entity.setSilent(true);
                entity.setPersistent(false);
            });
            chairs.put(seat, stand);
        }
        stand.addPassenger(player);
    }

    /**
     * Gets somebody off their chair and puts the chair away.
     * <p>
     * The chair has to go, not just the rider: a stand nobody can see, left standing in the middle of a
     * room, is a thing people walk into and cannot explain.
     */
    private void unsit(Player player) {
        player.leaveVehicle();
        for (Map.Entry<Integer, ArmorStand> entry : new ArrayList<>(chairs.entrySet())) {
            ArmorStand stand = entry.getValue();
            if (stand == null || !stand.isValid()) {
                chairs.remove(entry.getKey());
                continue;
            }
            if (!stand.getPassengers().isEmpty()) continue;
            stand.remove();
            chairs.remove(entry.getKey());
        }
    }

    /* ------------------------------------------------------------------ what the rules report */

    @Override
    public void onHandStarted(PokerTable table, int handNumber) {
        botThinkingUntil.clear();
        // who was dealt in has to be written down now: by the time the hand is settled the rules have
        // already cleared the seats, and a record taken afterwards would be a record of nobody
        lastDealt.clear();
        lastWinners.clear();
        stackAtHandStart.clear();
        for (PokerPlayer player : table.getPlayers()) {
            if (!player.isInHand()) continue;
            lastDealt.add(player);
            stackAtHandStart.put(player, player.getChips());
        }
        view.redraw(table);
        for (PokerPlayer player : table.getPlayers()) {
            if (player.isBot() || !player.isInHand()) continue;
            Player online = Bukkit.getPlayer(player.getId());
            if (online != null) {
                online.playSound(online.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.3f);
            }
        }
    }

    @Override
    public void onHoleCards(PokerTable table, PokerPlayer player) {
        if (player.isBot()) return;
        Player online = Bukkit.getPlayer(player.getId());
        if (online == null) return;
        Component cards = Component.empty();
        for (Card card : player.getHole()) {
            cards = cards.append(CardArt.inChat(card)).append(Component.space());
        }
        online.sendMessage(Component.text("Deine Karten: ", NamedTextColor.GRAY).append(cards));
    }

    @Override
    public void onBlindPosted(PokerTable table, PokerPlayer player, int amount, boolean big) {
        say(Component.text(player.getName() + " setzt " + (big ? "Big" : "Small") + " Blind "
                + amount, NamedTextColor.DARK_GRAY));
    }

    @Override
    public void onActionTaken(PokerTable table, PokerPlayer player, Action action) {
        String what = switch (action.type()) {
            case FOLD -> "passt";
            case CHECK -> "schiebt";
            case CALL -> "geht mit";
            case BET -> "setzt " + action.amount();
            case RAISE -> "erhöht auf " + action.amount();
            case ALL_IN -> "geht ALL-IN";
        };
        say(Component.text(player.getName() + " " + what,
                action.type().isAggressive() ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
        sound(action.type().isAggressive() ? Sound.BLOCK_NOTE_BLOCK_BELL : Sound.BLOCK_WOODEN_BUTTON_CLICK_ON);
        view.redraw(table);
    }

    @Override
    public void onTurn(PokerTable table, PokerPlayer player, int toCall, int minRaiseTo, long deadline) {
        view.redraw(table);
        if (player.isBot()) {
            BotBrain brain = brains.get(player);
            long think = brain == null ? BOT_MIN_THINK_MS : brain.thinkingTime();
            botThinkingUntil.put(player, System.currentTimeMillis() + Math.max(BOT_MIN_THINK_MS, think));
            return;
        }
        Player online = Bukkit.getPlayer(player.getId());
        if (online == null) return;
        TurnControls.give(online, table, player);
        online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.6f);
    }

    @Override
    public void onStreetDealt(PokerTable table, Street street) {
        say(Component.text("── " + street.getTitle() + " ──", NamedTextColor.AQUA));
        sound(Sound.ITEM_BOOK_PAGE_TURN);
        view.redraw(table);
    }

    @Override
    public void onShowdown(PokerTable table, List<PokerTable.ShowdownEntry> entries) {
        view.reveal(table);
        for (PokerTable.ShowdownEntry entry : entries) {
            Component cards = Component.empty();
            for (Card card : entry.hole()) {
                cards = cards.append(CardArt.inChat(card)).append(Component.space());
            }
            say(Component.text(entry.player().getName() + ": ", NamedTextColor.WHITE)
                    .append(cards)
                    .append(Component.text("- " + entry.value().describe(), NamedTextColor.GRAY)));
        }
    }

    @Override
    public void onPotAwarded(PokerTable table, PokerPlayer winner, int amount, int rake, HandValue with) {
        Component message = Component.text(winner.getName() + " gewinnt " + amount + " Chips",
                NamedTextColor.GOLD);
        if (with != null) {
            message = message.append(Component.text(" mit " + with.describe(), NamedTextColor.GRAY));
        }
        if (rake > 0) {
            message = message.append(Component.text(" (Haus " + rake + ")", NamedTextColor.DARK_GRAY));
        }
        say(message);
        sound(Sound.ENTITY_PLAYER_LEVELUP);
        if (!lastWinners.contains(winner)) lastWinners.add(winner);
    }

    @Override
    public void onHandEnded(PokerTable table) {
        // the numbers the ranking is built from: who played, who won and how big it was. Only for people -
        // a bot's money belongs to its owner, its hands do not
        int pot = table.getLastPot();
        for (PokerPlayer player : lastDealt) {
            if (player.isBot()) {
                // a bot's temperament moves with what the evening does to it: a hand that cost it a
                // quarter of its stack pushes it, and the push fades over the next few hands
                BotBrain brain = brains.get(player);
                if (brain != null) {
                    brain.afterHand(lastWinners.contains(player),
                            stackAtHandStart.getOrDefault(player, player.getChips()), player.getChips());
                }
                continue;
            }
            Bank.recordHand(player.getAccount(), player.getName(), lastWinners.contains(player), pot);
        }
        lastDealt.clear();
        lastWinners.clear();
        stackAtHandStart.clear();
        reportStacks();
        view.redraw(table);
    }

    @Override
    public void onSeatBroke(PokerTable table, PokerPlayer player) {
        Tournament tournament = Casino.getTournament();
        if (tournament != null) {
            // out of chips in a tournament is out of the tournament. There is nothing to buy back in with
            // and the place is recorded now, while it is still known
            tournament.knockOut(player);
            return;
        }
        if (player.isBot()) return;
        Player online = Bukkit.getPlayer(player.getId());
        if (online == null) return;
        online.sendMessage(Component.text("Deine Chips sind alle. Mit /poker nachkaufen "
                + "kaufst du nach, mit /poker aufstehen gehst du.", NamedTextColor.YELLOW));
    }

    @Override
    public void onPlayerLeft(PokerTable table, PokerPlayer player, int chips) {
        UUID account = player.getAccount();
        String name = player.isBot() ? nameOfOwner(player) : player.getName();
        Tournament tournament = Casino.getTournament();
        if (tournament != null) {
            // tournament chips are a position in a race, not money. Standing up with a stack wins nothing
            // and cashing it out would hand somebody bits nobody paid in
            if (chips > 0) tournament.knockOut(player);
            Player leaving = Bukkit.getPlayer(player.getId());
            if (leaving != null) {
                unsit(leaving);
                TurnControls.clear(leaving);
            }
            view.redraw(table);
            return;
        }
        if (chips > 0) {
            Bank.cashOut(account, name, chips,
                    player.isBot() ? "Poker: Bot abgeräumt" : "Poker: Chips ausgezahlt");
        }
        if (player.isBot()) {
            brains.remove(player);
            botOwners.remove(player);
            Player owner = Bukkit.getPlayer(account);
            if (owner != null) {
                owner.sendMessage(Component.text(player.getName() + " ist weg - "
                        + chips + " Bits zurück.", NamedTextColor.GRAY));
            }
        } else {
            Player online = Bukkit.getPlayer(player.getId());
            if (online != null) {
                unsit(online);
                TurnControls.clear(online);
                online.sendMessage(Component.text("Du hast " + chips + " Bits mitgenommen.",
                        NamedTextColor.GREEN));
            }
        }
        reportStacks();
        view.redraw(table);
    }

    @Override
    public void onStateChanged(PokerTable table) {
        view.redraw(table);
    }

    /** Who was dealt into the hand that is being settled, for the record afterwards. */
    private final List<PokerPlayer> lastDealt = new ArrayList<>();
    /** And who won something out of it. */
    private final List<PokerPlayer> lastWinners = new ArrayList<>();
    /** What everybody had when the hand was dealt, so a bot can be told what it just cost them. */
    private final Map<PokerPlayer, Integer> stackAtHandStart = new HashMap<>();

    /**
     * Tells the launcher what everybody has in front of them.
     * <p>
     * This is the safety net. If this server dies right now, these are the numbers out of which the money
     * is handed back - so they are sent after every hand and every time somebody sits down or stands up,
     * not on a timer.
     */
    private void reportStacks() {
        Map<UUID, Integer> byAccount = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();
        for (PokerPlayer player : rules.getPlayers()) {
            byAccount.merge(player.getAccount(), player.getChips(), Integer::sum);
            names.putIfAbsent(player.getAccount(),
                    player.isBot() ? nameOfOwner(player) : player.getName());
        }
        Casino.reportStacks(spot.getIndex(), byAccount, names);
    }

    private String nameOfOwner(PokerPlayer bot) {
        Player owner = Bukkit.getPlayer(bot.getAccount());
        return owner == null ? bot.getAccount().toString() : owner.getName();
    }

    /**
     * @return everybody sitting here who is a person and online
     */
    public List<Player> watchers() {
        List<Player> nearby = new ArrayList<>();
        Location centre = spot.centre(world);
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(centre) <= 20 * 20) nearby.add(player);
        }
        return nearby;
    }

    private void say(Component message) {
        for (Player player : watchers()) {
            player.sendMessage(Component.text("[T" + (spot.getIndex() + 1) + "] ", NamedTextColor.DARK_GRAY)
                    .append(message));
        }
    }

    private void sound(Sound sound) {
        Location centre = spot.centre(world);
        world.playSound(centre, sound, 0.6f, 1f);
    }

    /**
     * Takes the table down: everybody is paid out and everything drawn is removed.
     */
    public void close() {
        for (PokerPlayer player : new ArrayList<>(rules.getPlayers())) {
            rules.standUp(player);
        }
        for (ArmorStand stand : chairs.values()) {
            if (stand != null && stand.isValid()) stand.remove();
        }
        chairs.clear();
        view.despawn();
    }

    /**
     * @param playerId somebody
     * @return their seat at this table, or {@code null}
     */
    public @Nullable PokerPlayer find(UUID playerId) {
        return rules.find(playerId);
    }
}
