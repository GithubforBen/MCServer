package de.schnorrenbergers.survival.featrues.lab.brew;

/**
 * Checks the brew state: pause, resume, losing the ingredient and starting twice.
 * <p>
 * A class with a main, like the other checks. From the repository root:
 * <pre>
 * javac -d /tmp/lbc Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/BrewState.java \
 *       Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/brew/LabBrewCheck.java
 * java -cp /tmp/lbc de.schnorrenbergers.survival.featrues.lab.brew.LabBrewCheck
 * </pre>
 * Exits non-zero when something is wrong.
 */
public final class LabBrewCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        pauseAndResume();
        ingredientLost();
        doubleStart();
        arrow();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void arrow() {
        BrewState brew = new BrewState(400);
        check("arrow idle", brew.arrowTicks(0, 400), 0);
        brew.advance(0, true);
        brew.advance(100, true);
        check("arrow at quarter", brew.arrowTicks(0, 400), 300);
        check("arrow fills over time", brew.arrowTicks(100, 400), 200);
        check("arrow never below empty", brew.arrowTicks(900, 400), 0);
    }

    private static void pauseAndResume() {
        BrewState brew = new BrewState(400);
        check("first step starts", brew.advance(0, true), BrewState.Step.STARTED);
        check("runs", brew.advance(200, true), BrewState.Step.RUNNING);
        check("half progress", brew.fraction(), 0.5);

        // unload and load: only the saved value crosses, and no time passes meanwhile
        BrewState again = BrewState.load(brew.save(), 400);
        check("resumed running", again.isRunning(), true);
        check("resumed at half", again.progress(), 200);
        check("continues", again.advance(100, true), BrewState.Step.RUNNING);
        check("three quarters", again.fraction(), 0.75);
        check("finishes", again.advance(100, true), BrewState.Step.FINISHED);
        check("finishes once", again.advance(20, false), BrewState.Step.IDLE);
        check("nothing to keep", again.save(), null);
        check("damaged value is a fresh state", BrewState.load("x", 400).isRunning(), false);
        check("a value past the end never finishes at load", BrewState.load("9999", 400).progress(), 399);
    }

    // spec: lab-brewing#ingredient-taken-out-mid-brew
    private static void ingredientLost() {
        BrewState brew = new BrewState(400);
        brew.advance(0, true);
        brew.advance(200, true);
        check("ingredient gone resets", brew.advance(20, false), BrewState.Step.RESET);
        check("progress is gone", brew.progress(), 0);
        check("not running", brew.isRunning(), false);
        check("nothing to keep", brew.save(), null);
        check("ingredient back starts over", brew.advance(20, true), BrewState.Step.STARTED);
        check("from zero", brew.progress(), 0);
    }

    private static void doubleStart() {
        BrewState brew = new BrewState(400);
        check("start", brew.start(), true);
        brew.advance(100, true);
        check("second start is refused", brew.start(), false);
        check("progress untouched", brew.progress(), 100);
        BrewState loaded = BrewState.load(brew.save(), 400);
        check("a loaded brew refuses a start too", loaded.start(), false);
        check("and keeps its progress", loaded.progress(), 100);
    }

    private static void check(String what, Object actual, Object expected) {
        if (java.util.Objects.equals(expected, actual)) {
            passed++;
            return;
        }
        failed++;
        System.out.println("FAIL " + what + ": expected " + expected + ", got " + actual);
    }
}
