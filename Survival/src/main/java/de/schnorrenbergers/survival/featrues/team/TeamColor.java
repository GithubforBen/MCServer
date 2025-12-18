package de.schnorrenbergers.survival.featrues.team;

import org.bukkit.ChatColor;

import java.net.MalformedURLException;
import java.net.URL;

public enum TeamColor {
    DARK_BLUE(ChatColor.DARK_BLUE, "Dunkelblau", "ff95ec50c41390d97513b8f9283dbc009037f8044d5b8b2d7a7cdc56c9166a28"),
    DARK_GREEN(ChatColor.DARK_GREEN, "Dunkelgrün", "f49bfe865db147608abb71da04fd2cc614602644fd2261d5f45f73abc3a"),
    DARK_AQUA(ChatColor.DARK_AQUA, "Dunkeltürkis", "31f57051130e850848e8e37e72110a16f09dbdab7d9d6e33a9fecfd348d5a110"),
    DARK_RED(ChatColor.DARK_RED, "Dunkelrot", "df4dc3c3753bf5b0b7f081cdb49b83d37428a12e4187f6346dec06fac54ce"),
    DARK_PURPLE(ChatColor.DARK_PURPLE, "Dunkellila", "9b82e72b8e4832e5a114ab0fc127c8acb83f31fd4d266d08b2cacc5b6401a400"),
    GOLD(ChatColor.GOLD, "Gold", "2090d09e173ee34138c3b01b48ee0be534bbb1ace0ddf5ff98e66f7b02113995"),
    GRAY(ChatColor.GRAY, "Grau", "1c8e0ddf2432f4332b87691b5952c7679763ef4f275b874e9bceb888ed5b5b9"),
    DARK_GRAY(ChatColor.DARK_GRAY, "Dunkelgrau", "ff9bb9e56125c8227b94bbda9f6e0f862931c229255ba8f1205d13c44c1bb561"),
    BLUE(ChatColor.BLUE, "Blau", "3f9c32138c9764c639aebd819cd91992aed01bf448f0e710a03ab443ac490ee9"),
    GREEN(ChatColor.GREEN, "Grün", "383a88b593dab7e6218b79f5d95c44bb7ea17427c8e6c8cf6bb51cd2ba6ece2a"),
    AQUA(ChatColor.AQUA, "Türkis", "2d6a8b47da923b7d10142447fdbdcfd1e8e82eb484964252bb36ddb5f73b51c2"),
    RED(ChatColor.RED, "Rot", "ac14600ace50695c7c9bcf09e42afd9f53c9e20daa1524c95db4197dd3116412"),
    LIGHT_PURPLE(ChatColor.LIGHT_PURPLE, "Helllila", "11f1dc113ebab125e683e2761b621c1469ef2990303f31bb829c11ee2ba0f93"),
    YELLOW(ChatColor.YELLOW, "Gelb", "1f7a7de25b164f899bd6e8a2aa5956e86e7841e82273b1f8790622fc6275e9");

    private final ChatColor color;
    private final String readableName;
    private final String headUUID;

    TeamColor(ChatColor color, String readableName, String headUUID) {
        this.color = color;
        this.readableName = readableName;
        this.headUUID = headUUID;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getReadableName() {
        return readableName;
    }

    public URL getHeadTexture() throws MalformedURLException {
        return new URL("http://textures.minecraft.net/texture/" + this.headUUID);
    }
}
