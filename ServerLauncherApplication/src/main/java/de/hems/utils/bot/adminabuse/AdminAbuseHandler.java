package de.hems.utils.bot.adminabuse;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminAbuseHandler {

    public static List<AdminAbuse> adminAbuses;

    public AdminAbuseHandler() {
        if (adminAbuses == null) {
            adminAbuses = new ArrayList<>();
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(1000L * 10);
                        for (AdminAbuse adminAbus : adminAbuses) {
                            adminAbus.sendIfNecessary();
                            if (adminAbus.isHasBeenSent()) {
                                adminAbuses.remove(adminAbus);
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    public static UUID addAdminAbuse(String command, String player, Long time) {
        AdminAbuse adminAbuse = new AdminAbuse(command, player, time);
        adminAbuses.add(adminAbuse);
        return adminAbuse.getUuid();
    }

    public static boolean reasonAdminAbuse(UUID id, String reason) {
        for (AdminAbuse adminAbus : adminAbuses) {
            if (adminAbus.getUuid().equals(id)) {
                adminAbus.setReason(reason);
                return true;
            }
        }
        return false;
    }
}
