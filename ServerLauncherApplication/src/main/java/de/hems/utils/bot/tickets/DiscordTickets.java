package de.hems.utils.bot.tickets;

import de.hems.Main;
import de.hems.communication.events.ticket.TicketUpdatedEvent;
import de.hems.types.discord.AccountLink;
import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketMessage;
import de.hems.types.ticket.TicketSource;
import de.hems.types.ticket.TicketStatus;
import de.hems.types.ticket.TicketType;
import de.hems.utils.Configuration;
import de.hems.utils.bot.verification.AccountLinkStore;
import de.hems.utils.ticket.TicketService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tickets on discord.
 * <p>
 * Players open them with the button in the ticket channel - or with the button under an admin action in the
 * logging channel, which opens one about that action. Every ticket gets a thread in the staff channel, where
 * the admins talk to the player: whatever they write there reaches the player, in a DM and in the game
 * ({@code //} at the start keeps a message among the admins). The player answers with the button in their DM,
 * in the game or - if their account is linked - wherever else they like.
 * <p>
 * The old version sent every ticket to every member who could manage messages, as a DM, on every change,
 * and emptied the ticket channel with a bulk delete at every start - which Discord refuses for messages older
 * than two weeks.
 */
public class DiscordTickets extends ListenerAdapter implements TicketService.Listener {

    private static final String PANEL_TITLE = "Tickets";
    private static final String CREATE = "createTicket";
    private static final String CREATE_MODAL = "createTicketModal";
    private static final String PREFIX = "ticket-";
    private static final String REPLY_MODAL = "ticketReplyModal-";

    private final TicketService tickets;
    private final AccountLinkStore links;
    private final Configuration configuration;
    /** The thread of each ticket, once it is known - also while it is being made, so it is made once. */
    private final Map<Integer, CompletableFuture<Resolved>> threads = new ConcurrentHashMap<>();
    /** What a ticket that is being written is about, by discord account, until the form comes back. */
    private final Map<String, String> pendingContext = new ConcurrentHashMap<>();

    /** A ticket's thread, and whether it was only just made - with everything already in it. */
    private record Resolved(ThreadChannel thread, boolean created) {
    }

    public DiscordTickets(TicketService tickets, AccountLinkStore links, Configuration configuration) {
        this.tickets = tickets;
        this.links = links;
        this.configuration = configuration;
        tickets.addListener(this);
    }

    private JDA jda() {
        return Main.getInstance() == null ? null : Main.getInstance().getJda();
    }

    // ---- the channels ---------------------------------------------------------------------------------------

    private TextChannel channel(String key) {
        JDA jda = jda();
        String id = configuration.getConfig().getString(key);
        return jda == null || id == null ? null : jda.getTextChannelById(id);
    }

    private TextChannel staffChannel() {
        return channel("ticket-staff-channel");
    }

    /**
     * Makes sure the ticket channel has the message with the button. Only posts one if none of the last
     * messages is it - nothing is ever deleted.
     */
    public void ensurePanel() {
        TextChannel channel = channel("ticket-channel");
        if (channel == null) return;
        String self = channel.getJDA().getSelfUser().getId();
        channel.getHistory().retrievePast(25).queue(messages -> {
            for (Message message : messages) {
                if (!message.getAuthor().getId().equals(self)) continue;
                for (MessageEmbed embed : message.getEmbeds()) {
                    if (PANEL_TITLE.equals(embed.getTitle())) return;
                }
            }
            channel.sendMessageEmbeds(Main.getEmbedBuilder()
                            .setTitle(PANEL_TITLE)
                            .setDescription("Du hast einen Bug gefunden, willst etwas melden oder vorschlagen oder "
                                    + "hast eine Frage? Klick auf den Knopf und schreib ein Ticket.\n\n"
                                    + "Die Antwort bekommst du per Direktnachricht - und im Spiel mit `/ticket`, "
                                    + "wenn dein Account mit `/verify` verknüpft ist.")
                            .build())
                    .addComponents(ActionRow.of(Button.primary(CREATE, "Ticket schreiben")))
                    .queue();
        }, error -> System.out.println("Could not read the ticket channel: " + error.getMessage()));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String key = switch (event.getName()) {
            case "setticketchannel" -> "ticket-channel";
            case "setticketstaffchannel" -> "ticket-staff-channel";
            default -> null;
        };
        if (key == null) return;
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Dafür brauchst du Administrator-Rechte.").setEphemeral(true).queue();
            return;
        }
        configuration.getConfig().set(key, event.getChannel().getId());
        configuration.save();
        if (key.equals("ticket-channel")) {
            event.reply("Tickets werden jetzt in " + event.getChannel().getAsMention() + " geschrieben."
                    + (staffChannel() == null ? " Setze noch mit `/setticketstaffchannel` den Kanal, in dem die "
                    + "Admins sie bearbeiten." : "")).setEphemeral(true).queue();
            ensurePanel();
        } else {
            // tickets that have no thread yet get one with their next change
            threads.clear();
            event.reply("Jedes Ticket bekommt jetzt einen Thread in " + event.getChannel().getAsMention()
                    + ". Nachrichten dort gehen an den Spieler, außer sie fangen mit `//` an.")
                    .setEphemeral(true).queue();
        }
    }

    // ---- writing a ticket -----------------------------------------------------------------------------------

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals(CREATE)) {
            openCreateForm(event);
            return;
        }
        if (!id.startsWith(PREFIX)) return;
        String[] parts = id.substring(PREFIX.length()).split("-");
        if (parts.length != 2) return;
        int ticketId;
        try {
            ticketId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        TicketData ticket = tickets.getStore().get(ticketId);
        if (ticket == null) {
            event.reply("Dieses Ticket gibt es nicht mehr.").setEphemeral(true).queue();
            return;
        }
        boolean staff = isStaffPlace(event.getChannel());
        boolean own = TicketService.ownedByDiscord(ticket, event.getUser().getId());
        if (!staff && !own) {
            event.reply("Das ist nicht dein Ticket.").setEphemeral(true).queue();
            return;
        }
        // a button in a DM is the player's, even when the player is an admin
        String who = staff ? staffName(event) : ticket.getAuthorName();
        switch (parts[0]) {
            case "reply" -> event.replyModal(Modal.create(REPLY_MODAL + ticketId, "Antwort auf " + ticket.getLabel())
                    .addComponents(
                            TextDisplay.of("**" + TicketData.clip(ticket.getTitle(), 80) + "**"),
                            Label.of("Deine Nachricht", TextInput.create("text", TextInputStyle.PARAGRAPH)
                                    .setMaxLength(TicketData.MAX_TEXT)
                                    .build()))
                    .build()).queue();
            case "close" -> {
                tickets.setStatus(ticketId, TicketStatus.CLOSED, who, staff, TicketSource.DISCORD);
                event.reply("Ticket " + ticket.getLabel() + " ist geschlossen.").setEphemeral(true).queue();
            }
            case "reopen" -> {
                tickets.setStatus(ticketId, TicketStatus.OPEN, who, staff, TicketSource.DISCORD);
                event.reply("Ticket " + ticket.getLabel() + " ist wieder offen.").setEphemeral(true).queue();
            }
            case "claim" -> {
                if (!staff) {
                    event.reply("Das dürfen nur Admins.").setEphemeral(true).queue();
                    return;
                }
                tickets.claim(ticketId, who, TicketSource.DISCORD);
                event.reply("Du bearbeitest jetzt " + ticket.getLabel() + ".").setEphemeral(true).queue();
            }
            default -> {
            }
        }
    }

    private void openCreateForm(ButtonInteractionEvent event) {
        // under an admin action the ticket is about that action - which the log message says
        String context = adminActionOf(event.getMessage());
        if (context != null) pendingContext.put(event.getUser().getId(), context);
        else pendingContext.remove(event.getUser().getId());
        StringSelectMenu.Builder type = StringSelectMenu.create("type");
        for (TicketType value : TicketType.values()) type.addOption(value.getTitle(), value.name());
        type.setDefaultValues(context != null ? TicketType.ADMIN_ACTION.name() : TicketType.QUESTION.name());
        List<net.dv8tion.jda.api.components.ModalTopLevelComponent> parts = new ArrayList<>();
        if (context != null) parts.add(TextDisplay.of("Es geht um: " + context));
        parts.add(Label.of("Worum geht es?", type.build()));
        parts.add(Label.of("Titel", TextInput.create("subject", TextInputStyle.SHORT)
                .setMaxLength(TicketData.MAX_TITLE)
                .build()));
        parts.add(Label.of("Beschreibung", TextInput.create("content", TextInputStyle.PARAGRAPH)
                .setMinLength(10)
                .setMaxLength(TicketData.MAX_TEXT)
                .build()));
        parts.add(TextDisplay.of("Mit \"Absenden\" bestätigst du, dass du im Ticket die Wahrheit sagst."));
        event.replyModal(Modal.create(CREATE_MODAL, "Ticket schreiben").addComponents(parts).build()).queue();
    }

    /**
     * @param message a message a button was pressed under
     * @return what the admin action in it was, or {@code null} if it is not the log of one
     */
    private static String adminActionOf(Message message) {
        if (message == null) return null;
        for (MessageEmbed embed : message.getEmbeds()) {
            String command = null;
            String player = null;
            for (MessageEmbed.Field field : embed.getFields()) {
                if ("Command:".equals(field.getName())) command = field.getValue();
                if ("Player:".equals(field.getName()) && field.getValue() != null) {
                    player = field.getValue().split("\n")[0].trim();
                }
            }
            if (command != null) return TicketData.clip((player == null ? "" : player + ": ") + command, 200);
        }
        return null;
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.equals(CREATE_MODAL)) {
            create(event);
        } else if (id.startsWith(REPLY_MODAL)) {
            reply(event, id.substring(REPLY_MODAL.length()));
        }
    }

    private void create(ModalInteractionEvent event) {
        ModalMapping subject = event.getValue("subject");
        ModalMapping content = event.getValue("content");
        ModalMapping typeValue = event.getValue("type");
        if (subject == null || content == null) {
            event.reply("Das Formular war unvollständig.").setEphemeral(true).queue();
            return;
        }
        TicketType type = TicketType.OTHER;
        if (typeValue != null && !typeValue.getAsStringList().isEmpty()) {
            type = TicketType.byName(typeValue.getAsStringList().getFirst());
        }
        String discordId = event.getUser().getId();
        AccountLink link = links.byDiscord(discordId);
        String name = link != null ? link.getMinecraftName()
                : event.getMember() != null ? event.getMember().getEffectiveName() : event.getUser().getName();
        TicketData ticket = tickets.create(type, subject.getAsString(), content.getAsString(), name,
                null, discordId, pendingContext.remove(discordId), TicketSource.DISCORD);
        event.reply("Ticket " + ticket.getLabel() + " ist angelegt. Antworten bekommst du per Direktnachricht"
                + (link != null ? " und im Spiel mit `/ticket`." : " - verknüpfe deinen Minecraft-Account mit "
                + "`/verify`, dann siehst du sie auch im Spiel.")).setEphemeral(true).queue();
    }

    private void reply(ModalInteractionEvent event, String rawId) {
        int ticketId;
        try {
            ticketId = Integer.parseInt(rawId);
        } catch (NumberFormatException e) {
            return;
        }
        ModalMapping text = event.getValue("text");
        TicketData ticket = tickets.getStore().get(ticketId);
        if (ticket == null || text == null) {
            event.reply("Dieses Ticket gibt es nicht mehr.").setEphemeral(true).queue();
            return;
        }
        boolean staff = isStaffPlace(event.getChannel());
        if (!staff && !TicketService.ownedByDiscord(ticket, event.getUser().getId())) {
            event.reply("Das ist nicht dein Ticket.").setEphemeral(true).queue();
            return;
        }
        String who = staff ? staffName(event) : ticket.getAuthorName();
        tickets.reply(ticketId, who, staff, text.getAsString(), TicketSource.DISCORD);
        event.reply("Gesendet.").setEphemeral(true).queue();
    }

    // ---- the admins' threads --------------------------------------------------------------------------------

    /**
     * A message in a ticket's thread is an answer to the player, unless it starts with {@code //}.
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || event.isWebhookMessage()) return;
        if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD
                && event.getChannelType() != ChannelType.GUILD_PRIVATE_THREAD) return;
        TicketData ticket = tickets.getStore().byThread(event.getChannel().getId());
        if (ticket == null) return;
        String text = event.getMessage().getContentDisplay().trim();
        if (text.isEmpty() || text.startsWith("//")) return;
        String who = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        if (tickets.reply(ticket.getId(), who, true, text, TicketSource.DISCORD) != null) {
            event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue(ok -> {
            }, error -> {
            });
        }
    }

    /**
     * @param channel where a button was pressed or a form sent
     * @return whether that is among the admins: the staff channel or one of its threads - also an archived
     * one, which is why the channel of the interaction is asked and not the cache
     */
    private boolean isStaffPlace(MessageChannelUnion channel) {
        TextChannel staff = staffChannel();
        if (staff == null || channel == null) return false;
        if (channel.getId().equals(staff.getId())) return true;
        if (!channel.getType().isThread()) return false;
        return channel.asThreadChannel().getParentChannel().getId().equals(staff.getId());
    }

    /**
     * @return the name an admin signs with: their name on the server
     */
    private static String staffName(net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent event) {
        return event.getMember() != null ? event.getMember().getEffectiveName() : event.getUser().getName();
    }

    // ---- passing changes on ---------------------------------------------------------------------------------

    @Override
    public void changed(TicketData ticket, TicketUpdatedEvent.Change change, TicketSource source, boolean byStaff) {
        if (jda() == null) return;
        toThread(ticket, change, source);
        toAuthor(ticket, change, byStaff);
    }

    private void toThread(TicketData ticket, TicketUpdatedEvent.Change change, TicketSource source) {
        if (staffChannel() == null) return;
        TicketMessage last = ticket.getMessages().isEmpty() ? null : ticket.getMessages().getLast();
        thread(ticket).thenAccept(resolved -> {
            ThreadChannel thread = resolved.thread();
            // a thread that was only just made already holds everything
            if (resolved.created()) {
                if (ticket.getStatus() == TicketStatus.CLOSED) thread.getManager().setArchived(true).queue();
                return;
            }
            Runnable post = switch (change) {
                case CREATED -> null;
                case STAFF_REPLY -> source == TicketSource.DISCORD ? null
                        : () -> thread.sendMessageEmbeds(messageEmbed(last)).queue();
                case PLAYER_REPLY -> () -> thread.sendMessageEmbeds(messageEmbed(last)).queue();
                case STATUS -> () -> thread.sendMessage("Status: **" + ticket.getStatus().getTitle() + "**").queue();
                case CLAIMED -> () -> thread.sendMessage("**" + ticket.getAssignee() + "** bearbeitet das Ticket.").queue();
            };
            if (post == null) return;
            if (thread.isArchived()) {
                thread.getManager().setArchived(false).queue(ok -> post.run(), error -> post.run());
            } else {
                post.run();
            }
            if (ticket.getStatus() == TicketStatus.CLOSED) {
                thread.getManager().setArchived(true).queueAfter(2, java.util.concurrent.TimeUnit.SECONDS);
            }
        }).exceptionally(error -> {
            System.out.println("Could not reach the thread of ticket " + ticket.getLabel() + ": " + error.getMessage());
            return null;
        });
    }

    /**
     * Finds the ticket's thread - in the cache, among the archived ones, or by making it.
     */
    private CompletableFuture<Resolved> thread(TicketData ticket) {
        CompletableFuture<Resolved> known = threads.get(ticket.getId());
        if (known != null && !known.isCompletedExceptionally()) {
            return known.thenApply(resolved -> new Resolved(resolved.thread(), false));
        }
        CompletableFuture<Resolved> future = new CompletableFuture<>();
        threads.put(ticket.getId(), future);
        TextChannel staff = staffChannel();
        String threadId = ticket.getThreadId();
        ThreadChannel cached = threadId == null ? null : staff.getJDA().getThreadChannelById(threadId);
        if (cached != null) {
            future.complete(new Resolved(cached, false));
        } else if (threadId != null) {
            // archived threads are not in the cache
            staff.retrieveArchivedPublicThreadChannels().takeAsync(200).thenAccept(archived -> {
                for (ThreadChannel thread : archived) {
                    if (thread.getId().equals(threadId)) {
                        future.complete(new Resolved(thread, false));
                        return;
                    }
                }
                create(staff, ticket, future);
            }).exceptionally(error -> {
                create(staff, ticket, future);
                return null;
            });
        } else {
            create(staff, ticket, future);
        }
        future.whenComplete((resolved, error) -> {
            if (error != null) threads.remove(ticket.getId(), future);
        });
        return future;
    }

    private void create(TextChannel staff, TicketData ticket, CompletableFuture<Resolved> future) {
        staff.createThreadChannel(TicketData.clip(ticket.getLabel() + " · " + ticket.getTitle(), 100))
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .queue(thread -> {
                    tickets.setThread(ticket.getId(), thread.getId());
                    thread.sendMessageEmbeds(header(ticket)).addComponents(ActionRow.of(
                            Button.primary(PREFIX + "claim-" + ticket.getId(), "Übernehmen"),
                            Button.danger(PREFIX + "close-" + ticket.getId(), "Schließen"),
                            Button.secondary(PREFIX + "reopen-" + ticket.getId(), "Wieder öffnen"))).queue();
                    // the first message is in the header; the rest of the conversation so far follows it
                    List<TicketMessage> messages = ticket.getMessages();
                    for (int i = 1; i < messages.size(); i++) thread.sendMessageEmbeds(messageEmbed(messages.get(i))).queue();
                    future.complete(new Resolved(thread, true));
                }, future::completeExceptionally);
    }

    private static MessageEmbed header(TicketData ticket) {
        EmbedBuilder embed = Main.getEmbedBuilder()
                .setTitle(TicketData.clip(ticket.getLabel() + " · " + ticket.getTitle(), 256))
                .setDescription(ticket.getDescription())
                .addField("Art", ticket.getType().getTitle(), true)
                .addField("Von", ticket.getAuthorName()
                        + (ticket.getDiscordId() != null ? " (<@" + ticket.getDiscordId() + ">)" : ""), true)
                .addField("Status", ticket.getStatus().getTitle(), true)
                .setFooter("Nachrichten hier gehen an den Spieler. Mit // am Anfang bleiben sie unter euch.");
        if (ticket.getContext() != null) embed.addField("Bezieht sich auf", ticket.getContext(), false);
        if (!ticket.getMessages().isEmpty()) {
            embed.addField("Geschrieben", ticket.getMessages().getFirst().getSource().getTitle(), true);
        }
        return embed.build();
    }

    private static MessageEmbed messageEmbed(TicketMessage message) {
        return new EmbedBuilder()
                .setAuthor(message.getAuthor() + (message.isStaff() ? " (Admin)" : "") + " · "
                        + message.getSource().getTitle())
                .setDescription(message.getText())
                .setColor(message.isStaff() ? new Color(0x2E86DE) : new Color(0x27AE60))
                .setTimestamp(Instant.ofEpochMilli(message.getAt()))
                .build();
    }

    /**
     * The player hears about a new ticket, an answer and a change of status - by DM, if their discord
     * account is known.
     */
    private void toAuthor(TicketData ticket, TicketUpdatedEvent.Change change, boolean byStaff) {
        if (ticket.getDiscordId() == null) return;
        EmbedBuilder embed = Main.getEmbedBuilder().setTitle(TicketData.clip(ticket.getLabel() + " · " + ticket.getTitle(), 256));
        switch (change) {
            case CREATED -> embed.setDescription("Dein Ticket ist angelegt. Die Admins melden sich hier.")
                    .addField("Du hast geschrieben", ticket.getDescription(), false);
            case STAFF_REPLY -> {
                TicketMessage last = ticket.getMessages().getLast();
                embed.setDescription(last.getText()).setAuthor(last.getAuthor() + " hat geantwortet");
            }
            case STATUS -> embed.setDescription("Dein Ticket ist jetzt **" + ticket.getStatus().getTitle() + "**."
                    + (ticket.getStatus() == TicketStatus.CLOSED ? " Schreib einfach noch einmal, falls doch "
                    + "noch etwas ist - dann ist es wieder offen." : ""));
            default -> {
                return;
            }
        }
        // a player who closed or reopened their ticket knows that already
        if (change == TicketUpdatedEvent.Change.STATUS && !byStaff) return;
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.primary(PREFIX + "reply-" + ticket.getId(), "Antworten"));
        buttons.add(ticket.getStatus() == TicketStatus.CLOSED
                ? Button.secondary(PREFIX + "reopen-" + ticket.getId(), "Wieder öffnen")
                : Button.danger(PREFIX + "close-" + ticket.getId(), "Schließen"));
        MessageEmbed built = embed.build();
        jda().retrieveUserById(ticket.getDiscordId()).queue(user -> user.openPrivateChannel().queue(
                channel -> channel.sendMessageEmbeds(built).addComponents(ActionRow.of(buttons)).queue(ok -> {
                }, error -> System.out.println("Could not DM the author of " + ticket.getLabel()
                        + ": " + error.getMessage())),
                error -> {
                }), error -> {
        });
    }
}
