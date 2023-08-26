package me.bounser.nascraft.database.playerinfo;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class PlayerInfoManager {

    HashMap<UUID, PlayerReport> playerReports = new HashMap<>();

    private static PlayerInfoManager instance;

    public static PlayerInfoManager getInstance() { return instance == null ? instance = new PlayerInfoManager() : instance; }

    public void stopTrackingPlayer(Player player) {

        playerReports.get(player.getUniqueId()).save();
        playerReports.remove(player.getUniqueId());

    }

    public PlayerReport getPlayerReport(Player player) {

        if(playerReports.get(player.getUniqueId()) == null) {

            playerReports.put(player.getUniqueId(), new PlayerReport(player.getUniqueId()));
            return playerReports.get(player.getUniqueId());

        }
        return playerReports.get(player.getUniqueId());
    }

    public void saveEverything() {
        for (PlayerReport playerReport: playerReports.values()) { playerReport.save(); }
        playerReports.clear();
    }

}
