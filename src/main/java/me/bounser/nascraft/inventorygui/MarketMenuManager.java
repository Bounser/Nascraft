package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MarketMenuManager {

    private static MarketMenuManager instance;

    private HashMap<Player, MenuPage> playerMenus = new HashMap<>();

    public static MarketMenuManager getInstance() { return instance == null ? instance = new MarketMenuManager() : instance; }

    public void openMenu(Player player) {

        new MainMenu(player);

    }

    public void setMenuOfPlayer(Player player, MenuPage menu) {
        playerMenus.put(player, menu);
    }

    public MenuPage getMenuFromPlayer(Player player) {
        return playerMenus.get(player);
    }

    public void removeMenuFromPlayer(Player player) {
        playerMenus.remove(player);
    }

    public ItemStack generateItemStack(Material material, String name, List<String> lore) {

        ItemStack itemStack = new ItemStack(material);

        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.setAttributeModifiers(null);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    public ItemStack generateItemStack(Material material, String name) {

        ItemStack itemStack = new ItemStack(material);

        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(name);
        meta.setAttributeModifiers(null);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    public List<String> getLoreFromItem(Item item, String lore) {

        List<String> itemLoreLines = new ArrayList<>();

        float change = RoundUtils.roundToOne(-100 + item.getPrice().getValue() *100/item.getPrice().getValueAnHourAgo());

        String itemLore = lore
                .replace("[PRICE]", Formatter.format(item.getCurrency(), item.getPrice().getValue(), Style.ROUND_BASIC))
                .replace("[SELL-PRICE]", Formatter.format(item.getCurrency(), item.getPrice().getSellPrice(), Style.ROUND_BASIC))
                .replace("[BUY-PRICE]", Formatter.format(item.getCurrency(), item.getPrice().getBuyPrice(), Style.ROUND_BASIC));

        String changeFormatted = Lang.get().message(Message.GUI_POSITIVE_CHANGE);

        if (change == 0) changeFormatted = Lang.get().message(Message.GUI_NO_CHANGE);
        else if (change < 0) changeFormatted = Lang.get().message(Message.GUI_NEGATIVE_CHANGE);

        itemLore = itemLore
                .replace("[CHANGE]", changeFormatted)
                .replace("[PERCENTAGE]", String.valueOf(change));


        for (String line : itemLore.split("\\n")) {
            Component loreSegment = MiniMessage.miniMessage().deserialize(line);
            itemLoreLines.add(BukkitComponentSerializer.legacy().serialize(loreSegment));
        }

        return itemLoreLines;
    }

}
