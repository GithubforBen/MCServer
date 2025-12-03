package de.hems.utils.bot.tickets;

import de.hems.Main;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TicketListener extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getCustomId().equals("createTicket")) createTicket(event);
        if (event.getButton().getCustomId().startsWith("mark-")) mark(event);
        if (event.getButton().getCustomId().startsWith("close-")) close(event);
        if (event.getButton().getCustomId().startsWith("respond-")) respond(event);
    }

    private void respond(ButtonInteractionEvent event) {
        Ticket ticket = Tickets.getTicket(Integer.parseInt(event.getButton().getCustomId().split("-")[1]));
        TextInput answer = TextInput.create("answer", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Antwort auf das Ticket")
                .build();
        Modal modal = Modal.create("respondTicketModal-" + ticket.getTicketId(), "Antwort auf Ticket")
                .addComponents(
                        TextDisplay.of("Ticket: " + ticket.getTitle() + "-" + ticket.getTicketId()),
                        TextDisplay.of("Created by: " + ticket.getAuthor() + "#" + event.getJDA().retrieveUserById(ticket.getAuthor()).complete().getAsTag()),
                        TextDisplay.of("Content: " + ticket.getContent()),
                        Label.of("Antwort:", answer)
                ).build();
        event.replyModal(modal).queue();
    }

    private void close(ButtonInteractionEvent event) {
        Ticket ticket = Tickets.getTicket(Integer.parseInt(event.getButton().getCustomId().split("-")[1]));
        ticket.setStatus(Ticket.Status.CLOSED);
        if (update(ticket, event.getJDA(), event.reply("Ticket channel not set!"))) return;
        event.reply("Ticket geschlossen!").queue();
    }

    private void mark(ButtonInteractionEvent event) {
        Ticket ticket = Tickets.getTicket(Integer.parseInt(event.getButton().getCustomId().split("-")[1]));
        ticket.setStatus(Ticket.Status.InProgress);
        Tickets.saveTicket(ticket);
        event.getJDA().retrieveUserById(ticket.getAuthor()).queue(user -> user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(ticket.getEmbed()).queue()));
        String string = Main.getInstance().getConfiguration().getConfig().getString("ticket-channel");
        if (string == null) {
            event.reply("Ticket channel not set!").queue();
            return;
        }
        TextChannel textChannelById = Main.getInstance().getJda().getTextChannelById(string);
        if (update(ticket, event.getJDA(), event.reply("Ticket channel not set!"))) {
            return;
        }
        event.reply("Ticket als in Bearbeitung gekennzeichnet!").queue();
    }

    private void createTicket(ButtonInteractionEvent event) {
        TextInput title = TextInput.create("subject", TextInputStyle.SHORT)
                .setPlaceholder("Subject of this ticket")
                .setMaxLength(100) // or setRequiredRange(10, 100)
                .build();
        StringSelectMenu type = StringSelectMenu.create("type")
                .addOption("Regelverstoß", Ticket.TicketType.REGELN.toString())
                .addOption("Bug", Ticket.TicketType.BUG.toString())
                .addOption("Vorschlag", Ticket.TicketType.SUGGESTION.toString()).build();
        TextInput content = TextInput.create("content", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Subject of this ticket")
                .setMinLength(10)
                .build();
        Modal modal = Modal.create("createTicketModal", "Create Ticket")
                .addComponents(
                        Label.of("Titel:", title),
                        Label.of("Ticket typ", type),
                        Label.of("Informationen:", content),
                        TextDisplay.of("Mit dem click auf \"Absenden\" bestätigst du, dass du im ticket die wahrheit gesagt hast.")
                ).build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().startsWith("respondTicketModal-")) {
            Ticket ticket = Tickets.getTicket(Integer.parseInt(event.getModalId().split("-")[1]));
            ticket.setStatus(Ticket.Status.CLOSED);
            ticket.setResponse(event.getValue("answer").getAsString());
            if (update(ticket, event.getJDA(), event.reply("Ticket channel not set!"))) return;
            event.reply("Antwort erfolgreich!").queue((x) -> {
                x.deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
            });
        }
        if (!event.getModalId().equals("createTicketModal")) return;
        String subject = event.getValue("subject").getAsString();
        Ticket ticket = new Ticket(
                subject,
                Ticket.TicketType.valueOf(event.getValue("type").getAsStringList().getFirst().toUpperCase()),
                event.getUser().getId(),
                event.getValue("content").getAsString(),
                "-"
        );
        Tickets.createTicket(ticket);
        PrivateChannel complete = event.getUser().openPrivateChannel().complete();
        complete.sendMessageEmbeds(ticket.getEmbed()).queue();

        event.reply("Ticket erstellt!").complete().deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
    }

    private boolean update(Ticket ticket, JDA jda, ReplyCallbackAction reply) {
        Tickets.saveTicket(ticket);
        jda.retrieveUserById(ticket.getAuthor()).queue(user -> user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(ticket.getEmbed()).queue()));
        String string = Main.getInstance().getConfiguration().getConfig().getString("ticket-channel");
        if (string == null) {
            reply.queue();
            return true;
        }
        TextChannel textChannelById = Main.getInstance().getJda().getTextChannelById(string);
        new Thread(() -> {
        textChannelById.getGuild().loadMembers().get().stream().filter((x) -> x.hasPermission(Permission.MESSAGE_MANAGE)).forEach((member) -> {
            if (!member.getUser().isBot()) {
                member.getUser().openPrivateChannel().queue((channel) -> channel.sendMessageEmbeds(ticket.getEmbed()).addComponents(
                        ActionRow.of(
                                Button.primary("mark-" + ticket.getTicketId(), "mark ticket as in progress ticket"),
                                Button.primary("close-" + ticket.getTicketId(), "close ticket"),
                                Button.primary("respond-" + ticket.getTicketId(), "respond ticket")
                        )
                ).queue());
            }
        });
        }).start();
        return false;
    }
}
