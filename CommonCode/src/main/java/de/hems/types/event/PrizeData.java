package de.hems.types.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What somebody gets for a placing: some money and a handful of items.
 * <p>
 * Stored as one string in the free settings of an {@link EventData}, so prizes need no table of their own
 * and can be read straight out of the config file. The items are kept as material names rather than as
 * bukkit types, because the launcher stores them and has no bukkit to resolve them with - the game server
 * turns them into real stacks when it hands them out.
 */
public class PrizeData implements Serializable {

    private static final long serialVersionUID = 4320L;

    /** The settings key holding the prize for a placing, one to three. */
    public static final String PLACE_KEY = "prize.place.";
    /** The settings key holding what everybody who took part gets. */
    public static final String PARTICIPATION_KEY = "prize.participation";
    /** How many places can be rewarded separately. */
    public static final int PLACES = 3;

    private int money;
    /** Material name to amount, in the order they were added. */
    private Map<String, Integer> items = new LinkedHashMap<>();

    public PrizeData() {
    }

    public PrizeData(int money) {
        this.money = money;
    }

    /**
     * @param material the item, as a bukkit material name
     * @param amount   how many
     * @return this prize, so calls can be chained
     */
    public PrizeData withItem(String material, int amount) {
        if (material == null || material.isBlank() || amount <= 0) return this;
        getItems().merge(material.toUpperCase(Locale.ROOT), amount, Integer::sum);
        return this;
    }

    /**
     * @return whether there is anything to hand out
     */
    public boolean isEmpty() {
        return money <= 0 && getItems().isEmpty();
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = Math.max(0, money);
    }

    public Map<String, Integer> getItems() {
        if (items == null) items = new LinkedHashMap<>();
        return items;
    }

    public void setItems(Map<String, Integer> items) {
        this.items = items == null ? new LinkedHashMap<>() : items;
    }

    /**
     * @return the prize written out, readable in a config file
     */
    public String serialize() {
        StringBuilder text = new StringBuilder("money=").append(money);
        if (!getItems().isEmpty()) {
            text.append(";items=");
            boolean first = true;
            for (Map.Entry<String, Integer> item : getItems().entrySet()) {
                if (!first) text.append(',');
                text.append(item.getKey()).append(':').append(item.getValue());
                first = false;
            }
        }
        return text.toString();
    }

    /**
     * @param text a prize as {@link #serialize()} wrote it
     * @return the prize, empty if the text is unusable
     */
    public static PrizeData parse(String text) {
        PrizeData prize = new PrizeData();
        if (text == null || text.isBlank()) return prize;
        for (String part : text.split(";")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) continue;
            String key = pair[0].trim().toLowerCase(Locale.ROOT);
            String value = pair[1].trim();
            if (key.equals("money")) {
                try {
                    prize.setMoney(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                    // a broken number costs the money, not the whole prize
                }
            } else if (key.equals("items")) {
                for (String item : value.split(",")) {
                    String[] spec = item.split(":", 2);
                    if (spec.length != 2) continue;
                    try {
                        prize.withItem(spec[0].trim(), Integer.parseInt(spec[1].trim()));
                    } catch (NumberFormatException ignored) {
                        // same again - skip the entry, keep the rest
                    }
                }
            }
        }
        return prize;
    }

    /**
     * @param event the event to read from
     * @param place the placing, one to {@link #PLACES}
     * @return what that place gets
     */
    public static PrizeData ofPlace(EventData event, int place) {
        return parse(event.getSetting(PLACE_KEY + place, null));
    }

    /**
     * @param event the event to read from
     * @return what everybody who took part gets
     */
    public static PrizeData ofParticipation(EventData event) {
        return parse(event.getSetting(PARTICIPATION_KEY, null));
    }

    /**
     * @param event the event to write to
     * @param place the placing, one to {@link #PLACES}
     * @param prize what that place gets
     */
    public static void setPlace(EventData event, int place, PrizeData prize) {
        event.setSetting(PLACE_KEY + place, prize.serialize());
    }

    public static void setParticipation(EventData event, PrizeData prize) {
        event.setSetting(PARTICIPATION_KEY, prize.serialize());
    }

    /**
     * @return the prize written out for a tooltip, one line per thing
     */
    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        if (money > 0) lines.add(money + " Bits");
        for (Map.Entry<String, Integer> item : getItems().entrySet()) {
            lines.add(item.getValue() + "x " + item.getKey());
        }
        if (lines.isEmpty()) lines.add("nichts");
        return lines;
    }
}
