package me.bounser.nascraft.commands.admin.marketeditor.edit.category;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Objects;

public class CategoryEditorListener implements Listener {

    private static CategoryEditorListener instance = null;

    public static CategoryEditorListener getInstance() { return instance == null ? new CategoryEditorListener() : instance; }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (!event.getWhoClicked().hasPermission("nascraft.admin")) return;

        if (event.getView().getTopInventory().getSize() != 27 || !event.getView().getTitle().equals("§8§lEdit Category") || event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();

        if (Objects.equals(event.getClickedInventory(), event.getView().getTopInventory())) event.setCancelled(true);

        CategoryEditor categoryEditor = CategoryEditorManager.getInstance().getEditCategoryFromPlayer(player);

        switch (event.getRawSlot()) {

            case 9:
                categoryEditor.save();
                return;

            case 11:
                CategoryEditorManager.getInstance().clearEditing(player);
                new MarketEditor(player);
                return;

            case 17:
                ItemStack deletePanel = event.getCurrentItem();

                ItemMeta metaDelete = deletePanel.getItemMeta();

                if (metaDelete.getDisplayName().equals(ChatColor.RED + "§lDELETE CATEGORY")) {
                    metaDelete.setDisplayName(ChatColor.RED + "§lCONFIRM");
                    deletePanel.setItemMeta(metaDelete);
                } else {
                    categoryEditor.removeCategory();
                }

                return;

            case 13:
                new AnvilGUI.Builder()
                        .onClick((slot, stateSnapshot) -> {

                            String categoryName = stateSnapshot.getText();

                            categoryEditor.setDisplayName(categoryName);

                            stateSnapshot.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Category display name correctly!");
                            return Arrays.asList(
                                    AnvilGUI.ResponseAction.close(),
                                    AnvilGUI.ResponseAction.run(categoryEditor::open)
                            );

                        })
                        .preventClose()
                        .text("Display name...")
                        .title("Category name")
                        .plugin(Nascraft.getInstance())
                        .open(player);

                return;

            case 14:

                if (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)){
                    categoryEditor.setMaterial(event.getCursor().getType());

                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Category material changed to: " + event.getCursor().getType().toString().toLowerCase());
                    categoryEditor.open();
                }
                return;
        }
    }

}
