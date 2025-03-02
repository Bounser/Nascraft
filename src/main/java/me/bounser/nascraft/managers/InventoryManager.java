package me.bounser.nascraft.managers;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.MarketManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class InventoryManager {

    public static boolean checkInventory(Player player, boolean feedback, ItemStack itemStack, int amount) {

        if (player == null) return true;

        if (player.getInventory().firstEmpty() == -1) {

            int untilFull = 0;

            for (ItemStack is : player.getInventory()) {
                if(is != null && MarketManager.getInstance().isSimilarEnough(is, itemStack)) {
                    untilFull += itemStack.getType().getMaxStackSize() - is.getAmount();
                }
            }

            if (untilFull < amount) {
                if (feedback) Lang.get().message(player, Message.NOT_ENOUGH_SPACE);
                return false;
            }

        } else {
            int slotsUsed = 0;

            for (ItemStack content : player.getInventory().getStorageContents())
                if (content != null && !content.getType().equals(Material.AIR)) slotsUsed++;

            if ((36 - slotsUsed) < (amount/itemStack.getType().getMaxStackSize())) {
                if (feedback) Lang.get().message(player, Message.NOT_ENOUGH_SPACE);
                return false;
            }
        }

        return true;
    }

    public static void addItemsToInventory(Player player, ItemStack itemStack, int amount) {

        ItemStack operationItemStack = itemStack.clone();

        operationItemStack.setAmount(amount);

        HashMap<Integer, ItemStack> toDrop = player.getInventory().addItem(operationItemStack);

        for (ItemStack itemStackToDrop: toDrop.values())
            player.getWorld().dropItem(player.getLocation(), itemStackToDrop);
    }

    public static boolean containsAtLeast(Player player, ItemStack itemStack, int amount) {
        return player.getInventory().containsAtLeast(itemStack, amount);
    }

    public static void removeItems(Player player, ItemStack itemStack, int amount) {
        ItemStack operation = itemStack.clone();
        operation.setAmount(amount);
        player.getInventory().removeItem(operation);
    }

}
