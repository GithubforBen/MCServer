package de.hems.types.cosmetic;

import java.io.Serializable;

/**
 * What became of an attempt to buy a cosmetic.
 * <p>
 * The whole purchase happens on the launcher - the price is read, the bits are taken and the cosmetic is
 * granted in one place - so this is the only answer the buying server needs, and nothing is handed over on
 * the strength of a copy that might be a second old.
 */
public class CosmeticPurchase implements Serializable {

    private static final long serialVersionUID = 4804L;

    private boolean successful;
    private String cosmeticId;
    private int paid;
    private int balance;
    private String message;

    public CosmeticPurchase() {
    }

    public CosmeticPurchase(boolean successful, String cosmeticId, int paid, int balance, String message) {
        this.successful = successful;
        this.cosmeticId = cosmeticId;
        this.paid = paid;
        this.balance = balance;
        this.message = message;
    }

    public static CosmeticPurchase ok(String cosmeticId, int paid, int balance) {
        return new CosmeticPurchase(true, cosmeticId, paid, balance, null);
    }

    public static CosmeticPurchase failed(String cosmeticId, int balance, String message) {
        return new CosmeticPurchase(false, cosmeticId, 0, balance, message);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getCosmeticId() {
        return cosmeticId;
    }

    /** @return what it cost */
    public int getPaid() {
        return paid;
    }

    /** @return what is left on the account */
    public int getBalance() {
        return balance;
    }

    /** @return why it did not work, {@code null} when it did */
    public String getMessage() {
        return message;
    }
}
