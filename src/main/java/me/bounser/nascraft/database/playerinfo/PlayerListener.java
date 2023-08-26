package me.bounser.nascraft.database.playerinfo;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        PlayerInfoManager.getInstance().stopTrackingPlayer(event.getPlayer());
    }
}