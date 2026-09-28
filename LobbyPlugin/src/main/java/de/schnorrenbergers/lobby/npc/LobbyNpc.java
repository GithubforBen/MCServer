package de.schnorrenbergers.lobby.npc;

import de.hems.paper.hologram.Hologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.jetbrains.annotations.Nullable;

/**
 * One npc standing in the lobby: what it is, where it stands, and - while it is in the world - the
 * mannequin and the text over its head.
 * <p>
 * The settings are what {@link NpcStore} saves. The mannequin and the hologram are never saved: both are
 * put back by {@link LobbyNpcs} on every start and whenever the chunk comes back, the same way the lotto
 * stand is, so there is never a second one left over from a crash.
 */
public final class LobbyNpc {

    /** Over the head of a mannequin, which is as tall as a player. */
    static final double TEXT_HEIGHT = 2.05d;

    private final String id;
    private final NpcType type;
    private @Nullable String target;
    private String name;
    private @Nullable String skin;
    private Location location;

    private @Nullable Mannequin entity;
    private @Nullable Hologram sign;

    /**
     * @param id       how admins refer to it, unique in the lobby
     * @param type     what it does
     * @param target   the server a {@link NpcType#WARP} npc sends people to, {@code null} for the others
     * @param name     what it is called, with {@code &} colour codes
     * @param skin     whose skin it wears, {@code null} for the default one
     * @param location where it stands
     */
    public LobbyNpc(String id, NpcType type, @Nullable String target, String name, @Nullable String skin,
                    Location location) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.name = name;
        this.skin = skin;
        this.location = location.clone();
    }

    public String getId() {
        return id;
    }

    public NpcType getType() {
        return type;
    }

    public @Nullable String getTarget() {
        return target;
    }

    public void setTarget(@Nullable String target) {
        this.target = target;
    }

    /**
     * @return the name as it was typed, with {@code &} colour codes
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the name as it is shown
     */
    public Component displayName() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(name);
    }

    public @Nullable String getSkin() {
        return skin;
    }

    public void setSkin(@Nullable String skin) {
        this.skin = skin;
    }

    public Location getLocation() {
        return location.clone();
    }

    public void setLocation(Location location) {
        this.location = location.clone();
    }

    // ------------------------------------------------------------------ in the world

    @Nullable Mannequin getEntity() {
        return entity;
    }

    void setEntity(@Nullable Mannequin entity) {
        this.entity = entity;
    }

    @Nullable Hologram getSign() {
        return sign;
    }

    void setSign(@Nullable Hologram sign) {
        this.sign = sign;
    }

    /**
     * @return whether the mannequin is standing in the world right now
     */
    boolean isSpawned() {
        return entity != null && entity.isValid();
    }

    /**
     * Takes the mannequin and its text out of the world. The npc itself stays, and comes back with the
     * next {@link LobbyNpcs#ensure()}.
     */
    void despawn() {
        if (entity != null && entity.isValid()) entity.remove();
        entity = null;
        if (sign != null) sign.remove();
        sign = null;
    }
}
