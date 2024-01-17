package me.bounser.nascraft.commands.admin.marketeditor.overview;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.MarketManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MarketEditor {


    public MarketEditor(Player player) {

        Inventory inventory = Bukkit.createInventory(player, 54, "§8§lAdmin view: Market");

        insertFillingPanes(inventory);
        insertArrows(inventory);
        insertHelpHead(inventory);
        insertButtons(inventory);
        MarketEditorInvListener.getInstance().insertItems(inventory, 3, 0);

        MarketEditorInvListener.getInstance().initializeOffsets(player, 3, 0);

        player.openInventory(inventory);

    }


    public void insertFillingPanes(Inventory inventory) {

        ItemStack blackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaBlack = blackFiller.getItemMeta();
        metaBlack.setDisplayName(" ");
        blackFiller.setItemMeta(metaBlack);

        for(int i : new int[]{1, 2, 3, 5, 6, 7, 46, 47, 49, 51}) {
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
        metaNewItem.setDisplayName(ChatColor.GREEN + "§lADD ITEM TO MARKET");
        metaNewItem.setLore(Arrays.asList(
                ChatColor.GRAY + "Drop here an item to configure",
                ChatColor.GRAY + "it as a new item."
        ));
        newItem.setItemMeta(metaNewItem);

        inventory.setItem(50, newItem);

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

        inventory.setItem(49, enabled);

        ItemStack category = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta metaCategory = category.getItemMeta();
        metaCategory.setDisplayName(ChatColor.GREEN + "§lEDIT CATEGORIES");
        metaCategory.setLore(Arrays.asList(
                ChatColor.GRAY + "Click here to edit and add",
                ChatColor.GRAY + "new categories."
        ));
        category.setItemMeta(metaCategory);

        inventory.setItem(48, category);
    }


}
