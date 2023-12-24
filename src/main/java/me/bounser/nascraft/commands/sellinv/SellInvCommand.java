package me.bounser.nascraft.commands.sellinv;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class SellInvCommand implements CommandExecutor {

    private Config lang = Config.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {


        if(!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nascraft.sellinv")) {
            Lang.get().message(player, Message.NO_PERMISSION);
            return false;
        }

        Inventory inventory = Bukkit.createInventory(player, 45, Lang.get().message(Message.SELL_TITLE));

        insertFillingPanes(inventory);
        insertSellButton(inventory);
        insertCloseButton(inventory);

        player.openInventory(inventory);
        return false;
    }

    public void insertFillingPanes(Inventory inventory) {

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 36, 37, 38, 39, 41, 42, 43, 44}) {
            inventory.setItem(i, filler);
        }
    }

    public void insertSellButton(Inventory inventory) {

        ItemStack sellButton = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = sellButton.getItemMeta();
        meta.setDisplayName(Lang.get().message(Message.SELL_BUTTON_NAME));
        meta.setLore(Collections.singletonList(Lang.get().message(Message.SELL_BUTTON_LORE, "0", "", "")));
        sellButton.setItemMeta(meta);

        inventory.setItem(40, sellButton);
    }

    public void insertCloseButton(Inventory inventory) {

        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = closeButton.getItemMeta();
        meta.setDisplayName(Lang.get().message(Message.SELL_CLOSE));
        closeButton.setItemMeta(meta);

        inventory.setItem(8, closeButton);
    }

}
