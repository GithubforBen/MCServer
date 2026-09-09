package de.schnorrenbergers.poker.game;

/**
 * One decision.
 *
 * @param type   what was done
 * @param amount the total this player is committed for on this street afterwards, not the difference. A
 *               raise "to 400" is 400 here whatever was already in, which is how the rules talk about it
 *               and the only version that stays right when somebody had already called something
 */
public record Action(ActionType type, int amount) {

    public static Action fold() {
        return new Action(ActionType.FOLD, 0);
    }

    public static Action check() {
        return new Action(ActionType.CHECK, 0);
    }

    public static Action call(int to) {
        return new Action(ActionType.CALL, to);
    }

    public static Action bet(int to) {
        return new Action(ActionType.BET, to);
    }

    public static Action raise(int to) {
        return new Action(ActionType.RAISE, to);
    }

    public static Action allIn(int to) {
        return new Action(ActionType.ALL_IN, to);
    }
}
