package de.hems.types.ticket;

import java.io.Serializable;

/** Where something in a ticket was written. */
public enum TicketSource implements Serializable {

    DISCORD("Discord"),
    GAME("im Spiel"),
    WEB("Website");

    private final String title;

    TicketSource(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
