package de.schnorrenbergers.bedwars.shop.item;

import de.schnorrenbergers.bedwars.game.Loadout;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A tool chain, climbed one step at a time.
 * <p>
 * A player does not pick a golden pickaxe out of four pickaxes, they upgrade the one they have: a death
 * costs a step, and the step above the one they are on is the only one worth buying. Written out as four
 * separate buttons the shop reads as four unrelated items, three of which cannot be bought at all.
 * <p>
 * Only tools. Swords and armour are not chains and are deliberately left alone - a diamond sword is
 * something you buy because you can afford it, not something you work up to, and the same goes for the
 * armour. Collapsing those would take a choice away rather than clear one up.
 *
 * @param key   what the chain is called, e.g. {@code tool:pickaxe}
 * @param steps its entries, lowest step first
 */
public record ShopChain(String key, List<ShopItem> steps) {

    /**
     * @param item a shop entry
     * @return the chain it belongs to, or {@code null} when it is bought on its own
     */
    public static @Nullable String keyOf(ShopItem item) {
        return item.isTool() ? "tool:" + item.toolGroup().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * Sorts a page into chains and plain items, keeping the order the page was written in.
     * <p>
     * A chain takes the place of its lowest written step, so collapsing four pickaxes into one button
     * does not shuffle the rest of the page around.
     *
     * @param items the entries of one page, already filtered down to what may be sold here
     * @return one entry per button: a chain, or a single item
     */
    public static List<Object> group(List<ShopItem> items) {
        Map<String, List<ShopItem>> chains = new LinkedHashMap<>();
        List<Object> order = new ArrayList<>();
        for (ShopItem item : items) {
            String key = keyOf(item);
            if (key == null) {
                order.add(item);
                continue;
            }
            List<ShopItem> steps = chains.get(key);
            if (steps == null) {
                steps = new ArrayList<>();
                chains.put(key, steps);
                order.add(key);
            }
            steps.add(item);
        }
        List<Object> grouped = new ArrayList<>();
        for (Object entry : order) {
            if (entry instanceof ShopItem item) {
                grouped.add(item);
                continue;
            }
            List<ShopItem> steps = new ArrayList<>(chains.get((String) entry));
            steps.sort(Comparator.comparingInt(ShopChain::tierOf));
            grouped.add(new ShopChain((String) entry, List.copyOf(steps)));
        }
        return List.copyOf(grouped);
    }

    /**
     * @param item an entry of a chain
     * @return which step it is
     */
    public static int tierOf(ShopItem item) {
        return item.toolTier();
    }

    /**
     * @param loadout what the player owns
     * @return the step they have reached, 0 when they have never bought into this chain
     */
    public int reached(Loadout loadout) {
        return loadout.getToolTier(key.substring("tool:".length()));
    }

    /**
     * @param loadout what the player owns
     * @return the step to offer them: the cheapest one above what they have, or the top one when they
     *         are already there
     */
    public ShopItem offer(Loadout loadout) {
        int owned = reached(loadout);
        for (ShopItem step : steps) {
            if (tierOf(step) > owned) return step;
        }
        return steps.getLast();
    }

    /**
     * @param loadout what the player owns
     * @return whether there is nothing left to buy in this chain
     */
    public boolean isMaxed(Loadout loadout) {
        return reached(loadout) >= tierOf(steps.getLast());
    }

    /**
     * @param loadout what the player owns
     * @return how many steps of the chain they have behind them, counted from its own first step
     */
    public int level(Loadout loadout) {
        int owned = reached(loadout);
        int level = 0;
        for (ShopItem step : steps) {
            if (tierOf(step) <= owned) level++;
        }
        return level;
    }

    /**
     * @return how many steps the chain has
     */
    public int size() {
        return steps.size();
    }

    /**
     * @return where the button sits, taken from the first step that names a slot
     */
    public int slot() {
        for (ShopItem step : steps) {
            if (step.slot() >= 0) return step.slot();
        }
        return -1;
    }
}
