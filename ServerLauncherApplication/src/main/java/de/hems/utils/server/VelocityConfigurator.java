package de.hems.utils.server;

import de.hems.FileHandler;
import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.MissingConfigurationException;
import de.hems.types.SecureTokenGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class VelocityConfigurator extends ServerConfigurator {
    private final int port;
    private final FileType.PLUGIN[] plugins;

    public VelocityConfigurator(String directory,int port, FileType.PLUGIN[] plugins) throws IOException {
        super(directory);
        this.port = port;
        this.plugins = plugins;
        //TODO: replace this enterprise™ workaround
        //deletes the configuration so it can be set again
        if (!firstTime) new File(directory + "/velocity.toml").delete();
        firstTime = true;
        System.out.println(plugins.length + " plugins will be installed.");
    }

    public void configure() throws Exception {
        File jar = new File(this.directory + "/" + FileType.SERVER.getFileName(FileType.SERVER.VELOCITY));
        File jarFile = new FileHandler().provideFile(FileType.SERVER.VELOCITY);
        Files.copy(jarFile.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        for (FileType.PLUGIN plugin : plugins) {
            File pluginF = new File(this.directory + "/plugins/" + FileType.PLUGIN.getFileName(plugin));
            pluginF.getParentFile().mkdirs();
            File pluginFile = new FileHandler().provideFile(plugin);
            Files.copy(pluginFile.toPath(), pluginF.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied " + plugin.toString());
        }
        File toml = new File(this.directory + "/velocity.toml");
        if (toml.exists()) {toml.delete();}
        writeToFile("velocity.toml", "bind = \"" + Main.getInstance().getIp() + ":" + port + "\"", false);
        writeToFile("velocity.toml", "player-info-forwarding-mode = \"modern\"", false);
        writeToFile("velocity.toml", "config-version = \"2.7\"", false);
        if (Main.getInstance().getConfiguration().getConfig().contains("serversecret")) {
            overwriteToFile("forwarding.secret", Main.getInstance().getConfiguration().getConfig().getString("serversecret"), false);
            writeToFile("velocity.toml", "forwarding-secret-file = \"forwarding.secret\"", false);
        } else {
            Main.getInstance().getConfiguration().getConfig().setComments("serversecret", List.of("The velocity secret key."));
            Main.getInstance().getConfiguration().getConfig().set("serversecret", SecureTokenGenerator.generateSecureToken());
            Main.getInstance().getConfiguration().save();
            throw new MissingConfigurationException("serversecret is missing in config.yml. You might want to change it to something else. It was auto set.");
        }
        writeToFile("velocity.toml", "show-max-players = 100",false);
        writeToFile("velocity.toml", "motd = \"&kabc&r &3minecraft server &r&kabc\"",false);
        writeToFile("velocity.toml", "# Should we authenticate players with Mojang? By default, this is on.\n" +
                "online-mode = true\n" +
                "\n" +
                "# Should the proxy enforce the new public key security standard? By default, this is on.\n" +
                "force-key-authentication = true\n" +
                "\n" +
                "# If client's ISP/AS sent from this proxy is different from the one from Mojang's\n" +
                "# authentication server, the player is kicked. This disallows some VPN and proxy\n" +
                "# connections but is a weak form of protection.\n" +
                "prevent-client-proxy-connections = false\n" +
                "\n" +
                "\n" +
                "\n" +
                "# Announce whether or not your server supports Forge. If you run a modded server, we\n" +
                "# suggest turning this on.\n" +
                "# \n" +
                "# If your network runs one modpack consistently, consider using ping-passthrough = \"mods\"\n" +
                "# instead for a nicer display in the server list.\n" +
                "announce-forge = false\n" +
                "\n" +
                "# If enabled (default is false) and the proxy is in online mode, Velocity will kick\n" +
                "# any existing player who is online if a duplicate connection attempt is made.\n" +
                "kick-existing-players = false\n" +
                "\n" +
                "# Should Velocity pass server list ping requests to a backend server?\n" +
                "# Available options:\n" +
                "# - \"disabled\":    No pass-through will be done. The velocity.toml and server-icon.png\n" +
                "#                  will determine the initial server list ping response.\n" +
                "# - \"mods\":        Passes only the mod list from your backend server into the response.\n" +
                "#                  The first server in your try list (or forced host) with a mod list will be\n" +
                "#                  used. If no backend servers can be contacted, Velocity won't display any\n" +
                "#                  mod information.\n" +
                "# - \"description\": Uses the description and mod list from the backend server. The first\n" +
                "#                  server in the try (or forced host) list that responds is used for the\n" +
                "#                  description and mod list.\n" +
                "# - \"all\":         Uses the backend server's response as the proxy response. The Velocity\n" +
                "#                  configuration is used if no servers could be contacted.\n" +
                "ping-passthrough = \"DISABLED\"\n" +
                "\n" +
                "# If enabled (default is false), then a sample of the online players on the proxy will be visible\n" +
                "# when hovering over the player count in the server list.\n" +
                "# This doesn't have any effect when ping passthrough is set to either \"description\" or \"all\".\n" +
                "sample-players-in-ping = false\n" +
                "\n" +
                "# If not enabled (default is true) player IP addresses will be replaced by <ip address withheld> in logs\n" +
                "enable-player-address-logging = true",false);
        writeToFile("velocity.toml", "[servers]",false);
        for (ListenerAdapter.ServerName value : ListenerAdapter.ServerName.values()) {
            if (value.getPort() == -1) continue;
            writeToFile("velocity.toml", value.toString() + " = \"" + "localhost" + ":" + value.getPort() + "\"".replaceAll("\t",""),false);
        }
        writeToFile("velocity.toml", "try = [\n" +
                "    \""+ ListenerAdapter.ServerName.LOBBY +"\",\n" +
                "    \""+ ListenerAdapter.ServerName.SURVIVAL +"\"\n" +
                "]", false);
        writeToFile("velocity.toml", "[forced-hosts]",false);
        writeToFile("velocity.toml", "[advanced]\n" +
                "# How large a Minecraft packet has to be before we compress it. Setting this to zero will\n" +
                "# compress all packets, and setting it to -1 will disable compression entirely.\n" +
                "compression-threshold = 256\n" +
                "\n" +
                "# How much compression should be done (from 0-9). The default is -1, which uses the\n" +
                "# default level of 6.\n" +
                "compression-level = -1\n" +
                "\n" +
                "# How fast (in milliseconds) are clients allowed to connect after the last connection? By\n" +
                "# default, this is three seconds. Disable this by setting this to 0.\n" +
                "login-ratelimit = 3000\n" +
                "\n" +
                "# Specify a custom timeout for connection timeouts here. The default is five seconds.\n" +
                "connection-timeout = 5000\n" +
                "\n" +
                "# Specify a read timeout for connections here. The default is 30 seconds.\n" +
                "read-timeout = 30000\n" +
                "\n" +
                "# Enables compatibility with HAProxy's PROXY protocol. If you don't know what this is for, then\n" +
                "# don't enable it.\n" +
                "haproxy-protocol = false\n" +
                "\n" +
                "# Enables TCP fast open support on the proxy. Requires the proxy to run on Linux.\n" +
                "tcp-fast-open = false\n" +
                "\n" +
                "# Enables BungeeCord plugin messaging channel support on Velocity.\n" +
                "bungee-plugin-message-channel = true\n" +
                "\n" +
                "# Shows ping requests to the proxy from clients.\n" +
                "show-ping-requests = false\n" +
                "\n" +
                "# By default, Velocity will attempt to gracefully handle situations where the user unexpectedly\n" +
                "# loses connection to the server without an explicit disconnect message by attempting to fall the\n" +
                "# user back, except in the case of read timeouts. BungeeCord will disconnect the user instead. You\n" +
                "# can disable this setting to use the BungeeCord behavior.\n" +
                "failover-on-unexpected-server-disconnect = true\n" +
                "\n" +
                "# Declares the proxy commands to 1.13+ clients.\n" +
                "announce-proxy-commands = true\n" +
                "\n" +
                "# Enables the logging of commands\n" +
                "log-command-executions = false\n" +
                "\n" +
                "# Enables logging of player connections when connecting to the proxy, switching servers\n" +
                "# and disconnecting from the proxy.\n" +
                "log-player-connections = true\n" +
                "\n" +
                "# Allows players transferred from other hosts via the\n" +
                "# Transfer packet (Minecraft 1.20.5) to be received.\n" +
                "accepts-transfers = false\n" +
                "\n" +
                "# Enables support for SO_REUSEPORT. This may help the proxy scale better on multicore systems\n" +
                "# with a lot of incoming connections, and provide better CPU utilization than the existing\n" +
                "# strategy of having a single thread accepting connections and distributing them to worker\n" +
                "# threads. Disabled by default. Requires Linux or macOS.\n" +
                "enable-reuse-port = false\n" +
                "\n" +
                "# How fast (in milliseconds) are clients allowed to send commands after the last command\n" +
                "# By default this is 50ms (20 commands per second)\n" +
                "command-rate-limit = 50\n" +
                "\n" +
                "# Should we forward commands to the backend upon being rate limited?\n" +
                "# This will forward the command to the server instead of processing it on the proxy.\n" +
                "# Since most server implementations have a rate limit, this will prevent the player\n" +
                "# from being able to send excessive commands to the server.\n" +
                "forward-commands-if-rate-limited = true\n" +
                "\n" +
                "# How many commands are allowed to be sent after the rate limit is hit before the player is kicked?\n" +
                "# Setting this to 0 or lower will disable this feature.\n" +
                "kick-after-rate-limited-commands = 0\n" +
                "\n" +
                "# How fast (in milliseconds) are clients allowed to send tab completions after the last tab completion\n" +
                "tab-complete-rate-limit = 10\n" +
                "\n" +
                "# How many tab completions are allowed to be sent after the rate limit is hit before the player is kicked?\n" +
                "# Setting this to 0 or lower will disable this feature.\n" +
                "kick-after-rate-limited-tab-completes = 0\n" +
                "\n" +
                "[query]\n" +
                "# Whether to enable responding to GameSpy 4 query responses or not.\n" +
                "enabled = false\n" +
                "\n" +
                "# If query is enabled, on what port should the query protocol listen on?\n" +
                "port = 25565\n" +
                "\n" +
                "# This is the map name that is reported to the query services.\n" +
                "map = \"Velocity\"\n" +
                "\n" +
                "# Whether plugins should be shown in query response by default or not\n" +
                "show-plugins = false\n",false);
        System.out.println("Velocity configuration done!");
    }
}
