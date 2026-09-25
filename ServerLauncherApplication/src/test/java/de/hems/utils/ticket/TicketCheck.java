package de.hems.utils.ticket;

import de.hems.communication.events.ticket.TicketUpdatedEvent;
import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketSource;
import de.hems.types.ticket.TicketStatus;
import de.hems.types.ticket.TicketType;
import de.hems.utils.Configuration;
import de.hems.utils.bot.verification.AccountLinkStore;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checks the ticket store, the conversation rules and the move of the old tickets.
 * <p>
 * Like {@code RewardCheck}, a class with a main. Run it from an empty directory - the migration check writes
 * a {@code main-config.yml} into the current one:
 * <pre>
 * mvn -q -pl ServerLauncherApplication -am install -DskipTests
 * mvn -q -pl ServerLauncherApplication dependency:build-classpath -Dmdep.outputFile=cp.txt
 * javac -cp "$(cat ServerLauncherApplication/cp.txt):ServerLauncherApplication/target/classes" \
 *       -d /tmp/tc ServerLauncherApplication/src/test/java/de/hems/utils/ticket/TicketCheck.java
 * mkdir -p /tmp/tc-run &amp;&amp; cd /tmp/tc-run &amp;&amp; java -cp "...:/tmp/tc" de.hems.utils.ticket.TicketCheck
 * </pre>
 * Exits non-zero when something is wrong. Without a network, "Could not announce" lines are expected.
 */
public final class TicketCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        conversation();
        numbering();
        copies();
        migration();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static File temp(String name) throws Exception {
        File file = File.createTempFile(name, ".yml");
        file.delete();
        return file;
    }

    private static void conversation() throws Exception {
        File file = temp("tickets");
        AccountLinkStore links = new AccountLinkStore(temp("links"));
        UUID player = UUID.randomUUID();
        String code = links.startLink("111", "disc", player, "Steve");
        links.confirm(player, "Steve", code);

        TicketService service = new TicketService(new TicketStore(file), links);
        List<String> heard = new ArrayList<>();
        service.addListener((ticket, change, source, byStaff) -> heard.add(change + "/" + byStaff));

        TicketData ticket = service.create(TicketType.BUG, "Kiste weg", "Meine Kiste ist weg", "Steve",
                player, null, null, TicketSource.GAME);
        check("the discord half comes from the link", ticket.getDiscordId(), "111");
        check("a new ticket is open", ticket.getStatus(), TicketStatus.OPEN);
        check("the author has seen their own text", ticket.getUnseenAnswers(), 0);

        service.reply(ticket.getId(), "Admin", true, "Schaue ich mir an", TicketSource.WEB);
        TicketData now = service.getStore().get(ticket.getId());
        check("an answer puts it in progress", now.getStatus(), TicketStatus.IN_PROGRESS);
        check("and gives it to whoever answered", now.getAssignee(), "Admin");
        check("the author has an unread answer", now.getUnseenAnswers(), 1);
        check("a player reaches their ticket by account", service.of(player).size(), 1);

        service.markSeen(ticket.getId());
        check("reading it clears that", service.getStore().get(ticket.getId()).getUnseenAnswers(), 0);

        service.setStatus(ticket.getId(), TicketStatus.CLOSED, "Admin", true, TicketSource.DISCORD);
        check("closed", service.getStore().get(ticket.getId()).getStatus(), TicketStatus.CLOSED);
        check("closed tickets are not open", service.open().size(), 0);
        service.reply(ticket.getId(), "Steve", false, "Doch noch was", TicketSource.DISCORD);
        check("the player writing again opens it", service.getStore().get(ticket.getId()).getStatus(), TicketStatus.OPEN);
        check("an empty reply is nothing", service.reply(ticket.getId(), "Steve", false, "  ", TicketSource.GAME), null);
        check("a reply to no ticket is nothing", service.reply(999, "Steve", false, "x", TicketSource.GAME), null);
        check("setting a status twice says nothing the second time",
                heard.size() == countAfter(service, ticket.getId(), heard), true);

        check("listeners hear who did it", heard.toString(),
                "[CREATED/false, STAFF_REPLY/true, STATUS/true, PLAYER_REPLY/false]");

        TicketStore reread = new TicketStore(file);
        TicketData back = reread.get(ticket.getId());
        check("the conversation survives a restart", back.getMessages().size(), 3);
        check("with who wrote it", back.getMessages().get(1).isStaff(), true);
        check("and where", back.getMessages().get(1).getSource(), TicketSource.WEB);
        check("and the account", back.getMinecraftId(), player);
        check("and what was seen", back.getSeenByAuthor(), 3);
        file.delete();
    }

    private static int countAfter(TicketService service, int id, List<String> heard) {
        TicketStatus status = service.getStore().get(id).getStatus();
        service.setStatus(id, status, "x", true, TicketSource.WEB);
        return heard.size();
    }

    private static void numbering() throws Exception {
        File file = temp("numbers");
        TicketService service = new TicketService(new TicketStore(file), new AccountLinkStore(temp("links")));
        int a = service.create(TicketType.OTHER, "a", "a", "x", null, "1", null, TicketSource.DISCORD).getId();
        int b = service.create(TicketType.OTHER, "b", "b", "x", null, "1", null, TicketSource.DISCORD).getId();
        check("two tickets get two numbers", a != b, true);
        TicketService again = new TicketService(new TicketStore(file), new AccountLinkStore(temp("links")));
        int c = again.create(TicketType.OTHER, "c", "c", "x", null, "1", null, TicketSource.DISCORD).getId();
        check("numbers carry on after a restart", c > b, true);
        check("a title is cut to size", again.create(TicketType.OTHER, "x".repeat(300), "t", "x", null, "1", null,
                TicketSource.DISCORD).getTitle().length() <= TicketData.MAX_TITLE, true);
        file.delete();
    }

    private static void copies() throws Exception {
        File file = temp("copies");
        TicketStore store = new TicketStore(file);
        TicketService service = new TicketService(store, new AccountLinkStore(temp("links")));
        TicketData ticket = service.create(TicketType.OTHER, "t", "text", "x", null, "1", null, TicketSource.DISCORD);
        TicketData outside = store.get(ticket.getId());
        outside.setStatus(TicketStatus.CLOSED);
        outside.getMessages().clear();
        check("changing a ticket that was handed out does not change the stored one",
                store.get(ticket.getId()).getStatus(), TicketStatus.OPEN);
        check("nor its messages", store.get(ticket.getId()).getMessages().size(), 1);
        file.delete();
    }

    private static void migration() throws Exception {
        File config = new File("main-config.yml");
        if (config.exists()) throw new IllegalStateException("run this from an empty directory");
        Files.writeString(config.toPath(), """
                discord-token: abc
                tickets:
                - 0
                - 1
                ticket-0:
                  title: Alt
                  uuid: 0
                  type: REGELN
                  description: Jemand griefed
                  status: InProgress
                  author: '222'
                  response: '-'
                ticket-1:
                  title: Beantwortet
                  uuid: 1
                  type: NACHFRAGE
                  description: Wie geht das?
                  status: CLOSED
                  author: '333'
                  response: So geht das.
                """);
        try {
            Configuration configuration = new Configuration();
            File file = temp("migrated");
            TicketStore store = new TicketStore(file);
            AccountLinkStore links = new AccountLinkStore(temp("links"));
            UUID player = UUID.randomUUID();
            links.confirm(player, "Alex", links.startLink("333", "alex", player, "Alex"));
            TicketMigration.run(configuration, store, links);

            TicketData first = store.get(0);
            check("an old ticket keeps its number", first != null, true);
            check("its type is translated", first.getType(), TicketType.RULES);
            check("its status too", first.getStatus(), TicketStatus.IN_PROGRESS);
            check("an old ticket without an answer has one message", first.getMessages().size(), 1);
            TicketData second = store.get(1);
            check("the answer becomes a message", second.getMessages().size(), 2);
            check("from an admin", second.getMessages().get(1).isStaff(), true);
            check("a linked author gets their name", second.getAuthorName(), "Alex");
            check("and their account", second.getMinecraftId(), player);
            check("the old keys are gone", configuration.getConfig().contains("ticket-0"), false);
            check("the list too", configuration.getConfig().contains("tickets"), false);
            check("the rest of the config stays", configuration.getConfig().getString("discord-token"), "abc");
            check("a new ticket gets a new number", store.nextId() > 1, true);
            TicketMigration.run(new Configuration(), store, links);
            check("running it again changes nothing", store.all().size(), 2);
            file.delete();
        } finally {
            config.delete();
        }
    }

    private static void check(String what, Object actual, Object expected) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
            return;
        }
        failed++;
        System.out.println("FAIL " + what + ": expected " + expected + ", got " + actual);
    }
}
