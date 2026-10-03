package de.hems.types.event;

import de.hems.types.item.ItemSpec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What somebody gets for a placing: some money and a handful of items.
 * <p>
 * Stored as one string in the free settings of an {@link EventData}, so prizes need no table of their own
 * and can be read straight out of the config file. The items are kept as {@link ItemSpec}s rather than as
 * bukkit types, because the launcher stores them and has no bukkit to resolve them with - the game server
 * turns them into real stacks, enchantments and all, when it hands them out.
 */
public class PrizeData implements Serializable {

    // raised when the items became full item descriptions instead of material names
    private static final long serialVersionUID = 4321L;

    /** The settings key holding the prize for a placing, one to three. */
    public static final String PLACE_KEY = "prize.place.";
    /** The settings key holding what everybody who took part gets. */
    public static final String PARTICIPATION_KEY = "prize.participation";
    /** How many places can be rewarded separately. */
    public static final int PLACES = 3;
    /** What an item entry that carries more than material and amount starts with. */
    private static final String RICH = "~";

    private int money;
    /** The items, in the order they were added. */
    private List<ItemSpec> items = new ArrayList<>();

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
        return withItem(new ItemSpec(material, amount));
    }

    /**
     * Adds an item, onto one that is the same apart from its amount if there is one.
     *
     * @param item the item, with everything it carries
     * @return this prize, so calls can be chained
     */
    public PrizeData withItem(ItemSpec item) {
        if (item == null || item.getMaterial() == null || item.getMaterial().isEmpty() || item.getAmount() <= 0) {
            return this;
        }
        for (ItemSpec existing : getItems()) {
            if (existing.isSimilar(item)) {
                existing.setAmount(existing.getAmount() + item.getAmount());
                return this;
            }
        }
        getItems().add(item.copy());
        return this;
    }

    /**
     * @param material a bukkit material name
     * @return how many plain items of it the prize holds - enchanted or named ones are not counted
     */
    public int amountOf(String material) {
        String wanted = ItemSpec.normaliseMaterial(material);
        int amount = 0;
        for (ItemSpec item : getItems()) {
            if (item.isPlain() && wanted.equals(item.getMaterial())) amount += item.getAmount();
        }
        return amount;
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

    public List<ItemSpec> getItems() {
        if (items == null) items = new ArrayList<>();
        return items;
    }

    public void setItems(List<ItemSpec> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    /**
     * Writes the prize out. A plain item stays readable as {@code MATERIAL:amount}; one that carries more is
     * written as {@code ~} and the item encoded, which needs no separator the line uses itself. A server
     * from before items could carry more skips such an entry instead of misreading it.
     *
     * @return the prize written out, readable in a config file
     */
    public String serialize() {
        StringBuilder text = new StringBuilder("money=").append(money);
        if (!getItems().isEmpty()) {
            text.append(";items=");
            boolean first = true;
            for (ItemSpec item : getItems()) {
                if (!first) text.append(',');
                if (item.isPlain()) {
                    text.append(item.getMaterial()).append(':').append(item.getAmount());
                } else {
                    text.append(RICH).append(item.encode());
                }
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
                    String entry = item.trim();
                    if (entry.startsWith(RICH)) {
                        // added as it is, not merged - two identical entries were written as two on purpose
                        ItemSpec spec = ItemSpec.decode(entry.substring(RICH.length()));
                        if (spec != null && spec.getMaterial() != null && !spec.getMaterial().isEmpty()) {
                            prize.getItems().add(spec);
                        }
                        continue;
                    }
                    String[] spec = entry.split(":", 2);
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
        for (ItemSpec item : getItems()) lines.add(item.describe());
        if (lines.isEmpty()) lines.add("nichts");
        return lines;
    }
}
