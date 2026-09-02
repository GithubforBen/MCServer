package de.hems.types.money;

import java.io.Serializable;

/**
 * What became of one change to a balance.
 * <p>
 * A change is a delta rather than a new number, so two servers that both pay somebody out at the same
 * moment add up instead of overwriting each other. The launcher is the only node that applies them, which
 * is what makes {@link #isSuccessful()} trustworthy: when it says the money was taken, it was taken once.
 */
public class BalanceResult implements Serializable {

    private static final long serialVersionUID = 4501L;

    private boolean successful;
    private String holder;
    private int balance;
    private String message;

    public BalanceResult() {
    }

    public BalanceResult(boolean successful, String holder, int balance, String message) {
        this.successful = successful;
        this.holder = holder;
        this.balance = balance;
        this.message = message;
    }

    /**
     * @param holder  whose account it is
     * @param balance what is on it now
     * @return a result saying the change went through
     */
    public static BalanceResult ok(String holder, int balance) {
        return new BalanceResult(true, holder, balance, null);
    }

    /**
     * @param holder  whose account it is
     * @param balance what is on it, unchanged
     * @param message why nothing happened
     * @return a result saying the change was refused
     */
    public static BalanceResult failed(String holder, int balance, String message) {
        return new BalanceResult(false, holder, balance, message);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getHolder() {
        return holder;
    }

    /**
     * @return the balance after the change, or the untouched one when it was refused
     */
    public int getBalance() {
        return balance;
    }

    /**
     * @return why the change was refused, {@code null} when it went through
     */
    public String getMessage() {
        return message;
    }
}
