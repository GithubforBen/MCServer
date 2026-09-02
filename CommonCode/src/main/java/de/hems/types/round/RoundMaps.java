package de.hems.types.round;

import de.hems.types.FileType;
import de.hems.types.ServerTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The maps a round can be played on, as far as the lobby can know before a bedwars server exists.
 * <p>
 * A round server ships with the maps of its template, so the template is the list. That is not the whole
 * truth - an admin can drop a world into {@code maps/} on a running server, and the bedwars plugin will
 * happily play it - but it is the part that is true for a server that has not been created yet, which is
 * exactly the moment somebody picks a map.
 */
public final class RoundMaps {

    private RoundMaps() {
    }

    /**
     * @return the id of every map a fresh round server is delivered with
     */
    public static List<String> available() {
        List<String> maps = new ArrayList<>();
        for (FileType.ASSET asset : ServerTemplate.BEDWARS.getAssets()) {
            String map = FileType.ASSET.getBedwarsMap(asset);
            if (map != null && !maps.contains(map)) maps.add(map);
        }
        return maps;
    }

    /**
     * @param id a map id
     * @return it written the way a player would read it
     */
    public static String displayName(String id) {
        if (id == null || id.isBlank()) return "Zufällig";
        String cleaned = id.replace('_', ' ').replace('-', ' ').trim();
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1).toLowerCase(Locale.ROOT);
    }
}
