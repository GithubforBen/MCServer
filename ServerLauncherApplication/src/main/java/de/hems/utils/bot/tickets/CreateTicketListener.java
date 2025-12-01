package de.hems.utils.bot.tickets;

import de.hems.Main;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class CreateTicketListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getButton().getCustomId().equals("createTicket")) return;
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
        if (!event.getModalId().equals("createTicketModal")) return;
        String type = event.getValue("type").getAsStringList().getFirst();
        Ticket ticket = new Ticket(
                Ticket.TicketType.valueOf(type),
                event.getUser().getId(),
                event.getValue("content").getAsString()
        );
        Tickets.createTicket(ticket);
        PrivateChannel complete = event.getUser().openPrivateChannel().complete();
        complete.sendMessageEmbeds(Main.getEmbedBuilder().setTitle("Ticket erstellt")
                .setFooter("ID: " + ticket.getTicketId().toString())
                .addField("Inhalt: ", ticket.getContent(), true)
                .addField("Derzeitiger Status:", ticket.getStatus().toString() + "\n Status updates werden automatisch gesendet.", true).build()).queue();

        event.reply("Ticket erstellt!").complete().deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
    }
}
