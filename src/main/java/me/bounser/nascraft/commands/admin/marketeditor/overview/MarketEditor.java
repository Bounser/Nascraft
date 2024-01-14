package me.bounser.nascraft.commands.admin.marketeditor.overview;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
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
        insertHopper(inventory);
        MarketEditorInvListener.getInstance().insertItems(inventory, 3);

        player.openInventory(inventory);

    }


    public void insertFillingPanes(Inventory inventory) {

        ItemStack blackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaBlack = blackFiller.getItemMeta();
        metaBlack.setDisplayName(" ");
        blackFiller.setItemMeta(metaBlack);

        for(int i : new int[]{1, 2, 3, 5, 6, 7, 46, 47, 48, 50, 51, 52, 53}) {
            inventory.setItem(i, blackFiller);
        }

        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = closeButton.getItemMeta();
        meta.setDisplayName(Lang.get().message(Message.SELL_CLOSE));
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
    }

    public void insertHelpHead(Inventory inventory) {

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "§lMARKET EDITOR");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "In this menu you can add, remove",
                ChatColor.GRAY + "and edit items of the market."
        ));
        info.setItemMeta(meta);

        inventory.setItem(4, info);
    }

    public void insertHopper(Inventory inventory) {

        ItemStack info = new ItemStack(Material.HOPPER);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "§lADD ITEM TO MARKET");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Drop here an item to configure",
                ChatColor.GRAY + "it as a new item."
        ));
        info.setItemMeta(meta);

        inventory.setItem(49, info);
    }


}
