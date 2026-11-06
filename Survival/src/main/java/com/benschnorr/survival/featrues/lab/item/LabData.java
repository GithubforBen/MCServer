package com.benschnorr.survival.featrues.lab.item;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The content of one lab item. Effects are kept as an ordered set, so the same content always gives the
 * same data, which is what makes items stack whatever order effects were cut in.
 *
 * @param kind      what it is
 * @param purity    1 to 5, or 0 for kinds without purity
 * @param effects   cut-in effects, at most three
 * @param stretched whether it came from stretching
 */
public record LabData(LabKind kind, int purity, Set<LabEffect> effects, boolean stretched) {

    public static final int MAX_EFFECTS = 3;

    public LabData {
        if (kind.hasPurity()) {
            if (purity < 1 || purity > 5) throw new IllegalArgumentException("purity " + purity);
        } else {
            purity = 0;
        }
        EnumSet<LabEffect> sorted = EnumSet.noneOf(LabEffect.class);
        if (kind == LabKind.KOKAIN) {
            sorted.addAll(effects);
            if (sorted.size() > MAX_EFFECTS) throw new IllegalArgumentException("effects " + sorted);
        } else {
            stretched = false;
        }
        effects = Collections.unmodifiableSet(sorted);
    }

    public static LabData of(LabKind kind, int purity) {
        return new LabData(kind, purity, Set.of(), false);
    }

    public static LabData of(LabKind kind) {
        return new LabData(kind, 0, Set.of(), false);
    }

    public LabData withPurity(int newPurity) {
        return new LabData(kind, newPurity, effects, stretched);
    }

    public LabData withEffects(Collection<LabEffect> newEffects) {
        return new LabData(kind, purity, Set.copyOf(newEffects), stretched);
    }

    public LabData asStretched() {
        return new LabData(kind, purity, effects, true);
    }
}
