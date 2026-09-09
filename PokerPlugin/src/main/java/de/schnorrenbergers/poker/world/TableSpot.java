package de.schnorrenbergers.poker.world;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Where one table stands and where its chairs are.
 * <p>
 * Kept as plain numbers rather than as blocks in the world, because the world is meant to be edited. Once
 * the casino has been built the map belongs to whoever builds on it - they can pull a table apart, move it
 * or replace it with something nicer - and what has to survive that is the answer to "where do people sit
 * and where do the cards lie", which is this.
 */
public final class TableSpot {

    private final int index;
    private final double centreX;
    private final double centreY;
    private final double centreZ;
    /** How far the chairs stand from the middle. */
    private final double seatRadius;
    private final List<double[]> seats = new ArrayList<>();

    /**
     * @param index      which table this is
     * @param centreX    the middle of the table
     * @param centreY    the height the felt is at, which is where cards and chips are drawn
     * @param centreZ    the middle of the table
     * @param seatRadius how far out the chairs are
     */
    public TableSpot(int index, double centreX, double centreY, double centreZ, double seatRadius) {
        this.index = index;
        this.centreX = centreX;
        this.centreY = centreY;
        this.centreZ = centreZ;
        this.seatRadius = seatRadius;
    }

    public int getIndex() {
        return index;
    }

    public double getSeatRadius() {
        return seatRadius;
    }

    /**
     * @param world the world it stands in
     * @return the middle of the felt
     */
    public Location centre(World world) {
        return new Location(world, centreX, centreY, centreZ);
    }

    /**
     * Lays the chairs out evenly around the table.
     * <p>
     * The first seat sits due south and they go round clockwise, which is only a convention - what matters
     * is that it is the same convention every time, because seat three has to be the same chair for the
     * rules, for the cards on the felt and for the head floating over it.
     *
     * @param count how many seats the table has
     */
    public void layOutSeats(int count) {
        seats.clear();
        for (int seat = 0; seat < count; seat++) {
            double angle = 2 * Math.PI * seat / count;
            seats.add(new double[]{
                    centreX + Math.sin(angle) * seatRadius,
                    centreZ + Math.cos(angle) * seatRadius,
                    angle});
        }
    }

    public int getSeatCount() {
        return seats.size();
    }

    /**
     * @param world the world
     * @param seat  which chair
     * @param y     the height of the floor the chair stands on
     * @return where the chair is, looking at the middle of the table
     */
    public Location seat(World world, int seat, double y) {
        double[] spot = seats.get(Math.floorMod(seat, seats.size()));
        Location location = new Location(world, spot[0] + 0.5, y, spot[1] + 0.5);
        // facing the middle, which is the direction the chair and its sitter both look
        location.setYaw((float) (Math.toDegrees(spot[2]) + 180));
        return location;
    }

    /**
     * Where the cards of one seat lie on the felt: in front of the chair, a little in from the edge.
     *
     * @param world the world
     * @param seat  which chair
     * @param inset how far in from the seat, in blocks
     * @return the spot on the felt
     */
    public Location cardSpot(World world, int seat, double inset) {
        double[] spot = seats.get(Math.floorMod(seat, seats.size()));
        double angle = spot[2];
        double radius = Math.max(0.8d, seatRadius - inset);
        Location location = new Location(world,
                centreX + Math.sin(angle) * radius,
                centreY,
                centreZ + Math.cos(angle) * radius);
        location.setYaw((float) Math.toDegrees(angle));
        return location;
    }

    /**
     * Where the chips somebody has bet sit: between their cards and the middle, so the table shows at a
     * glance who has put what in.
     *
     * @param world the world
     * @param seat  which chair
     * @return the spot on the felt
     */
    public Location betSpot(World world, int seat) {
        return cardSpot(world, seat, seatRadius * 0.55d);
    }

    public double getCentreX() {
        return centreX;
    }

    public double getCentreY() {
        return centreY;
    }

    public double getCentreZ() {
        return centreZ;
    }

    public List<double[]> getSeats() {
        return seats;
    }
}
