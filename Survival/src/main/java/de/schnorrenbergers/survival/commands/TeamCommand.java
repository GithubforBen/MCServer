package de.schnorrenbergers.survival.commands;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeamCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "chunks": {
                if (!(sender instanceof Player)) {
                    sendUsage(sender);
                    return false;
                }
                Player player = (Player) sender;
                Chunk[][] chunks = new Chunk[11][11];
                Location location = player.getLocation();
                for (int i = location.getChunk().getX() - 5; i < location.getChunk().getX() +5; i++) {
                    for (int j = location.getChunk().getZ() - 5; j < location.getChunk().getZ() +5; j++) {
                        chunks[i - location.getChunk().getX() + 5][j - location.getChunk().getZ() + 5] = location.getWorld().getChunkAt(i, j);
                    }
                }

            }
        }
        return false;
    }

    public void sendUsage(CommandSender sender) {
        sender.sendMessage("Usage: /team <chunks>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of("chunks");
    }
}
