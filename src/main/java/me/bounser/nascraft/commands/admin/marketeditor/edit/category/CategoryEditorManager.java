package me.bounser.nascraft.commands.admin.marketeditor.edit.category;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class CategoryEditorManager {

    private HashMap<Player, CategoryEditor> playerMenus = new HashMap<>();

    private static CategoryEditorManager instance;

    public static CategoryEditorManager getInstance() { return instance == null ? instance = new CategoryEditorManager() : instance; }


    public void startEditing(Player player) {

        playerMenus.put(player, new CategoryEditor(player));

    }

    public void clearEditing(Player player) {

        playerMenus.remove(player);

    }

    public CategoryEditor getEditCategoryFromPlayer(Player player) { return playerMenus.get(player); }

}
