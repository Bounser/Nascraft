package me.bounser.nascraft.notifications;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class NotificationsManager {

    HashMap<UUID, List<Item>> players = new HashMap<>();
    NotificationType notype;

    private static NotificationsManager instance = null;

    public static NotificationsManager getInstance() { return instance == null ? new NotificationsManager() : instance; }

    private NotificationsManager() {
        instance = this;
        notype = NotificationType.valueOf(Config.getInstance().getNotificationsMode());
    }

    public void notifyTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for(Player player : Bukkit.getOnlinePlayers()) {
                if(players.containsKey(player.getUniqueId())) {
                    notify(player, players.get(player.getUniqueId()));
                }
            }

        }, 600, Config.getInstance().getNotificationsInterval());
    }

    public void notify(Player player, List<Item> items) {

        switch(notype) {
            case CHAT:
                for(Item item : items) {
                    player.sendMessage(ChatColor.GREEN + item.getName() + " > " + item.getPrice());
                }
            case ACTIONBAR:
                // TODO
            case BOSSBAR:
                // TODO
        }
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
    }

}
