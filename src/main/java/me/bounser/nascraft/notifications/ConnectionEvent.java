package me.bounser.nascraft.notifications;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionEvent implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        NotificationsManager.getInstance().removePlayer(event.getPlayer());
    }

}
