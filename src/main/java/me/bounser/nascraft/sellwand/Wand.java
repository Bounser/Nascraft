package me.bounser.nascraft.sellwand;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class Wand {


    private final String name;

    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int defaultUses;
    private final float multiplier;
    private final float maxProfit;
    private final int cooldown;

    private final ItemStack itemStack;

    public Wand (String name, Material material, String displayName, List<String> lore, int uses, float multiplier, float maxProfit, int cooldown, boolean glim) {

        this.name = name;
        this.material = material;
        this.displayName = displayName;

        this.lore = new ArrayList<>();
        for (String s : lore) {
            this.lore.add(s.replace("&", "§"));
        }

        this.defaultUses = uses;
        this.multiplier = multiplier;
        this.maxProfit = maxProfit;
        this.cooldown = cooldown;

        this.itemStack = generateItemStackOfNewWand(glim);
    }

    public String getName() { return name; }

    public List<String> getLore() { return lore; }

    public int getDefaultUses() { return defaultUses; }

    public float getMultiplier() { return multiplier; }

    public int getCooldown() { return cooldown; }

    public ItemStack generateItemStackOfNewWand(boolean glim) {

        ItemStack wand = new ItemStack(material, 1);

        ItemMeta itemMeta = wand.getItemMeta();

        itemMeta.setDisplayName(displayName.replace("&", "§"));

        List<String> replacedLore = new ArrayList<>();

        for (String loreString : lore)
            replacedLore.add(loreString
                    .replace("[USES]", String.valueOf(defaultUses))
                    .replace("[PROFIT]", Formatter.format(maxProfit, Style.ROUND_BASIC)));

        itemMeta.setLore(replacedLore);

        NamespacedKey keyType = new NamespacedKey(Nascraft.getInstance(), "wand-type");
        itemMeta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, name);

        if (defaultUses > 0) {
            NamespacedKey keyUses = new NamespacedKey(Nascraft.getInstance(), "wand-uses");
            itemMeta.getPersistentDataContainer().set(keyUses, PersistentDataType.INTEGER, defaultUses);
        }

        if (maxProfit > 0) {
            NamespacedKey keyMaxProfit = new NamespacedKey(Nascraft.getInstance(), "wand-max-profit");
            itemMeta.getPersistentDataContainer().set(keyMaxProfit, PersistentDataType.FLOAT, maxProfit);
        }

        if (glim) {
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        wand.setItemMeta(itemMeta);

        return wand;
    }

    public ItemStack getItemStackOfNewWand() { return itemStack; }

}
