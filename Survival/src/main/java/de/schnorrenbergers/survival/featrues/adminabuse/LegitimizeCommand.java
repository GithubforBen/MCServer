package de.schnorrenbergers.survival.featrues.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.adminabuse.LegitamiseAdminAbuseEvent;
import de.hems.communication.events.adminabuse.RequestToLegitimizeEvent;
import de.hems.communication.events.adminabuse.RespondToLegitimizeEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/*
This class ads the command /legitimize
It is used to legitimize a command which was executed as an admin.
Arguments:
[0] = UUID of action, @allMine, @allAdmins
@allMine Sets the reason to all Commands of the sender
@allAdmins Sets the reason to all Commands awaiting reasoning
[1] = "Reason"
The Reason should be surrounded by '"'
 */
public class LegitimizeCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(usage());
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg + " ");
        }
        String[] split = stringBuilder.toString().split("\"");
        if (args[0].equals("@all")) {
            RequestToLegitimizeEvent requestToLegitimizeEvent = new RequestToLegitimizeEvent(ListenerAdapter.ServerName.HOST);
            try {
                ListenerAdapter.sendListeners(requestToLegitimizeEvent);
                RespondToLegitimizeEvent respondToLegitimizeEvent = (RespondToLegitimizeEvent) ListenerAdapter.waitForEvent(requestToLegitimizeEvent.getEventId());
                List<Runnable> runnables = new ArrayList<>();
                Collections.synchronizedMap(respondToLegitimizeEvent.getToLegitimize()).forEach((x, y) -> {
                        runnables.add(() -> {
                            try {
                                ListenerAdapter.sendListeners(new LegitamiseAdminAbuseEvent(ListenerAdapter.ServerName.HOST, x, split[1]));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                });
                runnables.forEach(Runnable::run);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(args[0]);
        } catch (Exception e) {
            sender.sendMessage(usage());
            return false;
        }
        if (split.length != 3) {
            sender.sendMessage(usage());
            return false;
        }
        try {
            ListenerAdapter.sendListeners(new LegitamiseAdminAbuseEvent(ListenerAdapter.ServerName.HOST, uuid, split[1]));
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String usage() {
        return ChatColor.GRAY + "/legitimize UUID \"Begründung\"";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.clear();
            list.add("@all");
            RequestToLegitimizeEvent requestToLegitimizeEvent = new RequestToLegitimizeEvent(ListenerAdapter.ServerName.HOST);
            try {
                ListenerAdapter.sendListeners(requestToLegitimizeEvent);
                RespondToLegitimizeEvent respondToLegitimizeEvent = (RespondToLegitimizeEvent) ListenerAdapter.waitForEvent(requestToLegitimizeEvent.getEventId());
                list.addAll(respondToLegitimizeEvent.getToLegitimize().keySet().stream().map(UUID::toString).toList());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (args.length == 2) {
            list.clear();
            list.add("\"{Begründung}\"");
        }
        return list;
    }
}

