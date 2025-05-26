package me.bounser.nascraft.commands.admin.marketeditor.propertieseditor;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class PropertiesEditorManager {

    private HashMap<Player, PropertiesEditor> playerMenus = new HashMap<>();

    private static PropertiesEditorManager instance;

    public static PropertiesEditorManager getInstance() { return instance == null ? instance = new PropertiesEditorManager() : instance; }

    public void startEditing(Player player) {
        playerMenus.put(player, new PropertiesEditor(player));
    }

    public void clearEditing(Player player) {
        playerMenus.remove(player);
    }

    public PropertiesEditor getPropertiesEditorFromPlayer(Player player) {
        if (playerMenus.get(player) != null) return playerMenus.get(player);
        return null;
    }

}
