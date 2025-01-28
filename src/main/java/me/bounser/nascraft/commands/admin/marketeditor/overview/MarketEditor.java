package me.bounser.nascraft.commands.admin.marketeditor.overview;

import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarketEditor {

    private int verticalOffset, horizontalOffset;

    private Player player;


    public MarketEditor(Player player) {

        this.player = player;

        verticalOffset = 0;
        horizontalOffset = 0;

        open();
    }

    public void open() {
        Inventory inventory = Bukkit.createInventory(player, 54, "§8§lAdmin view: Market");

        insertFillingPanes(inventory);
        insertArrows(inventory);
        insertHelpHead(inventory);
        insertButtons(inventory);
        insertItems(inventory);

        player.openInventory(inventory);
    }

    public void insertFillingPanes(Inventory inventory) {

        ItemStack blackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaBlack = blackFiller.getItemMeta();
        metaBlack.setDisplayName(" ");
        blackFiller.setItemMeta(metaBlack);

        for(int i : new int[]{1, 2, 3, 5, 6, 47, 48, 50, 51}) {
            inventory.setItem(i, blackFiller);
        }

        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = closeButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "§lCLOSE");
        closeButton.setItemMeta(meta);

        inventory.setItem(8, closeButton);
    }

    public void insertArrows(Inventory inventory) {

        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "§lSCROLL UP");
        arrow.setItemMeta(meta);

        inventory.setItem(0, arrow);

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "§lSCROLL DOWN");
        arrow.setItemMeta(meta);

        inventory.setItem(45, arrow);

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "§l< LEFT");
        arrow.setItemMeta(meta);

        inventory.setItem(52, arrow);

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "§lRIGHT >");
        arrow.setItemMeta(meta);

        inventory.setItem(53, arrow);
    }

    public void insertHelpHead(Inventory inventory) {

        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "§lMARKET EDITOR");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "In this menu you can add, remove",
                ChatColor.GRAY + "and edit items of the market."
        ));
        info.setItemMeta(meta);

        inventory.setItem(4, info);
    }

    public void insertButtons(Inventory inventory) {

        ItemStack newItem = new ItemStack(Material.HOPPER);
        ItemMeta metaNewItem = newItem.getItemMeta();
        metaNewItem.setDisplayName(ChatColor.BLUE + "§lADD ITEM TO MARKET");
        metaNewItem.setLore(Arrays.asList(
                ChatColor.GRAY + "Drop here an item to configure",
                ChatColor.GRAY + "it as a new item."
        ));
        newItem.setItemMeta(metaNewItem);

        inventory.setItem(49, newItem);

        ItemStack newCategory = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta newCategoryItemMeta = newCategory.getItemMeta();
        newCategoryItemMeta.setDisplayName(ChatColor.BLUE + "§lNEW CATEGORY");
        newCategoryItemMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to create a new category."
        ));
        newCategory.setItemMeta(newCategoryItemMeta);

        inventory.setItem(46, newCategory);

        ItemStack enabled;
        ItemMeta metaEnabled;

        if (MarketManager.getInstance().getActive()) {
            enabled = new ItemStack(Material.LIME_DYE);

            metaEnabled = enabled.getItemMeta();
            metaEnabled.setDisplayName(ChatColor.GREEN + "§lMARKET ACTIVE");
            metaEnabled.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to stop the market.",
                    ChatColor.GRAY + "Users won't be able to buy/sell."
            ));

        } else {
            enabled = new ItemStack(Material.RED_DYE);

            metaEnabled = enabled.getItemMeta();
            metaEnabled.setDisplayName(ChatColor.RED + "§lMARKET STOPPED");
            metaEnabled.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to resume the market.",
                    ChatColor.GRAY + "Users will be able to buy/sell."
            ));
        }

        enabled.setItemMeta(metaEnabled);

        inventory.setItem(7, enabled);
    }

    public void insertItems(Inventory inventory) {

        List<Category> categories = new ArrayList<>();

        List<Category> allCategories = MarketManager.getInstance().getCategories();

        for (int i = 0; i <= 3; i++) {
            if (allCategories.size() > i + verticalOffset)
                categories.add(MarketManager.getInstance().getCategories().get(i + verticalOffset));
        }

        int j = 0;

        for (Category category : categories) {

            ItemStack categoryItemStack = new ItemStack(category.getMaterial());

            ItemMeta CategoryMeta = categoryItemStack.getItemMeta();

            CategoryMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Category: " + category.getDisplayName());
            CategoryMeta.setLore(Arrays.asList(ChatColor.GRAY + "Identifier: " + ChatColor.GOLD + category.getIdentifier(),
                    "", ChatColor.GREEN + "§lCLICK TO EDIT"));

            categoryItemStack.setItemMeta(CategoryMeta);

            inventory.setItem(9 + 9*j, categoryItemStack);

            List<Item> items = new ArrayList<>();

            if (horizontalOffset < category.getNumberOfItems())
                items = new ArrayList<>(category.getItems().subList(horizontalOffset, category.getNumberOfItems()));

            while (items.size() < 9)
                items.add(null);

            for (int k = 1; k <= 8; k++) {

                Item item = items.get(k-1);

                if (item == null) {
                    inventory.clear((j+1)*9 + k);
                } else {
                    ItemStack itemStack = item.getItemStack();

                    ItemMeta meta = itemStack.getItemMeta();

                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Alias: " + item.getName());

                    Component price = MiniMessage.miniMessage().deserialize(Formatter.format(item.getCurrency(), item.getPrice().getInitialValue(), Style.ROUND_BASIC));

                    Component support = MiniMessage.miniMessage().deserialize(Formatter.format(item.getCurrency(), item.getPrice().getSupport(), Style.ROUND_BASIC));
                    Component resistance = MiniMessage.miniMessage().deserialize(Formatter.format(item.getCurrency(), item.getPrice().getResistance(), Style.ROUND_BASIC));

                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Initial price: " + BukkitComponentSerializer.legacy().serialize(price),
                            ChatColor.GRAY + "Elasticity: " + ChatColor.GREEN + item.getPrice().getElasticity(),
                            ChatColor.GRAY + "Noise Intensity: " + ChatColor.GREEN + item.getPrice().getNoiseIntensity(),
                            ChatColor.GRAY + "Support: " + (item.getPrice().getSupport() == 0 ? ChatColor.RED + "DISABLED" : BukkitComponentSerializer.legacy().serialize(support)),
                            ChatColor.GRAY + "Resistance: " + (item.getPrice().getResistance() == 0 ? ChatColor.RED + "DISABLED" : BukkitComponentSerializer.legacy().serialize(resistance)),
                            " ",
                            ChatColor.GREEN + "§lCLICK TO EDIT"
                    ));

                    itemStack.setItemMeta(meta);

                    inventory.setItem(((j+1)*9) + k, itemStack);
                }
            }
            j++;
        }
    }

    public void increaseVerticalOffset() {
        if (MarketManager.getInstance().getCategories().size() - 4 > verticalOffset)
            verticalOffset++;

    }

    public void increaseHorizontalOffset() {
        int biggestCategory = 0;

        for (Category category : MarketManager.getInstance().getCategories())
            if (category.getNumberOfItems() > biggestCategory) biggestCategory = category.getNumberOfItems();

        if (horizontalOffset < biggestCategory-8) horizontalOffset++;
    }

    public void decreaseVerticalOffset() { if (verticalOffset > 0) verticalOffset--; }

    public void decreaseHorizontalOffset() {

        if (horizontalOffset > 0)
            horizontalOffset--;

    }

    public int getVerticalOffset() { return verticalOffset; }

    public int getHorizontalOffset() { return horizontalOffset; }

}
