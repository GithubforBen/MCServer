package de.hems.utils.server;

import de.hems.FileHandler;
import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.types.FileType;
import de.hems.types.MissingConfigurationException;
import de.hems.types.SecureTokenGenerator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class VelocityConfigurator extends ServerConfigurator {
    private final int port;
    private final FileType.PLUGIN[] plugins;

    public VelocityConfigurator(String directory,int port, FileType.PLUGIN[] plugins) {
        super(directory);
        this.port = port;
        this.plugins = plugins;
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
        }
        File file = new File(this.directory + "/velocity.toml");
        if (file.exists()) {file.delete();}
        writeToFile("velocity.toml", "bind = \"" + Main.getInstance().getIp() + ":" + port + "\"");
        writeToFile("velocity.toml", "forward-to-backend = \"MODERN\"");
        if (Main.getInstance().getConfiguration().getConfig().contains("serversecret")) {
            writeToFile("velocity.toml", "secret = \"" + Main.getInstance().getConfiguration().getConfig().getString("serversecret") + "\"");
        } else {
            Main.getInstance().getConfiguration().getConfig().setComments("serversecret", List.of("The velocity secret key."));
            Main.getInstance().getConfiguration().getConfig().set("serversecret", SecureTokenGenerator.generateSecureToken());
            Main.getInstance().getConfiguration().save();
            throw new MissingConfigurationException("serversecret is missing in config.yml. ou might want to change it to something else. It was auto set.");
        }
        writeToFile("velocity.toml", "max-players = 100");
        writeToFile("velocity.toml", "motd = \"Welcome to My Minecraft Network!\"");
        writeToFile("velocity.toml", "[servers]");
        for (ListenerAdapter.ServerName value : ListenerAdapter.ServerName.values()) {
            if (value.getPort() == -1) continue;
            writeToFile("velocity.toml", value.toString() + " = \"" + Main.getInstance().getIp() + ":" + value.getPort() + "\"");
        }
    }
}
