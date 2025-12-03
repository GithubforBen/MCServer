package de.hems.utils.bot.tickets;

import de.hems.Main;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.configuration.file.YamlConfiguration;

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
    private int ticketId;
    private String response;
    private String title;

    public Ticket(String title, TicketType type, String author, String content, String response) {
        this.type = type;
        this.author = author;
        this.content = content;
        this.status = Status.OPEN;
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        int tickets = 0;
        if (config.contains("tickets")) {
            tickets = config.getStringList("tickets").size();
        }
        ticketId = tickets;
        this.response = response;
        this.title = title;
    }

    public Ticket(String title, TicketType type, String author, String content, Status status, int ticketId, String response) {
        this.type = type;
        this.author = author;
        this.content = content;
        this.status = status;
        this.ticketId = ticketId;
        this.response = response;
        this.title = title;
    }


    public MessageEmbed getEmbed() {
        return Main.getEmbedBuilder().setTitle("Ticket: " + getTitle())
                .setFooter("ID: " + getTicketId())
                .addField("Inhalt: ", getContent(), true)
                .addField("Antwort: ", response, false)
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

    public int getTicketId() {
        return ticketId;
    }

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
