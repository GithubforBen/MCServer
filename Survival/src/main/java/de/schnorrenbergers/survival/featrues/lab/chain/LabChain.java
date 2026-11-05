package de.schnorrenbergers.survival.featrues.lab.chain;

import de.schnorrenbergers.survival.featrues.lab.LabSettings;
import de.schnorrenbergers.survival.featrues.lab.LabSettings.Yield;
import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.item.LabKind;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * The step table and the rolling, with no server in it. One rule table serves both brewing paths: sugar
 * cane on empty slots, and a filter or Feenstaub on top of paste.
 * see knowledge: /decisions/gadget-choices.md
 */
public final class LabChain {

    /** What one rolled item became. */
    public enum Roll { GOODS, WASTE, NOTHING }

    /** Stands for a bottom item that is not a lab item: it occupies the slot and is no input of anything. */
    public static final LabData OCCUPIED = LabData.of(LabKind.PFLANZENABFALL);

    /**
     * The state of the three slots after a brew.
     *
     * @param slots  what each slot holds afterwards, null for empty
     * @param rolled which slots were rolled
     * @param used   how many of the top ingredient were used
     */
    public record Brew(LabData[] slots, boolean[] rolled, int used) {
    }

    private final Yield cane;
    private final Yield[] filters;
    private final Yield kokain;

    public LabChain(Yield cane, Yield[] filters, Yield kokain) {
        if (filters.length != 4) throw new IllegalArgumentException("four filters");
        this.cane = cane;
        this.filters = filters.clone();
        this.kokain = kokain;
    }

    public static LabChain of(LabSettings settings) {
        return new LabChain(settings.sugarCane(),
                new Yield[]{settings.filter(1), settings.filter(2), settings.filter(3), settings.filter(4)},
                settings.kokain());
    }

    /** The good result of {@code top} working on {@code bottom}, or empty when it is no valid step. */
    public Optional<LabData> goods(LabData top, LabData bottom) {
        if (top == null || bottom == null || bottom.kind() != LabKind.PASTE) return Optional.empty();
        int tier = top.kind().filterTier();
        if (tier > 0) {
            return bottom.purity() == tier ? Optional.of(LabData.of(LabKind.PASTE, tier + 1)) : Optional.empty();
        }
        if (top.kind() == LabKind.FEENSTAUB) {
            return Optional.of(LabData.of(LabKind.KOKAIN, bottom.purity()));
        }
        return Optional.empty();
    }

    /** Whether {@code top} is a valid step for {@code bottom}. */
    public boolean isStep(LabData top, LabData bottom) {
        return goods(top, bottom).isPresent();
    }

    /** The chances of the step, or null when it is none. */
    public Yield chances(LabData top, LabData bottom) {
        if (!isStep(top, bottom)) return null;
        int tier = top.kind().filterTier();
        return tier > 0 ? filters[tier - 1] : kokain;
    }

    /** Sugar cane on an empty slot. */
    public Yield caneYield() {
        return cane;
    }

    /** The good result of sugar cane: Rohpaste of purity 1. */
    public static LabData caneGoods() {
        return LabData.of(LabKind.PASTE, 1);
    }

    public static Roll roll(Yield yield, RandomGenerator random) {
        double r = random.nextDouble();
        if (r < yield.goods()) return Roll.GOODS;
        if (r < yield.goods() + yield.waste()) return Roll.WASTE;
        return Roll.NOTHING;
    }

    /**
     * Runs one brew. {@code top} null means sugar cane and {@code cane} is how many there are; for a
     * filter or Feenstaub {@code cane} is ignored and one is used. Cane rolls empty slots, one cane each;
     * a filter rolls the slots it is a valid step for and is used once. Other slots, waste included,
     * stay untouched.
     *
     * @param slots three slots, null for empty, {@link #OCCUPIED} for something that is no lab item
     */
    public Brew brew(LabData top, int cane, LabData[] slots, RandomGenerator random) {
        LabData[] after = slots.clone();
        boolean[] rolled = new boolean[slots.length];
        int used = 0;
        for (int i = 0; i < slots.length; i++) {
            Yield odds;
            LabData goods;
            if (top == null) {
                if (slots[i] != null || used >= cane) continue;
                odds = this.cane;
                goods = caneGoods();
            } else {
                if (slots[i] == null) continue;
                goods = goods(top, slots[i]).orElse(null);
                if (goods == null) continue;
                odds = chances(top, slots[i]);
            }
            after[i] = switch (roll(odds, random)) {
                case GOODS -> goods;
                case WASTE -> LabData.of(LabKind.PFLANZENABFALL);
                case NOTHING -> null;
            };
            rolled[i] = true;
            if (top == null) used++;
        }
        if (top != null) {
            for (boolean r : rolled) if (r) used = 1;
        }
        return new Brew(after, rolled, used);
    }
}
