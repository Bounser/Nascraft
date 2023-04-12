package me.bounser.nascraft.notifications;

import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionEvent implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        for(Item item : MarketManager.getInstance().getAllItems()) {

            if(event.getPlayer().hasPermission("nascraft.notif." + item.getMaterialName())) {
                NotificationsManager.getInstance().addPlayer(event.getPlayer(), item);
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        NotificationsManager.getInstance().removePlayer(event.getPlayer());
    }

}
