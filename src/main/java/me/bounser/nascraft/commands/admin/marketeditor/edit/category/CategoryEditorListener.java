package me.bounser.nascraft.commands.admin.marketeditor.edit.category;

import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;


public class CategoryEditorListener implements Listener {

    private static CategoryEditorListener instance = null;

    public static CategoryEditorListener getInstance() { return instance == null ? new CategoryEditorListener() : instance; }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (!event.getWhoClicked().hasPermission("nascraft.admin")) return;

        if (event.getView().getTopInventory().getSize() != 45 || !event.getView().getTitle().equals("§8§lEditing Categories") || event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();

        event.setCancelled(true);

        if (event.getRawSlot() == 8) {

            CategoryEditorManager.getInstance().clearEditing(player);

            new MarketEditor(player);

        }

        CategoryEditor categoryEditor = CategoryEditorManager.getInstance().getEditCategoryFromPlayer(player);

        if (event.getRawSlot() == 36) {

            int offset = categoryEditor.getOffset();

            categoryEditor.decreaseOffset();

            CategoryEditorManager.getInstance().getEditCategoryFromPlayer(player).insertCategories(event.getInventory());

            return;
        }

        if (event.getRawSlot() == 44) {

            categoryEditor.increaseOffset();

            CategoryEditorManager.getInstance().getEditCategoryFromPlayer(player).insertCategories(event.getInventory());

            return;
        }

    }


}
