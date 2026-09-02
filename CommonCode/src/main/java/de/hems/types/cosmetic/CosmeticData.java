package de.hems.types.cosmetic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * One cosmetic, as the network sees it: what it is, whether it is for sale and what it costs.
 * <p>
 * The half that is code - what the effect actually does - lives on the server that plays it and is found
 * by {@link #getId()}. The half that is a decision lives here and is an admin's: whether it exists at all,
 * whether it can be bought, and for how many bits. That split is what lets a new effect be shipped switched
 * off and turned on later without a restart.
 */
public class CosmeticData implements Serializable {

    private static final long serialVersionUID = 4801L;

    private String id;
    private CosmeticType type;
    private String displayName;
    private String description;
    /** The name of the {@code Material} it is drawn as, kept as text so a renamed material cannot break it. */
    private String icon;
    private boolean enabled = true;
    private boolean buyable = true;
    private int priceBits;
    /** Given to everybody without buying, for the one effect that is the standard. */
    private boolean free;
    private HashMap<String, String> settings = new HashMap<>();

    public CosmeticData() {
    }

    public CosmeticData(String id, CosmeticType type, String displayName, String description, String icon,
                        int priceBits, boolean free) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.priceBits = priceBits;
        this.free = free;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CosmeticType getType() {
        return type == null ? CosmeticType.WIN_EFFECT : type;
    }

    public void setType(CosmeticType type) {
        this.type = type;
    }

    public String getDisplayName() {
        return displayName == null ? id : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return whether it exists for players at all
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return whether it can be bought right now
     */
    public boolean isBuyable() {
        return buyable;
    }

    public void setBuyable(boolean buyable) {
        this.buyable = buyable;
    }

    public int getPriceBits() {
        return Math.max(0, priceBits);
    }

    public void setPriceBits(int priceBits) {
        this.priceBits = Math.max(0, priceBits);
    }

    /**
     * @return whether everybody has it without paying
     */
    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    /**
     * The knobs of the effect itself, so a number inside it can be changed without a new version.
     *
     * @return the settings, never {@code null}
     */
    public Map<String, String> getSettings() {
        if (settings == null) settings = new HashMap<>();
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings == null ? new HashMap<>() : new HashMap<>(settings);
    }

    /**
     * @param key      a setting
     * @param fallback what it is when nobody set it
     * @return the number behind it
     */
    public int getNumber(String key, int fallback) {
        String raw = getSettings().get(key);
        if (raw == null) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public CosmeticData copy() {
        CosmeticData copy = new CosmeticData(id, type, displayName, description, icon, priceBits, free);
        copy.enabled = enabled;
        copy.buyable = buyable;
        copy.setSettings(getSettings());
        return copy;
    }
}
