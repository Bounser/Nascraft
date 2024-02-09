package me.bounser.nascraft.commands.admin.marketeditor.overview;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class MarketEditorManager {

    private HashMap<Player, MarketEditor> playerMenus = new HashMap<>();

    private static MarketEditorManager instance;

    public static MarketEditorManager getInstance() { return instance == null ? instance = new MarketEditorManager() : instance; }

    public void startEditing(Player player) {
        playerMenus.put(player, new MarketEditor(player));
    }

    public void clearEditing(Player player) {
        playerMenus.remove(player);
    }

    public MarketEditor getMarketEditorFromPlayer(Player player) {
        if (playerMenus.get(player) != null) return playerMenus.get(player);
        return null;
    }

}
