package me.bounser.nascraft.sellwand;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
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
    private final boolean glim;

    private final String permission;
    private final Action sell;
    private final Action estimate;
    private final List<Currency> currencies;

    private final ItemStack itemStack;

    public Wand (String name, Material material, String displayName, List<String> lore, int uses, float multiplier, float maxProfit, int cooldown, boolean glim, String permission, Action sell, Action estimate, List<Currency> currencies) {

        this.name = name;
        this.material = material;

        Component displayNameComponent = MiniMessage.miniMessage().deserialize(displayName);
        this.displayName = BukkitComponentSerializer.legacy().serialize(displayNameComponent);
        this.lore = lore;

        this.defaultUses = uses;
        this.multiplier = multiplier;
        this.maxProfit = maxProfit;
        this.cooldown = cooldown;
        this.glim = glim;

        this.permission = permission;
        this.sell = sell;
        this.estimate = estimate;
        this.currencies = currencies;

        this.itemStack = generateItemStackOfNewWand();
    }

    public String getName() { return name; }

    public List<String> getLore(int uses, float profitLeft) {

        List<String> newLore = new ArrayList<>();

        for (String line : lore) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(
                    line
                    .replace("[USES]", String.valueOf(uses))
                    .replace("[PROFIT-LEFT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), profitLeft, Style.ROUND_BASIC)));
            newLore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        return newLore;
    }

    public int getDefaultUses() { return defaultUses; }

    public float getMultiplier() { return multiplier; }

    public int getCooldown() { return cooldown; }

    public ItemStack generateItemStackOfNewWand() {

        ItemStack wand = new ItemStack(material, 1);

        ItemMeta itemMeta = wand.getItemMeta();

        itemMeta.setDisplayName(displayName);

        itemMeta.setLore(getLore(defaultUses, maxProfit));

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

    public String getPermission() { return permission; }

    public Action getSellAction() { return sell; }
    public Action getEstimateAction() { return estimate; }

    public List<Currency> getCurrencies() { return currencies; }

}
