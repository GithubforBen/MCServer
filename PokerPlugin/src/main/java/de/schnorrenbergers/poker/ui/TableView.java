package de.schnorrenbergers.poker.ui;

import de.schnorrenbergers.poker.game.Card;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import de.schnorrenbergers.poker.game.Street;
import de.schnorrenbergers.poker.world.TableSpot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a table looks like.
 * <p>
 * Everything a hand does is on the felt: the community cards face up in the middle, everybody's two cards
 * lying in front of their chair - face down to the room and face up to their owner - a heap of chips for
 * every bet, a bigger one in the middle for the pot, and a head floating over every seat with the name and
 * the stack under it. The head of whoever has to decide turns faster and rises, which is what makes it
 * obvious across the room whose turn it is without anybody reading a line of chat.
 * <p>
 * The view never asks the rules anything it has not been told. It is redrawn from a table it is handed, and
 * only when something happened - a poker table that redraws itself sixty times a second is a poker table
 * that costs more than the game.
 */
public final class TableView {

    /** How high above the felt the cards float, so they are not inside it. */
    private static final double CARD_LIFT = 0.07d;
    /** How high above a chair a head floats. */
    private static final double HEAD_LIFT = 1.9d;
    /** How far the head of whoever is to act rises above the others. */
    private static final double ACTING_LIFT = 0.25d;
    /** How far apart the two hole cards of one seat lie. */
    private static final double HOLE_SPREAD = 0.28d;
    /** How far apart the five community cards lie. */
    private static final double COMMUNITY_SPREAD = 0.62d;

    private final Plugin plugin;
    private final TableSpot spot;
    private final int seats;
    /** The casino, remembered when the view is put up so nothing has to go looking for it again. */
    private org.bukkit.World world;

    private final List<TextDisplay> community = new ArrayList<>();
    private final Map<Integer, TextDisplay[]> hole = new HashMap<>();
    private final Map<Integer, ItemDisplay> heads = new HashMap<>();
    private final Map<Integer, TextDisplay> labels = new HashMap<>();
    private final Map<Integer, ChipStack> bets = new HashMap<>();
    private ChipStack pot;
    private TextDisplay potLabel;
    private BlockDisplay button;

    /** Turns a little further every animation tick, so the heads are never quite still. */
    private float spin;

    public TableView(Plugin plugin, TableSpot spot, int seats) {
        this.plugin = plugin;
        this.spot = spot;
        this.seats = seats;
    }

    /* ------------------------------------------------------------------ setting up */

    /**
     * Puts everything that is always there into the world: the five slots for the community cards, the
     * pot, and a head over every chair.
     *
     * @param world the casino
     */
    public void spawn(org.bukkit.World world) {
        despawn();
        this.world = world;
        Location centre = spot.centre(world).add(0, CARD_LIFT, 0);

        for (int i = 0; i < 5; i++) {
            Location at = centre.clone().add((i - 2) * COMMUNITY_SPREAD, 0, 0);
            at.setYaw(0f);
            TextDisplay card = CardArt.back(at);
            if (card != null) {
                card.setViewRange(0.6f);
                community.add(card);
            }
        }
        // the community cards start hidden: an empty table should look like an empty table, not like five
        // cards nobody has been dealt
        for (TextDisplay card : community) card.text(Component.empty());
        setCommunityVisible(0);

        pot = new ChipStack(centre.clone().add(0, 0, 0.9d));
        potLabel = Displays.floatingText(spot.centre(world).add(0, 1.15d, 0), 0.55f);

        for (int seat = 0; seat < seats; seat++) {
            Location chair = spot.seat(world, seat, spot.getCentreY());
            Location above = chair.clone().add(0, HEAD_LIFT - 1, 0);
            ItemDisplay head = Displays.item(above, new ItemStack(Material.PLAYER_HEAD), 0.55f);
            if (head != null) heads.put(seat, head);
            TextDisplay label = Displays.floatingText(above.clone().add(0, 0.55d, 0), 0.42f);
            if (label != null) labels.put(seat, label);
            bets.put(seat, new ChipStack(spot.betSpot(world, seat).add(0, CARD_LIFT, 0)));
        }
    }

    /**
     * Takes everything off the table. Called when the casino closes, and whenever the view is rebuilt.
     */
    public void despawn() {
        Displays.removeAll(community);
        for (TextDisplay[] pair : hole.values()) {
            for (TextDisplay card : pair) {
                if (card != null && card.isValid()) card.remove();
            }
        }
        hole.clear();
        Displays.removeAll(new ArrayList<>(heads.values()));
        heads.clear();
        Displays.removeAll(new ArrayList<>(labels.values()));
        labels.clear();
        for (ChipStack stack : bets.values()) stack.clear();
        bets.clear();
        if (pot != null) pot.clear();
        if (potLabel != null && potLabel.isValid()) potLabel.remove();
        potLabel = null;
        if (button != null && button.isValid()) button.remove();
        button = null;
    }

    /* ------------------------------------------------------------------ drawing */

    /**
     * Redraws everything that can have changed. Called after anything the rules report.
     *
     * @param table the table as it now stands
     */
    public void redraw(PokerTable table) {
        if (world == null) return;
        drawCommunity(table);
        drawPot(table);
        drawButton(table);
        for (int seat = 0; seat < seats; seat++) {
            drawSeat(table, seat);
        }
    }

    private void drawCommunity(PokerTable table) {
        List<Card> cards = table.getCommunity();
        for (int i = 0; i < community.size(); i++) {
            TextDisplay display = community.get(i);
            if (!display.isValid()) continue;
            if (i < cards.size()) {
                CardArt.turn(display, cards.get(i));
            } else {
                display.text(Component.empty());
                display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            }
        }
        setCommunityVisible(cards.size());
    }

    /**
     * A slot with no card in it shows nothing at all rather than an empty white rectangle.
     */
    private void setCommunityVisible(int howMany) {
        for (int i = 0; i < community.size(); i++) {
            TextDisplay display = community.get(i);
            if (!display.isValid()) continue;
            if (i >= howMany) {
                display.text(Component.empty());
                display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            }
        }
    }

    private void drawPot(PokerTable table) {
        int inTheMiddle = table.isHandRunning() ? table.getPot() - committedThisStreet(table)
                : table.getLastPot();
        if (pot != null) pot.set(Math.max(0, inTheMiddle));
        if (potLabel == null || !potLabel.isValid()) return;

        if (!table.isHandRunning()) {
            potLabel.text(Component.text("Nächste Hand gleich", NamedTextColor.GRAY));
            return;
        }
        Component text = Component.text("Pot " + Math.max(0, inTheMiddle), NamedTextColor.GOLD)
                .append(Component.newline())
                .append(Component.text(table.getStreet().getTitle(), NamedTextColor.GRAY));
        if (table.getCurrentBet() > 0) {
            text = text.append(Component.newline())
                    .append(Component.text("Einsatz " + table.getCurrentBet(), NamedTextColor.YELLOW));
        }
        potLabel.text(text);
    }

    /**
     * @return what is still in front of the players rather than in the middle, so a bet is not drawn twice
     */
    private int committedThisStreet(PokerTable table) {
        int committed = 0;
        for (PokerPlayer player : table.getPlayers()) committed += player.getCommitted();
        return committed;
    }

    private void drawButton(PokerTable table) {
        int seat = table.getButtonSeat();
        if (world == null || seat < 0 || table.seatAt(seat) == null) {
            if (button != null && button.isValid()) button.remove();
            button = null;
            return;
        }
        Location at = spot.cardSpot(world, seat, spot.getSeatRadius() * 0.18d).add(0, CARD_LIFT, 0);
        if (button == null || !button.isValid()) {
            button = Displays.disc(at, Material.WHITE_CONCRETE, 0.22f, 0.05f);
            return;
        }
        button.teleport(at);
    }

    private void drawSeat(PokerTable table, int seat) {
        PokerPlayer player = table.seatAt(seat);
        ItemDisplay head = heads.get(seat);
        TextDisplay label = labels.get(seat);
        ChipStack bet = bets.get(seat);

        if (bet != null) bet.set(player == null ? 0 : player.getCommitted());

        if (head != null && head.isValid()) {
            head.setItemStack(player == null ? new ItemStack(Material.AIR) : headOf(player));
        }
        if (label == null || !label.isValid()) return;

        if (player == null) {
            label.text(Component.text("Frei", NamedTextColor.DARK_GRAY)
                    .append(Component.newline())
                    .append(Component.text("Rechtsklick zum Setzen", NamedTextColor.DARK_GRAY)));
            drawHoleCards(table, seat, null);
            return;
        }

        TextColor colour = player.isFolded() ? NamedTextColor.DARK_GRAY
                : table.getActingSeat() == seat ? NamedTextColor.YELLOW : NamedTextColor.WHITE;
        Component text = Component.text(player.getName(), colour)
                .append(Component.newline())
                .append(Component.text(player.getChips() + " Chips", NamedTextColor.GRAY));
        if (player.isAllIn()) {
            text = text.append(Component.newline())
                    .append(Component.text("ALL-IN", NamedTextColor.RED));
        } else if (player.isFolded()) {
            text = text.append(Component.newline())
                    .append(Component.text("raus", NamedTextColor.DARK_GRAY));
        } else if (player.isSittingOut()) {
            text = text.append(Component.newline())
                    .append(Component.text("Pause", NamedTextColor.DARK_GRAY));
        } else if (player.getCommitted() > 0) {
            text = text.append(Component.newline())
                    .append(Component.text("Einsatz " + player.getCommitted(), NamedTextColor.YELLOW));
        }
        label.text(text);
        drawHoleCards(table, seat, player);
    }

    /**
     * @param player the player at the seat
     * @return their face, or a skull for a bot so that nobody has to guess which of the eight are people
     */
    private ItemStack headOf(PokerPlayer player) {
        if (player.isBot()) return new ItemStack(Material.SKELETON_SKULL);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(player.getId()));
            head.setItemMeta(skull);
        }
        return head;
    }

    /**
     * Lays somebody's two cards in front of their chair.
     * <p>
     * Two displays per seat, both face down to the room. The one person they belong to is shown the face
     * instead of the back, which is done by hiding the entity from everybody else - the cards are in the
     * same place for everybody, and only what is written on them differs.
     */
    private void drawHoleCards(PokerTable table, int seat, PokerPlayer player) {
        if (player == null || !player.isInHand() || player.getHole().size() < 2) {
            TextDisplay[] pair = hole.remove(seat);
            if (pair != null) {
                for (TextDisplay card : pair) {
                    if (card != null && card.isValid()) card.remove();
                }
            }
            return;
        }

        if (world == null) return;

        TextDisplay[] pair = hole.get(seat);
        if (pair == null) {
            pair = new TextDisplay[2];
            Location base = spot.cardSpot(world, seat, 1.1d).add(0, CARD_LIFT, 0);
            double yaw = Math.toRadians(base.getYaw());
            for (int i = 0; i < 2; i++) {
                double offset = (i - 0.5d) * HOLE_SPREAD;
                Location at = base.clone().add(Math.cos(yaw) * offset, 0, -Math.sin(yaw) * offset);
                at.setYaw(base.getYaw());
                pair[i] = CardArt.back(at);
                if (pair[i] != null) pair[i].setViewRange(0.5f);
            }
            hole.put(seat, pair);
        }

        boolean shown = table.getStreet() == Street.SHOWDOWN || !table.isHandRunning();
        Player owner = player.isBot() ? null : Bukkit.getPlayer(player.getId());
        for (int i = 0; i < 2; i++) {
            if (pair[i] == null || !pair[i].isValid()) continue;
            if (shown && !player.isFolded()) {
                CardArt.turn(pair[i], player.getHole().get(i));
                Displays.showToAll(plugin, pair[i]);
            } else if (owner != null) {
                CardArt.turn(pair[i], player.getHole().get(i));
                Displays.showOnlyTo(plugin, pair[i], owner);
            } else {
                CardArt.turn(pair[i], null);
                Displays.showToAll(plugin, pair[i]);
            }
        }
    }

    /**
     * Turns everybody's cards face up, which is what a showdown is.
     *
     * @param table the table
     */
    public void reveal(PokerTable table) {
        for (Map.Entry<Integer, TextDisplay[]> entry : hole.entrySet()) {
            PokerPlayer player = table.seatAt(entry.getKey());
            if (player == null || player.isFolded() || player.getHole().size() < 2) continue;
            for (int i = 0; i < 2; i++) {
                TextDisplay card = entry.getValue()[i];
                if (card == null || !card.isValid()) continue;
                CardArt.turn(card, player.getHole().get(i));
                Displays.showToAll(plugin, card);
            }
        }
    }

    /* ------------------------------------------------------------------ movement */

    /**
     * The small amount of life the table has between decisions: every head turns slowly, and the head of
     * whoever has to decide turns faster and floats a little higher.
     * <p>
     * Called a few times a second. It moves entities and touches no text, which is what keeps it cheap.
     *
     * @param table the table
     */
    public void animate(PokerTable table) {
        spin += 2.5f;
        if (spin >= 360f) spin -= 360f;
        if (world == null) return;
        for (Map.Entry<Integer, ItemDisplay> entry : heads.entrySet()) {
            ItemDisplay head = entry.getValue();
            if (head == null || !head.isValid()) continue;
            PokerPlayer player = table.seatAt(entry.getKey());
            if (player == null) continue;
            boolean acting = table.getActingSeat() == entry.getKey();
            float yaw = acting ? spin * 3f : spin;
            head.setRotation(yaw % 360f, 0f);
            double bob = acting
                    ? ACTING_LIFT + Math.sin(Math.toRadians(spin * 6)) * 0.06d
                    : Math.sin(Math.toRadians(spin * 2 + entry.getKey() * 40)) * 0.03d;
            Location at = head.getLocation();
            Location wanted = baseHeadLocation(entry.getKey(), world).add(0, bob, 0);
            if (at.distanceSquared(wanted) > 0.0001d) head.teleport(wanted);
        }
    }

    private Location baseHeadLocation(int seat, org.bukkit.World world) {
        return spot.seat(world, seat, spot.getCentreY()).add(0, HEAD_LIFT - 1, 0);
    }

    public TableSpot getSpot() {
        return spot;
    }
}
