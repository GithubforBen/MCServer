package de.schnorrenbergers.survival.commands;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerRestartEvent;
import de.schnorrenbergers.survival.Survival;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RestartCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            return false;
        }
        if (args.length != 1) {
            sender.sendMessage("Please specify a server name");
            return false;
        }
        try {
            ListenerAdapter.sendListeners(new RequestServerRestartEvent(
                    ListenerAdapter.ServerName.SURVIVAL,
                    ListenerAdapter.ServerName.HOST,
                    UUID.randomUUID(),
                    args[0]
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
