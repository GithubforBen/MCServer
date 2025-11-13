package de.hems.api;


import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ItemApi {
    ItemStack itemStack;
    ItemMeta itemMeta;
    SkullMeta skullMeta;


    public ItemApi(Material material) {//generiert item mit standard material
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
    }

    public ItemApi(Material material, String name) {//generiert item mit standard material und name
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
    }

    public ItemApi(Material material, String name, boolean hasEnchatmentGlint) {//generiert item mit standard material und name
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemStack.addUnsafeEnchantment(Enchantment.POWER, 1);
    }
    public ItemApi(Material material, String name, List<String> lore) {//generiert item mit standard material und name
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(lore);
    }

    public ItemApi(ItemStack itemStack, List<String> strings) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(strings);
    }

    public ItemApi(Material material, Enchantment enchantment, int enchantmentlevel) {//generiert item mit standard material und verzauberung
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.addEnchant(enchantment, enchantmentlevel, true);
    }

    public ItemApi(Material material, String name, Enchantment enchantment, int enchantmentlevel) {// generiert item stack mit material und name und verzauberung
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.addEnchant(enchantment, enchantmentlevel, true);
    }

    public ItemApi(Material material, int amount) {// generiert item stack mit material und anzahl
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemStack.setAmount(amount);
    }

    public ItemApi(Material material, int amount, int custommodeldata) {// generiert item stack mit material und anzahl
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemStack.setAmount(amount);
        itemMeta.setCustomModelData(custommodeldata);
    }

    public ItemApi(Material material, String name, int amount, int custommodeldata) {// generiert item stack mit material und anzahl
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemStack.setAmount(amount);
        itemMeta.setCustomModelData(custommodeldata);
        itemMeta.setDisplayName(name);
    }

    public ItemApi(Material material, int amount, String name) {// generiert item stack mit material und anzahl und name
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemStack.setAmount(amount);
        itemMeta.setDisplayName(name);
    }

    public ItemApi(Material material, int amount, String name, Enchantment enchantment, int enchantmentlevel) {// generiert item stack mit material und anzahl und name und verzauberung
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
        itemStack.setAmount(amount);
        itemMeta.setDisplayName(name);
        itemMeta.addEnchant(enchantment, enchantmentlevel, true);
    }

    public ItemApi(String linktoskin, UUID uuid) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwnerProfile(new PlayerProfile() {

            @Override
            public UUID getUniqueId() {
                return uuid;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public PlayerTextures getTextures() {
                return new PlayerTextures() {
                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public void clear() {

                    }

                    @Override
                    public URL getSkin() {
                        try {
                            return new URL(linktoskin);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    public void setSkin(URL skinUrl) {

                    }

                    @Override
                    public void setSkin(URL skinUrl, SkinModel skinModel) {

                    }

                    @Override
                    public SkinModel getSkinModel() {
                        return null;
                    }

                    @Override
                    public URL getCape() {
                        return null;
                    }

                    @Override
                    public void setCape( URL capeUrl) {

                    }

                    @Override
                    public long getTimestamp() {
                        return 0;
                    }

                    @Override
                    public boolean isSigned() {
                        return false;
                    }
                };
            }

            @Override
            public void setTextures( PlayerTextures textures) {

            }

            @Override
            public boolean isComplete() {
                return false;
            }


            @Override
            public CompletableFuture<PlayerProfile> update() {
                return null;
            }


            @Override
            public PlayerProfile clone() {
                return null;
            }


            @Override
            public Map<String, Object> serialize() {
                return null;
            }
        });
    }

    public ItemApi(String skullowner, String name) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setDisplayName(name);
        skullMeta.setOwner(skullowner);
    } //creates skull with name

    public ItemApi(String skullowner, int amount) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwner(skullowner);
        itemStack.setAmount(amount);
    } //creates skull with amount

    public ItemApi(String skullowner, int amount, Enchantment enchantment, int level) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwner(skullowner);
        itemStack.setAmount(amount);
        skullMeta.addEnchant(enchantment, level, true);
    } //creates skull with amount and enchantment

    public ItemApi(String skullowner, String name, int amount, Enchantment enchantment, int level) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwner(skullowner);
        itemStack.setAmount(amount);
        skullMeta.setDisplayName(name);
        skullMeta.addEnchant(enchantment, level, true);
    } //creates skull with amount and enchantment and name

    public ItemApi(Player skullowner, String name) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        PlayerProfile playerProfile = skullowner.getPlayerProfile();
        skullMeta.setOwnerProfile(playerProfile);
        skullMeta.setDisplayName(name);
    }//creates skull with name

    public ItemApi(String skullowner, String name, int amount) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwner(skullowner);
        skullMeta.setDisplayName(name);
        itemStack.setAmount(amount);
    }//creates skull with amount and name

    public ItemApi(String skullowner, String name, Enchantment enchantment, int enchantmentlevel) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwner(skullowner);
        skullMeta.setDisplayName(name);
        skullMeta.addEnchant(enchantment, enchantmentlevel, true);
    }//creates skull with name and entchantmen7

    public ItemApi(String skullowner, Enchantment enchantment, int enchantmentlevel) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        itemMeta = itemStack.getItemMeta();
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwner(skullowner);
        skullMeta.addEnchant(enchantment, enchantmentlevel, true);
    } //creates skull with enchantment

    public ItemApi(URL textures, String name) {
        itemStack = new ItemStack(Material.PLAYER_HEAD);
        skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setDisplayName(name);
        com.destroystokyo.paper.profile.PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID(),null);
        PlayerTextures textures1 = playerProfile.getTextures();
        textures1.setSkin(textures);
        playerProfile.setTextures(textures1);
        skullMeta.setPlayerProfile(playerProfile);
    }

    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack buildSkull() {
        itemStack.setItemMeta(skullMeta);
        return itemStack;
    }
}
