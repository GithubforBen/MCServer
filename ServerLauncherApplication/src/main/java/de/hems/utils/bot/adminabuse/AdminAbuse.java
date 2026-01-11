package de.hems.utils.bot.adminabuse;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.UUID;

public class AdminAbuse {
    private UUID uuid;
    private String command;
    private String player;
    private boolean hasBeenSent = false;
    private String reason;
    private long time;

    public AdminAbuse(String command, String player, long time) {
        this.command = command;
        this.player = player;
        uuid = UUID.randomUUID();
        this.time = time;
    }

    public void send() {
        String string = Main.getInstance().getConfiguration().getConfig().getString("logging-channel");
        if (string == null) return;
        hasBeenSent = true;
        TextChannel textChannelById = Main.getInstance().getJda().getTextChannelById(string);
        EmbedBuilder embedBuilder = Main.getEmbedBuilder();
        if (reason != null) embedBuilder.setColor(Color.GREEN);
        else embedBuilder.setColor(Color.RED);
        embedBuilder.setTitle("Der spieler " + player + " hat einen command ausgeführt!");
        embedBuilder.addField("Command:", command, true);
        if (reason == null) embedBuilder.addField("Begründung", "Der Spieler hat keinen grund angegeben!", true);
        else embedBuilder.addField("Begründung", reason, true);
        embedBuilder.addField("Player:", player + "\n UUID: " + UUIDFetcher.findUUIDByName(player, true), true);
        textChannelById.sendMessageEmbeds(embedBuilder.build()).addComponents(
                        ActionRow.of(
                                Button.danger("createTicket", "Frage zur begründung stellen(ticket)")))
                .queue();
    }

    public void sendIfNecessary() {
        if (isOverdue()) send();
    }

    public boolean isOverdue() {
        return !hasBeenSent && time < (System.currentTimeMillis() - Duration.ofMinutes(1L).get(ChronoUnit.MILLIS));
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public boolean isHasBeenSent() {
        return hasBeenSent;
    }

    public void setHasBeenSent(boolean hasBeenSent) {
        this.hasBeenSent = hasBeenSent;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
        send();
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
