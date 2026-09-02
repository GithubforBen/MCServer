package de.hems;

import de.hems.api.UUIDFetcher;
import de.hems.communication.ListenerAdapter;
import de.hems.events.*;
import de.hems.types.FileType;
import de.hems.types.MissingConfigurationException;
import de.hems.types.ServerTemplate;
import de.hems.utils.Configuration;
import de.hems.utils.admin.StashStore;
import de.hems.utils.event.AwardStore;
import de.hems.utils.event.EventSettlement;
import de.hems.utils.event.EventStore;
import de.hems.utils.event.RunStore;
import de.hems.utils.money.MoneyStore;
import de.hems.utils.team.BackpackStore;
import de.hems.utils.team.TeamStore;
import de.hems.utils.bot.adminabuse.*;
import de.hems.utils.bot.payingplayer.PayingPlayerCommand;
import de.hems.utils.bot.tickets.TicketListener;
import de.hems.utils.bot.tickets.SetTicketChannelListener;
import de.hems.utils.bot.tickets.Tickets;
import de.hems.utils.bot.verification.OnAccountVerifyCommand;
import de.hems.utils.server.IdleServerWatchdog;
import de.hems.utils.server.MemoryWatch;
import de.hems.utils.server.ServerHandler;
import de.hems.utils.types.RunningMode;
import de.hems.utils.webconsole.WebServer;
import de.hems.utils.webconsole.modules.StashModule;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Main {
    private static Main instance;
    private Configuration configuration;
    private ListenerAdapter listenerAdapter;
    private ServerHandler serverHandler;
    private TeamStore teamStore;
    private BackpackStore backpackStore;
    private StashStore stashStore;
    private EventStore eventStore;
    private RunStore runStore;
    private AwardStore awardStore;
    private MoneyStore moneyStore;
    private JDA jda;
    private WebServer webServer;
    private IdleServerWatchdog idleServerWatchdog;
    private MemoryWatch memoryWatch;
    //TODO: add a way to auto add ops

    public Main() throws Exception {
        System.out.println(System.getProperty("os.name"));
        if (instance == null) {
            instance = this;
        } else {
            throw new IllegalStateException("Already initialized");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                onShutdown();
            } catch (IOException ignored) {
            }
        }));
        configuration = new Configuration();
        System.out.println(getIp());
        if (!configuration.getConfig().contains("paying-players")) {
            UUID owner = UUIDFetcher.findUUIDByName("for_sale", true);
            // mojang may be unreachable - an empty list is better than not starting at all
            configuration.getConfig().set("paying-players",
                    owner == null ? List.of() : List.of(owner.toString()));
        }
        listenerAdapter = new ListenerAdapter(ListenerAdapter.ServerName.HOST);
        new RespondDataEvent();
        teamStore = new TeamStore();
        backpackStore = new BackpackStore();
        new TeamEvents(teamStore, backpackStore);
        stashStore = new StashStore();
        new StashEvents(stashStore);
        // the money of the network belongs here as well: it used to sit next to the survival
        // server, where only that one server could see it and nothing else could spend it
        moneyStore = new MoneyStore();
        new MoneyEvents(moneyStore);
        eventStore = new EventStore();
        runStore = new RunStore();
        awardStore = new AwardStore();
        new EventEvents(eventStore, runStore, awardStore, new EventSettlement(eventStore, runStore, awardStore));
        new AdminAbuseHandler();
        serverHandler = new ServerHandler();
        // what the machine has left, and which server is sitting on memory it never uses
        memoryWatch = new MemoryWatch(serverHandler);
        memoryWatch.start();
        new CapacityEvents(memoryWatch);
        new StartServerEvent();
        new RestartServerEvent();
        new StopServerEvent();
        new RequestAdminAbuse();
        new LegitimiseAdminAbuse();
        new RequestServerDataEvent();
        new RequestToLegitimise();
        if (configuration.getConfig().contains("discord-token")) {
            jda = JDABuilder.createDefault(configuration.getConfig().getString("discord-token"))
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(
                            new SetTicketChannelListener(),
                            new TicketListener(),
                            new OnAccountVerifyCommand(),
                            new PayingPlayerCommand(),
                            new SetLoggingChannel())
                    .setActivity(Activity.playing("Playing on " + getIp()))
                    .build();
            jda.awaitReady();
            jda.updateCommands().addCommands(Commands.slash("payingplayer", "Schreibe auf, dass ein spieler für den Server zahlt!").addOption(OptionType.STRING, "minecraftname", "Den Minecraft name hier einfügen.", true))
                    .addCommands(
                            Commands.slash("setticketchannel", "Set the channel for tickets").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)
                            ))
                    .addCommands(
                            Commands.slash("setloggingchannel", "Set the channel for admin abuse logging").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)
                            ))
                    .addCommands(Commands.slash("verify", "Verbinde deinen account mit deinem Minecraft account!").addOption(OptionType.STRING, "minecraftname", "Dein Minecraft name hier einfügen.", true))
                    .queue();
        } else {
            configuration.getConfig().set("discord-token", "<<add token here>>");
            configuration.getConfig().setComments("discord-token", List.of("The discord token to use for the bot!"));
            configuration.save();
            throw new MissingConfigurationException("discord-token is missing in config.yml.");
        }
        startConfiguredServers();
        // servers created for an event are nobody's job to clean up, so the launcher does it
        idleServerWatchdog = new IdleServerWatchdog(serverHandler);
        startWebServer();
        if (jda != null) Tickets.updateTicketChannel();
    }

    /**
     * Starts the admin website unless it is switched off in the config. A website that fails to come up
     * must not take the whole network with it, so problems are logged instead of thrown.
     */
    private void startWebServer() {
        if (!configuration.getConfig().getBoolean("web.enabled", true)) {
            System.out.println("The admin website is disabled (web.enabled).");
            return;
        }
        try {
            webServer = new WebServer(configuration, new StashModule(stashStore));
        } catch (RuntimeException e) {
            System.out.println("Could not start the admin website: " + e.getMessage());
        }
    }

    /**
     * Starts the servers that should come up with the network. Which ones those are is configurable, so a
     * network can boot with any number of servers - everything else is started later through the in-game
     * manager or {@link de.hems.api.ServerApi}.
     */
    private void startConfiguredServers() throws Exception {
        YamlConfiguration config = configuration.getConfig();
        if (!config.contains("whitelist")) {
            config.set("whitelist", List.of("for_Sale", "SA_MI"));
            config.setComments("whitelist", List.of("Players that are put onto the whitelist of every server."));
        }
        if (!config.contains("autostart")) {
            config.set("autostart", List.of(
                    ListenerAdapter.ServerName.LOBBY.toString(),
                    ListenerAdapter.ServerName.SURVIVAL.toString()));
            config.setComments("autostart", List.of("Servers that are started together with the network."));
        }
        configuration.save();
        for (String name : config.getStringList("autostart")) {
            ListenerAdapter.ServerName serverName = ListenerAdapter.ServerName.valueOf(name);
            ServerTemplate template = ServerTemplate.forServerName(serverName.toString());
            int memory = config.getInt("servers." + serverName + ".memory", template.getDefaultMemoryMB());
            try {
                serverHandler.startNewInstance(serverName, template, memory, new FileType.PLUGIN[0]);
            } catch (Exception e) {
                System.out.println("Could not start " + serverName + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main();
    }

    public static Main getInstance() {
        return instance;
    }

    public static EmbedBuilder getEmbedBuilder() {
        return new EmbedBuilder().setAuthor("The Server Team")
                .setColor(new Color(255, 0, 0, 255))
                .setTimestamp(Instant.now());
    }

    public void onShutdown() throws IOException {
        System.out.println("Shutting down...");
        configuration.save(); //neccessary
        if (idleServerWatchdog != null) idleServerWatchdog.stop();
        if (webServer != null) webServer.stop();
        serverHandler.shutdownNetwork();
        configuration.save();
        if (jda != null) jda.shutdownNow();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ListenerAdapter getListenerAdapter() {
        return listenerAdapter;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public TeamStore getTeamStore() {
        return teamStore;
    }

    public BackpackStore getBackpackStore() {
        return backpackStore;
    }

    public EventStore getEventStore() {
        return eventStore;
    }

    public RunStore getRunStore() {
        return runStore;
    }

    public AwardStore getAwardStore() {
        return awardStore;
    }

    public MoneyStore getMoneyStore() {
        return moneyStore;
    }

    public MemoryWatch getMemoryWatch() {
        return memoryWatch;
    }

    public StashStore getStashStore() {
        return stashStore;
    }

    public String getIp() throws IOException {
        YamlConfiguration config = configuration.getConfig();
        RunningMode mode = RunningMode.LOCAL;
        if (config.contains("running-mode")) {
            try {
                mode = RunningMode.valueOf(config.getString("running-mode"));
            } catch (Exception e) {
                config.set("running-mode", RunningMode.LOCAL.toString());
            }
        } else {
            config.set("running-mode", RunningMode.LOCAL.toString());
        }
        switch (mode) {
            case LOCAL -> {
                return "localhost";
            }
            case LOCALIP -> {
                return InetAddress.getLocalHost().getHostAddress();
            }
            case PUBLIC -> {
                if (config.contains("server-ip")) {
                    return config.get("server-ip").toString();
                }

                URL ip = new URL("http://checkip.amazonaws.com");
                BufferedReader reader = new BufferedReader(new InputStreamReader(ip.openStream()));
                return reader.readLine();
            }
        }
        throw new IllegalStateException("Unknown running mode");
    }

    public WebServer getWebServer() {
        return webServer;
    }

    public JDA getJda() {
        return jda;
    }
}