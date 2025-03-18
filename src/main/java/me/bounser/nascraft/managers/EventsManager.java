package me.bounser.nascraft.managers;

import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventsManager implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Database db = DatabaseManager.get().getDatabase();

        db.saveOrUpdateName(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        PortfoliosManager.getInstance().savePortfolioOfPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PortfoliosManager.getInstance().savePortfolioOfPlayer(event.getPlayer());
    }
}
