package de.schnorrenbergers.survival.featrues.lab.chain;

import de.schnorrenbergers.survival.featrues.lab.LabSettings;
import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.item.LabKind;

import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Checks the step table, the chances and the average of the chain with the default yields, using a seeded
 * random so the outcome is repeatable. Needs paper-api on the classpath (for the config reader). Exits
 * non-zero when something is wrong.
 */
public final class LabChainCheck {

    private static final LabKind[] FILTERS = {LabKind.PAPIERFILTER, LabKind.KOHLEFILTER,
            LabKind.SEIDENFILTER, LabKind.GOLDFILTER};
    private static final double[][] CHANCES = {{0.35, 0.40}, {0.15, 0.50}, {0.06, 0.54}, {0.04, 0.56}};
    private static int failed;

    public static void main(String[] args) throws Exception {
        File file = new File(Files.createTempDirectory("lab-chain").toFile(), "configs/lab.yml");
        LabChain chain = LabChain.of(new LabSettings(file, Logger.getLogger("check")));

        // spec: lab-brewing#papierfilter-chances
        // spec: lab-brewing#kohlefilter-chances
        // spec: lab-brewing#seidenfilter-chances
        // spec: lab-brewing#goldfilter-chances
        for (int t = 1; t <= 4; t++) {
            LabData filter = LabData.of(FILTERS[t - 1]);
            LabData paste = LabData.of(LabKind.PASTE, t);
            check("goods " + t, chain.goods(filter, paste).orElseThrow(), LabData.of(LabKind.PASTE, t + 1));
            Random random = new Random(100 + t);
            int[] counts = new int[3];
            int n = 200_000;
            for (int i = 0; i < n; i++) counts[LabChain.roll(chain.chances(filter, paste), random).ordinal()]++;
            near("goods share " + t, counts[0] / (double) n, CHANCES[t - 1][0]);
            near("waste share " + t, counts[1] / (double) n, CHANCES[t - 1][1]);
            near("nothing share " + t, counts[2] / (double) n, 1 - CHANCES[t - 1][0] - CHANCES[t - 1][1]);
        }

        // spec: lab-brewing#wrong-filter
        check("gold on purity 1", chain.isStep(LabData.of(LabKind.GOLDFILTER), LabData.of(LabKind.PASTE, 1)), false);
        check("paper on purity 2", chain.isStep(LabData.of(LabKind.PAPIERFILTER), LabData.of(LabKind.PASTE, 2)), false);

        // spec: lab-brewing#purity-carries-over
        check("kokain keeps purity", chain.goods(LabData.of(LabKind.FEENSTAUB), LabData.of(LabKind.PASTE, 3))
                .orElseThrow(), LabData.of(LabKind.KOKAIN, 3));

        // spec: lab-brewing#matching-filter
        LabData paper = LabData.of(LabKind.PAPIERFILTER);
        LabData p1 = LabData.of(LabKind.PASTE, 1);
        LabChain.Brew all = chain.brew(paper, 0, new LabData[]{p1, p1, p1}, new Random(7));
        check("filter used once", all.used(), 1);
        for (int i = 0; i < 3; i++) {
            LabData s = all.slots()[i];
            check("slot " + i + " rolled", all.rolled()[i], true);
            check("slot " + i + " result", s == null || s.kind() == LabKind.PFLANZENABFALL
                    || s.equals(LabData.of(LabKind.PASTE, 2)), true);
        }

        // spec: lab-brewing#mixed-tiers
        LabData p2 = LabData.of(LabKind.PASTE, 2);
        LabChain.Brew mixed = chain.brew(paper, 0, new LabData[]{p1, p2, p1}, new Random(8));
        check("mixed: other tier untouched", mixed.slots()[1], p2);
        check("mixed: other tier not rolled", mixed.rolled()[1], false);
        check("mixed: two rolled", mixed.rolled()[0] && mixed.rolled()[2], true);

        // spec: lab-brewing#waste-blocks-the-slot
        LabData waste = LabData.of(LabKind.PFLANZENABFALL);
        LabChain.Brew blocked = chain.brew(null, 3, new LabData[]{waste, null, LabChain.OCCUPIED}, new Random(9));
        check("waste kept", blocked.slots()[0], waste);
        check("waste not rolled", blocked.rolled()[0], false);
        check("occupied not rolled", blocked.rolled()[2], false);
        check("one cane for the one free slot", blocked.used(), 1);
        LabChain.Brew none = chain.brew(paper, 0, new LabData[]{waste, waste, LabChain.OCCUPIED}, new Random(9));
        check("filter on waste uses nothing", none.used(), 0);

        // spec: lab-brewing#two-sugar-cane-left
        LabChain.Brew two = chain.brew(null, 2, new LabData[3], new Random(10));
        check("two cane, two rolled", two.used(), 2);
        check("third slot not rolled", two.rolled()[2], false);

        // spec: lab-brewing#three-empty-slots
        LabChain.Brew three = chain.brew(null, 64, new LabData[3], new Random(11));
        check("three cane used", three.used(), 3);
        check("all rolled", three.rolled()[0] && three.rolled()[1] && three.rolled()[2], true);

        // spec: lab-brewing#no-free-slot
        LabChain.Brew full = chain.brew(null, 64, new LabData[]{p1, p2, LabChain.OCCUPIED}, new Random(12));
        check("no free slot uses no cane", full.used(), 0);

        average(chain);

        if (failed > 0) {
            System.out.println(failed + " checks failed");
            System.exit(1);
        }
        System.out.println("LabChainCheck ok");
    }

    /** One cane per item, items walk the whole chain; counts the cane per Kokain of purity 5. */
    // spec: lab-brewing#simulated-average
    private static void average(LabChain chain) {
        Random random = new Random(20261105L);
        long cane = 100_000_000L;
        long kokain = 0;
        LabData feen = LabData.of(LabKind.FEENSTAUB);
        for (long c = 0; c < cane; c++) {
            if (LabChain.roll(chain.caneYield(), random) != LabChain.Roll.GOODS) continue;
            LabData item = LabChain.caneGoods();
            boolean alive = true;
            while (alive && item.purity() < 5) {
                LabData filter = LabData.of(FILTERS[item.purity() - 1]);
                alive = LabChain.roll(chain.chances(filter, item), random) == LabChain.Roll.GOODS;
                if (alive) item = chain.goods(filter, item).orElseThrow();
            }
            if (!alive) continue;
            if (LabChain.roll(chain.chances(feen, item), random) == LabChain.Roll.GOODS) kokain++;
        }
        double perKokain = cane / (double) kokain;
        System.out.println("kokain=" + kokain + " cane per kokain=" + perKokain);
        check("average between 19000 and 21000", perKokain > 19_000 && perKokain < 21_000, true);
    }

    private static void near(String what, double actual, double expected) {
        check(what + " (" + actual + " vs " + expected + ")", Math.abs(actual - expected) < 0.01, true);
    }

    private static void check(String what, Object actual, Object expected) {
        if (!expected.equals(actual)) {
            System.out.println("FAIL " + what + ": expected " + expected + " but was " + actual);
            failed++;
        }
    }
}
