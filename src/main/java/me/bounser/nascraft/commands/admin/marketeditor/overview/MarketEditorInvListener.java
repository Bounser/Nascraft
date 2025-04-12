package me.bounser.nascraft.commands.admin.marketeditor.overview;

import de.tr7zw.changeme.nbtapi.NBT;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.admin.marketeditor.edit.item.EditorManager;
import me.bounser.nascraft.commands.admin.marketeditor.edit.category.CategoryEditorManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;

public class MarketEditorInvListener implements Listener {


    private static MarketEditorInvListener instance = null;

    public static MarketEditorInvListener getInstance() { return instance == null ? new MarketEditorInvListener() : instance; }


    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (!event.getWhoClicked().hasPermission("nascraft.admin")) return;

        if (event.getView().getTopInventory().getSize() != 54 || !event.getView().getTitle().equals("§8§lAdmin view: Market")) return;

        if (Objects.equals(event.getClickedInventory(), event.getView().getTopInventory())) event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == null || event.getClickedInventory().equals(event.getView().getTopInventory())) event.setCancelled(true);

        MarketEditor marketEditor = MarketEditorManager.getInstance().getMarketEditorFromPlayer(((Player) event.getWhoClicked()).getPlayer());

        switch (event.getRawSlot()) {
            case 0:

                marketEditor.decreaseVerticalOffset();
                marketEditor.insertItems(event.getInventory());

                return;

            case 8:
                event.getWhoClicked().closeInventory();
                return;

            case 45:

                marketEditor.increaseVerticalOffset();
                marketEditor.insertItems(event.getInventory());

                return;

            case 46:

                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {

                            String identifier = stateSnapshot.getText();

                            if (MarketManager.getInstance().getCategoryFromIdentifier(identifier) != null)
                                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Repeated identifier!"));

                            Category category = new Category(identifier);

                            category.setDisplayName(identifier);

                            MarketManager.getInstance().getCategories().add(category);

                            FileConfiguration categoriesFile = Config.getInstance().getCategoriesFileConfiguration();

                            categoriesFile.set("categories." + identifier + ".display-name", identifier);

                            try { categoriesFile.save(Config.getInstance().getCategoriesFile()); }
                            catch (IOException e) { throw new RuntimeException(e); }

                            stateSnapshot.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Category created correctly!");

                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> MarketEditorManager.getInstance().getMarketEditorFromPlayer(stateSnapshot.getPlayer()).open())
                            );

                        })
                        .preventClose()
                        .text("Identifier...")
                        .title("New category")
                        .plugin(Nascraft.getInstance())
                        .open(player);
                return;

            case 7:
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

                    Config.getInstance().setMarketClosed();

                } else {
                    enabled = new ItemStack(Material.RED_DYE);

                    metaEnabled = enabled.getItemMeta();
                    metaEnabled.setDisplayName(ChatColor.RED + "§lMARKET STOPPED");
                    metaEnabled.setLore(Arrays.asList(
                            ChatColor.GRAY + "Click to resume the market.",
                            ChatColor.GRAY + "Users will be able to buy/sell."
                    ));

                    Config.getInstance().setMarketOpen();
                }

                Nascraft.getInstance().saveConfig();

                enabled.setItemMeta(metaEnabled);

                event.getClickedInventory().setItem(7, enabled);
                return;

            case 49:
                if (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) {

                    ItemStack itemStack = event.getCursor().clone();

                    for (String key : Config.getInstance().getIgnoredKeys())
                        NBT.modify(itemStack, nbt -> {
                            nbt.removeKey(key);
                        });

                    EditorManager.getInstance().startEditing(player, itemStack);

                } else {
                    player.sendMessage(ChatColor.RED + "Drop an item to add it to the market!");
                }

                return;

            case 52:
                marketEditor.decreaseHorizontalOffset();
                marketEditor.insertItems(event.getInventory());
                return;

            case 53:
                marketEditor.increaseHorizontalOffset();
                marketEditor.insertItems(event.getInventory());
                return;

            case 9:
            case 18:
            case 27:
            case 36:
                CategoryEditorManager.getInstance().startEditing(player, getCategoryFromSlot(event.getRawSlot(), player));
                return;

            default:

                if (event.getCurrentItem() == null) return;

                if ((event.getRawSlot() >= 9 && event.getRawSlot() <= 44)) {

                    if (!event.getCurrentItem().getType().equals(Material.AIR)) {
                        Item item = getItemFromSlot(event.getRawSlot(), player);

                        if (item == null) return;

                        EditorManager.getInstance().startEditing(player, item);
                    }
                }
        }
    }


    public Item getItemFromSlot(int slot, Player player) {

        MarketEditor marketEditor = MarketEditorManager.getInstance().getMarketEditorFromPlayer(player);

        int i = 0;

        if (slot >= 19 && slot <= 25) i = 1;
        else if (slot >= 28 && slot <= 35) i = 2;
        else if (slot >= 37 && slot <= 44) i = 3;

        Category category = MarketManager.getInstance().getCategories().get(marketEditor.getVerticalOffset()+i);

        return category.getItemOfIndex(slot - 10 - (9 * i) + (marketEditor.getHorizontalOffset()));
    }

    public Category getCategoryFromSlot(int slot, Player player) {

        MarketEditor marketEditor = MarketEditorManager.getInstance().getMarketEditorFromPlayer(player);

        int offset = marketEditor.getVerticalOffset();

        switch (slot) {

            case 18:
                offset += 1; break;
            case 27:
                offset += 2; break;
            case 36:
                offset += 3; break;

        }

        return MarketManager.getInstance().getCategories().get(offset);
    }

}
