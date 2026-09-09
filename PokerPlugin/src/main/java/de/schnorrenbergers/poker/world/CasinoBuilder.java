package de.schnorrenbergers.poker.world;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Stairs;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the casino, once.
 * <p>
 * <b>Once</b> is the whole point. Generated architecture looks generated, and a room that is rebuilt on
 * every start is a room nobody can improve. So this runs when there is no casino yet, writes down where it
 * put everything in {@link CasinoLayout}, and is never asked again - from then on the world is a world like
 * any other and belongs to whoever builds on it. Move a table, tear the walls out, replace the whole thing
 * with something built by hand; correct the layout with {@code /poker setup} and the plugin follows.
 * <p>
 * What it builds is a hall: dark wood, red carpet, a gold line at head height, lanterns on chains over
 * every table. It is meant to be good enough to play in on the first evening and worth replacing on the
 * tenth.
 */
public final class CasinoBuilder {

    /** The floor blocks sit here; people stand on top of them. */
    public static final int FLOOR_Y = 63;
    /** What people stand and sit on. */
    public static final int STAND_Y = FLOOR_Y + 1;
    /** The felt, one block up from the floor, which is where cards and chips are drawn. */
    public static final int FELT_Y = STAND_Y + 1;
    /** How high the room is. */
    private static final int WALL_HEIGHT = 9;
    /** How far apart two tables stand. */
    private static final int TABLE_SPACING = 18;
    /** How far the chairs sit from the middle of a table. */
    private static final double SEAT_RADIUS = 4.5d;
    /** How wide the table itself is, as a radius in blocks. */
    private static final double TABLE_RADIUS = 3.2d;
    /** How much room is left between the outermost table and the wall. */
    private static final int MARGIN = 9;
    /** The most tables that stand next to each other before a new row is started. */
    private static final int COLUMNS = 3;

    private CasinoBuilder() {
    }

    /**
     * Builds a casino for a given number of tables and writes down where everything ended up.
     *
     * @param world  the (empty) world to build in
     * @param tables how many tables it needs
     * @param seats  how many chairs each of them has
     * @param layout where to record it
     */
    public static void build(World world, int tables, int seats, CasinoLayout layout) {
        int columns = Math.min(COLUMNS, Math.max(1, tables));
        int rows = (tables + columns - 1) / columns;

        List<TableSpot> spots = new ArrayList<>();
        for (int index = 0; index < tables; index++) {
            int column = index % columns;
            int row = index / columns;
            double x = (column - (columns - 1) / 2.0d) * TABLE_SPACING;
            double z = (row - (rows - 1) / 2.0d) * TABLE_SPACING;
            TableSpot spot = new TableSpot(index, x + 0.5d, FELT_Y, z + 0.5d, SEAT_RADIUS);
            spot.layOutSeats(seats);
            spots.add(spot);
        }

        int halfWidth = (int) Math.ceil((columns - 1) * TABLE_SPACING / 2.0d) + MARGIN;
        int halfDepth = (int) Math.ceil((rows - 1) * TABLE_SPACING / 2.0d) + MARGIN;

        clear(world, halfWidth + 2, halfDepth + 2);
        floor(world, halfWidth, halfDepth);
        walls(world, halfWidth, halfDepth);
        ceiling(world, halfWidth, halfDepth);
        entrance(world, halfDepth);

        for (TableSpot spot : spots) {
            table(world, spot, seats);
            chandelier(world, spot);
        }

        layout.setWorldName(world.getName());
        layout.setFloorY(STAND_Y);
        layout.setTables(spots);
        Location spawn = new Location(world, 0.5d, STAND_Y, halfDepth - 2 + 0.5d);
        spawn.setYaw(180f);
        layout.setSpawn(spawn);
        layout.setBuilt(true);
        layout.save();

        world.setSpawnLocation(spawn);
    }

    /**
     * Empties the box the casino goes in, so building twice in the same place cannot leave half of the old
     * one standing.
     */
    private static void clear(World world, int halfWidth, int halfDepth) {
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfDepth; z <= halfDepth; z++) {
                for (int y = FLOOR_Y; y <= FLOOR_Y + WALL_HEIGHT + 2; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private static void floor(World world, int halfWidth, int halfDepth) {
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfDepth; z <= halfDepth; z++) {
                boolean edge = Math.abs(x) == halfWidth || Math.abs(z) == halfDepth;
                world.getBlockAt(x, FLOOR_Y, z).setType(
                        edge ? Material.POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS, false);
                if (edge) continue;
                // a red carpet over everything but a blackstone border and a path down the middle, which is
                // what stops a hall this size reading as one flat colour
                boolean path = Math.abs(x) <= 1;
                if (!path) {
                    world.getBlockAt(x, STAND_Y, z).setType(
                            (x + z) % 7 == 0 ? Material.BROWN_CARPET : Material.RED_CARPET, false);
                }
            }
        }
    }

    private static void walls(World world, int halfWidth, int halfDepth) {
        for (int y = STAND_Y; y < STAND_Y + WALL_HEIGHT; y++) {
            Material material = wallMaterial(y - STAND_Y);
            for (int x = -halfWidth; x <= halfWidth; x++) {
                world.getBlockAt(x, y, -halfDepth).setType(material, false);
                world.getBlockAt(x, y, halfDepth).setType(material, false);
            }
            for (int z = -halfDepth; z <= halfDepth; z++) {
                world.getBlockAt(-halfWidth, y, z).setType(material, false);
                world.getBlockAt(halfWidth, y, z).setType(material, false);
            }
        }
        // pillars every five blocks, with a lantern on the inside face at head height
        for (int x = -halfWidth; x <= halfWidth; x += 5) {
            pillar(world, x, -halfDepth, 0, 1);
            pillar(world, x, halfDepth, 0, -1);
        }
        for (int z = -halfDepth; z <= halfDepth; z += 5) {
            pillar(world, -halfWidth, z, 1, 0);
            pillar(world, halfWidth, z, -1, 0);
        }
    }

    /**
     * @param height how far up the wall we are
     * @return dark wood at the bottom, a gold line at head height, deep red above it
     */
    private static Material wallMaterial(int height) {
        if (height <= 2) return Material.DARK_OAK_PLANKS;
        if (height == 3) return Material.GOLD_BLOCK;
        if (height >= WALL_HEIGHT - 1) return Material.DARK_OAK_PLANKS;
        return Material.RED_CONCRETE;
    }

    private static void pillar(World world, int x, int z, int inX, int inZ) {
        for (int y = STAND_Y; y < STAND_Y + WALL_HEIGHT; y++) {
            Block block = world.getBlockAt(x, y, z);
            block.setType(Material.DARK_OAK_LOG, false);
            if (block.getBlockData() instanceof Orientable orientable) {
                orientable.setAxis(Axis.Y);
                block.setBlockData(orientable, false);
            }
        }
        Block lamp = world.getBlockAt(x + inX, STAND_Y + 4, z + inZ);
        if (lamp.getType() == Material.AIR) lamp.setType(Material.LANTERN, false);
    }

    private static void ceiling(World world, int halfWidth, int halfDepth) {
        int y = STAND_Y + WALL_HEIGHT;
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfDepth; z <= halfDepth; z++) {
                boolean beam = Math.abs(x) % 6 == 0 || Math.abs(z) % 6 == 0;
                world.getBlockAt(x, y, z).setType(
                        beam ? Material.DARK_OAK_LOG : Material.DARK_OAK_PLANKS, false);
            }
        }
    }

    /**
     * A doorway in the south wall, so the room has somewhere to be entered from and the spawn has something
     * to look at.
     */
    private static void entrance(World world, int halfDepth) {
        for (int x = -1; x <= 1; x++) {
            for (int y = STAND_Y; y < STAND_Y + 3; y++) {
                world.getBlockAt(x, y, halfDepth).setType(Material.AIR, false);
            }
            world.getBlockAt(x, STAND_Y + 3, halfDepth).setType(Material.GOLD_BLOCK, false);
        }
        for (int z = halfDepth - 1; z >= halfDepth - 4; z--) {
            for (int x = -1; x <= 1; x++) {
                world.getBlockAt(x, STAND_Y, z).setType(Material.RED_CARPET, false);
            }
        }
    }

    /**
     * One table: a dark wooden rim with green felt inside it, and a chair for every seat, each looking at
     * the middle.
     */
    private static void table(World world, TableSpot spot, int seats) {
        int centreX = (int) Math.floor(spot.getCentreX());
        int centreZ = (int) Math.floor(spot.getCentreZ());
        int radius = (int) Math.ceil(TABLE_RADIUS);
        for (int x = centreX - radius; x <= centreX + radius; x++) {
            for (int z = centreZ - radius; z <= centreZ + radius; z++) {
                double distance = Math.hypot(x + 0.5d - spot.getCentreX(), z + 0.5d - spot.getCentreZ());
                if (distance > TABLE_RADIUS) continue;
                // no carpet under a table, or the felt sits on top of a rug
                world.getBlockAt(x, STAND_Y, z).setType(Material.DARK_OAK_PLANKS, false);
                boolean rim = distance > TABLE_RADIUS - 1.0d;
                world.getBlockAt(x, FELT_Y, z).setType(
                        rim ? Material.DARK_OAK_SLAB : Material.GREEN_CARPET, false);
            }
        }
        for (int seat = 0; seat < seats; seat++) {
            Location chair = spot.seat(world, seat, STAND_Y);
            Block block = world.getBlockAt(chair);
            block.setType(Material.DARK_OAK_STAIRS, false);
            if (block.getBlockData() instanceof Stairs stairs) {
                stairs.setFacing(facingAwayFrom(chair, spot));
                block.setBlockData(stairs, false);
            }
        }
    }

    /**
     * @return the direction the back of the chair points, so its seat looks at the table
     */
    private static BlockFace facingAwayFrom(Location chair, TableSpot spot) {
        double dx = chair.getX() - spot.getCentreX();
        double dz = chair.getZ() - spot.getCentreZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? BlockFace.WEST : BlockFace.EAST;
        }
        return dz > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    /**
     * Light over a table, hanging rather than buried in the ceiling: a table you cannot read your cards at
     * is a table nobody sits down at.
     */
    private static void chandelier(World world, TableSpot spot) {
        int x = (int) Math.floor(spot.getCentreX());
        int z = (int) Math.floor(spot.getCentreZ());
        int ceiling = STAND_Y + WALL_HEIGHT - 1;
        for (int y = ceiling; y > ceiling - 2; y--) {
            Block chain = world.getBlockAt(x, y, z);
            chain.setType(Material.IRON_CHAIN, false);
            BlockData data = chain.getBlockData();
            if (data instanceof Orientable orientable) {
                orientable.setAxis(Axis.Y);
                chain.setBlockData(orientable, false);
            }
        }
        world.getBlockAt(x, ceiling - 2, z).setType(Material.SEA_LANTERN, false);
        for (int[] offset : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
            world.getBlockAt(x + offset[0], ceiling, z + offset[1]).setType(Material.IRON_CHAIN, false);
            world.getBlockAt(x + offset[0], ceiling - 1, z + offset[1]).setType(Material.LANTERN, false);
        }
    }
}
