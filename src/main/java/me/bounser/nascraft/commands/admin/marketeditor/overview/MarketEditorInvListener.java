package me.bounser.nascraft.commands.admin.marketeditor.overview;

import me.bounser.nascraft.commands.admin.marketeditor.edit.EditorManager;
import me.bounser.nascraft.commands.admin.marketeditor.edit.category.CategoryEditor;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MarketEditorInvListener implements Listener {


    private final HashMap<Player, Integer> playerOffset = new HashMap<>();

    private final HashMap<Player, Integer> playerHorizontalOffset = new HashMap<>();

    private static MarketEditorInvListener instance = null;

    public static MarketEditorInvListener getInstance() { return instance == null ? new MarketEditorInvListener() : instance; }


    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (!event.getWhoClicked().hasPermission("nascraft.admin")) return;

        if (event.getView().getTopInventory().getSize() != 54 || !event.getView().getTitle().equals("§8§lAdmin view: Market") || event.getCurrentItem() == null)
            return;

        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {

            event.setCancelled(true);

        }

        if (event.getRawSlot() == 8) {
            event.getWhoClicked().closeInventory();
            return;
        }

        if ((event.getRawSlot() >= 9 && event.getRawSlot() <= 44)) {

            Item item = getItemFromSlot(event.getRawSlot(), player);

            EditorManager.getInstance().startEditing(player, item);

            return;
        }

        if (event.getRawSlot() == 48) {

            new CategoryEditor(player);

            return;
        }


        if (event.getRawSlot() == 49) {

            if (MarketManager.getInstance().getActive()) MarketManager.getInstance().stop();
            else MarketManager.getInstance().resume();

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

            event.getClickedInventory().setItem(49, enabled);
        }

        if (event.getRawSlot() == 50) {

            if (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) {

                EditorManager.getInstance().startEditing(player, event.getCursor().clone());

            } else {
                player.sendMessage(ChatColor.RED + "Drop an item to add it to the market!");
            }

            return;
        }

        if (event.getRawSlot() == 0) {

            if (playerOffset.get(player) == null) initializeOffsets(player, 3, 0);
            else if (playerOffset.get(player) > 3)
                playerOffset.put(player, playerOffset.get(player) - 1);

            insertItems(event.getInventory(), playerOffset.get(player), playerHorizontalOffset.get(player));

            return;
        }

        if (event.getRawSlot() == 45) {

            if (playerOffset.get(player) == null) initializeOffsets(player, 4, 0);
            else if (playerOffset.get(player) < MarketManager.getInstance().getCategories().size()-1)
                playerOffset.put(player, playerOffset.get(player) + 1);

            insertItems(event.getInventory(), playerOffset.get(player), playerHorizontalOffset.get(player));

            return;
        }

        if (event.getRawSlot() == 52) {

            if (playerHorizontalOffset.get(player) == null) initializeOffsets(player, 3, 0);
            else {
                if (playerHorizontalOffset.get(player) > 0)
                    playerHorizontalOffset.put(player, playerHorizontalOffset.get(player) - 1);
            }

            insertItems(event.getInventory(), playerOffset.get(player), playerHorizontalOffset.get(player));

            return;
        }

        if (event.getRawSlot() == 53) {

            if (playerHorizontalOffset.get(player) == null) initializeOffsets(player, 3, 1);
            else {
                int biggestCategory = 0;

                for (Category category : MarketManager.getInstance().getCategories())
                    if (category.getNumberOfItems() > biggestCategory) biggestCategory = category.getNumberOfItems();

                if (playerHorizontalOffset.get(player) < biggestCategory-9)
                    playerHorizontalOffset.put(player, playerHorizontalOffset.get(player) + 1);
            }

            insertItems(event.getInventory(), playerOffset.get(player), playerHorizontalOffset.get(player));

            return;
        }

    }

    public void initializeOffsets(Player player, int offset, int horizontalOffset) {

        playerOffset.put(player, offset);
        playerHorizontalOffset.put(player, horizontalOffset);

    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {

        if (!event.getPlayer().hasPermission("nascraft.admin")) return;

        if (!event.getView().getTitle().equals("§8§lAdmin view: Market") || playerOffset.get(event.getPlayer()) == null)  return;

        playerOffset.remove(event.getPlayer());

    }

    public void insertItems(Inventory inventory, int offset, int horizontalOffset) {

        List<Category> categories = new ArrayList<>();

        for (int i = 3; i >= 0; i--)
            categories.add(MarketManager.getInstance().getCategories().get(offset-i));

        int j = 0;

        for (Category category : categories) {

            List<Item> items = new ArrayList<>();

            if (horizontalOffset < category.getNumberOfItems())
                items = new ArrayList<>(category.getItems().subList(horizontalOffset, category.getNumberOfItems()));

            while (items.size() < 9)
                items.add(null);

            for (int k = 0; k <= 8; k++) {

                Item item = items.get(k);

                if (item == null) {
                    inventory.clear((j+1)*9 + k);
                } else {
                    ItemStack itemStack = item.getItemStack();

                    ItemMeta meta = itemStack.getItemMeta();

                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Alias: " + item.getName());

                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Initial price: " + ChatColor.GOLD + Formatter.format(item.getPrice().getInitialValue(), Style.ROUND_BASIC),
                            ChatColor.GRAY + "Elasticity: " + ChatColor.GOLD + item.getPrice().getElasticity(),
                            ChatColor.GRAY + "Noise Sensibility: " + ChatColor.GOLD + item.getPrice().getNoiseIntensity(),
                            ChatColor.GRAY + "Support: " + ChatColor.GOLD + item.getPrice().getSupport(),
                            ChatColor.GRAY + "Resistance: " + ChatColor.GOLD + item.getPrice().getResistance(),
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

    public Item getItemFromSlot(int slot, Player player) {

        int i = 0;

        if (slot >= 18 && slot <= 25) i = 1;
        else if (slot >= 27 && slot <= 35) i = 2;
        else if (slot >= 36 && slot <= 44) i = 3;

        Category category;

        if (playerOffset.get(player) == null || playerOffset.get(player) == 0) {
            category = MarketManager.getInstance().getCategories().get(i);
        } else {
            category = MarketManager.getInstance().getCategories().get(playerOffset.get(player)-3+i);
        }

        return category.getItemOfIndex(slot - 9-(9*i) + (playerHorizontalOffset.get(player) == null ? 0 : playerHorizontalOffset.get(player)));
    }

}
