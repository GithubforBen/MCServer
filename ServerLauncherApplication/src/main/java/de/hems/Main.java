package de.hems;

import de.hems.communication.ListenerAdapter;
import de.hems.events.*;
import de.hems.types.FileType;
import de.hems.utils.Configuration;
import de.hems.utils.bot.tickets.TicketListener;
import de.hems.utils.bot.tickets.SetTicketChannelListener;
import de.hems.utils.bot.tickets.Tickets;
import de.hems.utils.bot.verification.OnAccountVerifyCommand;
import de.hems.utils.server.ServerHandler;
import de.hems.utils.types.RunningMode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.time.Instant;
import java.util.List;

public class Main {
    private static Main instance;
    private Configuration configuration;
    private ListenerAdapter listenerAdapter;
    private ServerHandler serverHandler;
    private JDA jda;

    public Main() throws Exception {
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
        listenerAdapter = new ListenerAdapter(ListenerAdapter.ServerName.HOST);
        new RespondDataEvent();
        serverHandler = new ServerHandler();
        new StartServerEvent();
        new RestartServerEvent();
        new StopServerEvent();
        new RequestServerDataEvent();
        if (configuration.getConfig().contains("discord-token")) {
            jda = JDABuilder.createDefault(configuration.getConfig().getString("discord-token"))
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                    .addEventListeners(
                            new SetTicketChannelListener(),
                            new TicketListener(),
                            new OnAccountVerifyCommand())
                    .setActivity(Activity.playing("Playing on " + getIp()))
                    .build();
            jda.awaitReady();
            CommandListUpdateAction commandListUpdateAction = jda.updateCommands();
            commandListUpdateAction.addCommands(
                    Commands.slash("setticketchannel", "Set the channel for tickets").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS)
                    ));
            commandListUpdateAction.addCommands(Commands.slash("verify", "Verbinde deinen account mit deinem Minecraft account!").addOption(OptionType.STRING, "minecraftname", "Dein Minecraft name hier einfügen.", true));
            commandListUpdateAction.queue();
        } else {
            configuration.getConfig().set("discord-token", "<<add token here>>");
            configuration.getConfig().setComments("discord-token", List.of("The discord token to use for the bot!"));
            configuration.save();
            System.out.println("Please add your discord token to the config.yml");
            System.exit(0);
            return;
        }
        serverHandler.startNewInstance(
                ListenerAdapter.ServerName.SURVIVAL.toString(), 4000, FileType.SERVER.PAPER, 25565, false, new FileType.PLUGIN[]{FileType.PLUGIN.WORLDEDIT, FileType.PLUGIN.SIMPLE_VOICECHAT});
        //serverHandler.startNewInstance(
        //        ListenerAdapter.ServerName.LOBBY.toString(), 4000, FileType.SERVER.PAPER, 25555, false, new FileType.PLUGIN[]{FileType.PLUGIN.WORLDEDIT});
        if (jda != null) Tickets.updateTicketChannel();
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
        configuration.save(); //neccessary
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
                URL ip = new URL("http://checkip.amazonaws.com");
                BufferedReader reader = new BufferedReader(new InputStreamReader(ip.openStream()));
                return reader.readLine();
            }
        }
        throw new IllegalStateException("Unknown running mode");
    }

    public JDA getJda() {
        return jda;
    }
}