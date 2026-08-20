package de.schnorrenbergers.bedwars.map;

import de.schnorrenbergers.bedwars.game.TeamColor;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Everything one team owns on a map: where it starts, where its bed is and where it shops.
 */
public class TeamSpot {

    /** How far from the spawn nobody but the team may build, when the map does not say. */
    public static final int DEFAULT_PROTECTION = 8;

    private final TeamColor color;

    private MapPoint spawn;
    private MapPoint bed;
    private MapPoint shop;
    private MapPoint upgrade;
    private MapPoint generator;
    private int protection = DEFAULT_PROTECTION;

    public TeamSpot(TeamColor color) {
        this.color = color;
    }

    /**
     * @param color   which team
     * @param section its block in the map file
     * @return the team as it is written there
     */
    public static TeamSpot read(TeamColor color, ConfigurationSection section) {
        TeamSpot spot = new TeamSpot(color);
        spot.spawn = MapPoint.read(section, "spawn");
        spot.bed = MapPoint.read(section, "bed");
        spot.shop = MapPoint.read(section, "shop");
        spot.upgrade = MapPoint.read(section, "upgrade");
        spot.generator = MapPoint.read(section, "generator");
        spot.protection = section.getInt("protection", DEFAULT_PROTECTION);
        return spot;
    }

    /**
     * @param section its block in the map file
     */
    public void write(ConfigurationSection section) {
        if (spawn != null) spawn.write(section, "spawn");
        if (bed != null) bed.write(section, "bed");
        if (shop != null) shop.write(section, "shop");
        if (upgrade != null) upgrade.write(section, "upgrade");
        if (generator != null) generator.write(section, "generator");
        section.set("protection", protection);
    }

    /**
     * @return whether this team can be played: it has somewhere to start, a bed and a generator
     */
    public boolean isComplete() {
        return spawn != null && bed != null && generator != null;
    }

    public TeamColor getColor() {
        return color;
    }

    public MapPoint getSpawn() {
        return spawn;
    }

    public void setSpawn(MapPoint spawn) {
        this.spawn = spawn;
    }

    public MapPoint getBed() {
        return bed;
    }

    public void setBed(MapPoint bed) {
        this.bed = bed;
    }

    public MapPoint getShop() {
        return shop;
    }

    public void setShop(MapPoint shop) {
        this.shop = shop;
    }

    public MapPoint getUpgrade() {
        return upgrade;
    }

    public void setUpgrade(MapPoint upgrade) {
        this.upgrade = upgrade;
    }

    public MapPoint getGenerator() {
        return generator;
    }

    public void setGenerator(MapPoint generator) {
        this.generator = generator;
    }

    public int getProtection() {
        return protection;
    }

    public void setProtection(int protection) {
        this.protection = Math.max(0, protection);
    }
}
