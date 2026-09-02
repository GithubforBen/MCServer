package de.hems.communication.events.money;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Announces that one account changed, so every server's copy follows without anybody polling.
 */
public class BalanceUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4506L;

    private String holder;
    private int balance;

    public BalanceUpdatedEvent() {
    }

    public BalanceUpdatedEvent(String holder, int balance) {
        super(ListenerAdapter.ServerName.ALL);
        this.holder = holder;
        this.balance = balance;
    }

    public String getHolder() {
        return holder;
    }

    public int getBalance() {
        return balance;
    }
}
