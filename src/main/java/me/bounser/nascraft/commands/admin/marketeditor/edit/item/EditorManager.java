package me.bounser.nascraft.commands.admin.marketeditor.edit.item;

import me.bounser.nascraft.market.unit.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class EditorManager {

    private HashMap<Player, EditItemMenu> playerMenus = new HashMap<>();

    private static EditorManager instance;

    public static EditorManager getInstance() { return instance == null ? instance = new EditorManager() : instance; }

    public void startEditing(Player player, Item item) {

        playerMenus.put(player, new EditItemMenu(player, item));

    }

    public void startEditing(Player player, ItemStack itemStack) {

        playerMenus.put(player, new EditItemMenu(player, itemStack));

    }

    public void clearEditing(Player player) {

        playerMenus.remove(player);

    }

    public EditItemMenu getEditItemMenuFromPlayer(Player player) { return playerMenus.get(player); }


}
