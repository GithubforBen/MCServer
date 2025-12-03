package de.hems.utils.bot.tickets;

import de.hems.Main;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.UUID;

public class Ticket {
    enum TicketType {
        BUG,
        REGELN,
        SUGGESTION
    }
    enum Status {
        OPEN,
        InProgress,
        CLOSED
    }
    private TicketType type;
    private String author;
    private String content;
    private Status status;
    private UUID ticketId;

    public Ticket(TicketType type, String author, String content) {
        this.type = type;
        this.author = author;
        this.content = content;
        this.status = Status.OPEN;
        ticketId = UUID.randomUUID();
    }

    public Ticket(TicketType type, String author, String content, Status status, UUID ticketId) {
        this.type = type;
        this.author = author;
        this.content = content;
        this.status = status;
        this.ticketId = ticketId;
    }

    public MessageEmbed getEmbed() {
        return Main.getEmbedBuilder().setTitle("Ticket erstellt")
                .setFooter("ID: " + getTicketId().toString())
                .addField("Inhalt: ", getContent(), true)
                .addField("Derzeitiger Status:", getStatus().toString() + "\n Status updates werden automatisch gesendet.", true).build();
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }
}
