package de.hems.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UUIDApi {
    private static final Map<String, UUID> UUID_MAP = new HashMap<>();
    public static UUID fromString(String decleration) {
        if (!UUID_MAP.containsKey(decleration)) {
            UUID_MAP.put(decleration, UUID.randomUUID());
        }
        return UUID_MAP.getOrDefault(decleration, UUID.randomUUID());
    }
}
