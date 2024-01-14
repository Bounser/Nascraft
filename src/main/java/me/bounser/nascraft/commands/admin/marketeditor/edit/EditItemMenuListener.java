package me.bounser.nascraft.commands.admin.marketeditor.edit;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditor;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.Category;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class EditItemMenuListener implements Listener {

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (!event.getWhoClicked().hasPermission("nascraft.admin")) return;

        if (event.getView().getTopInventory().getSize() != 27 || !event.getView().getTitle().equals("§8§lEditing Item") || event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();

        event.setCancelled(true);

        switch (event.getRawSlot()) {

            case 11:
                EditorManager.getInstance().clearEditing(player);
                new MarketEditor(player);
                break;

            case 9:
                EditorManager.getInstance().getEditItemMenuFromPlayer(player).save();
                break;

            case 17:

                ItemStack deletePanel = event.getCurrentItem();

                ItemMeta metaDelete = deletePanel.getItemMeta();

                if (metaDelete.getDisplayName().equals(ChatColor.RED + "§lDELETE ITEM")) {
                    metaDelete.setDisplayName(ChatColor.RED + "§lCONFIRM");
                    deletePanel.setItemMeta(metaDelete);
                } else {
                    EditorManager.getInstance().getEditItemMenuFromPlayer(player).removeItem();
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Item deleted.");
                }
                break;

            case 4:

                openAnvil(
                        player,
                        ChatColor.LIGHT_PURPLE + "Initial price set correctly!",
                        "Initial price...",
                        "Initial price",
                        "initialprice");

                break;

            case 5:
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {
                            EditorManager.getInstance().getEditItemMenuFromPlayer(player).setAlias(stateSnapshot.getText());
                            stateSnapshot.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Alias set correctly!");
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(() -> EditorManager.getInstance().getEditItemMenuFromPlayer(stateSnapshot.getPlayer()).open())
                                    );
                        })
                        .preventClose()
                        .text("Item Alias...")
                        .title("Item Alias")
                        .plugin(Nascraft.getInstance())
                        .open(player);
                break;

            case 13:

                openAnvil(
                        player,
                        ChatColor.LIGHT_PURPLE + "Elasticity set correctly!",
                        "Price Elasticity...",
                        "Price Elasticity",
                        "elasticity");
                break;

            case 14:

                openAnvil(
                        player,
                        ChatColor.LIGHT_PURPLE + "Noise sensibility set correctly!",
                        "Noise sensibility...",
                        "Noise sensibility",
                        "noisesensibility");
                break;

            case 22:

                openAnvil(
                        player,
                        ChatColor.LIGHT_PURPLE + "Support set correctly!",
                        "Price Support...",
                        "Price Support",
                        "support");
                break;

            case 23:

                openAnvil(
                        player,
                        ChatColor.LIGHT_PURPLE + "Resistance set correctly!",
                        "Price Resistance...",
                        "Price Resistance",
                        "resistance");
                break;

            case 15:

                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {

                            String categoryReference = stateSnapshot.getText();

                            Category selectedCategory = null;

                            for (Category category : MarketManager.getInstance().getCategories())
                                if (category.getName().equalsIgnoreCase(categoryReference) || category.getDisplayName().equalsIgnoreCase(categoryReference))
                                    selectedCategory = category;

                            if (selectedCategory == null) {

                                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Category not recognized!"));

                            } else {
                                EditorManager.getInstance().getEditItemMenuFromPlayer(player).setCategory(selectedCategory);
                                stateSnapshot.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Category set correctly!");
                                return Arrays.asList(
                                        AnvilGUI.ResponseAction.close(),
                                        AnvilGUI.ResponseAction.run(() -> EditorManager.getInstance().getEditItemMenuFromPlayer(stateSnapshot.getPlayer()).open())
                                );
                            }

                        })
                        .preventClose()
                        .text("Category...")
                        .title("Category")
                        .plugin(Nascraft.getInstance())
                        .open(player);
                break;
        }
    }

    public void openAnvil(Player player, String setupedCorrectly, String text, String title, String type) {

        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if(slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    try {
                        float value = Float.parseFloat(stateSnapshot.getText());

                        if (value < 0)
                            return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Cannot be negative!"));

                        stateSnapshot.getPlayer().sendMessage(setupedCorrectly);

                        switch (type) {

                            case "initialprice":
                                EditorManager.getInstance().getEditItemMenuFromPlayer(player).setInitialPrice(value);
                                break;
                            case "elasticity":
                                EditorManager.getInstance().getEditItemMenuFromPlayer(player).setElasticity(value);
                                break;
                            case "noisesensibility":
                                EditorManager.getInstance().getEditItemMenuFromPlayer(player).setNoiseSensibility(value);
                                break;
                            case "support":
                                EditorManager.getInstance().getEditItemMenuFromPlayer(player).setSupport(value);
                                break;
                            case "resistance":
                                EditorManager.getInstance().getEditItemMenuFromPlayer(player).setResistance(value);
                                break;

                        }

                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> EditorManager.getInstance().getEditItemMenuFromPlayer(stateSnapshot.getPlayer()).open()
                                ));
                    } catch (NumberFormatException e) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Not a valid format!"));
                    }
                })
                .preventClose()
                .text(text)
                .title(title)
                .plugin(Nascraft.getInstance())
                .open(player);

    }

}
