package me.bounser.nascraft.database.playerinfo;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.UUID;

public class PlayerReport {

    UUID playerUUID;

    int operations;

    float capitalGains;

    float volumeMoved;

    float taxesCollected;

    HashMap<Item, Integer> history;

    public PlayerReport(UUID playerUUID) {
        this.playerUUID = playerUUID;

        Json report = new Json( "info-" + playerUUID, Nascraft.getInstance().getDataFolder().getPath() + "/data/players/" + playerUUID);

        if (report.contains("operations"))
        operations = report.getInt("operations");
        else operations = 0;

        if (report.contains("capitalGains"))
            capitalGains = report.getFloat("capitalGains");
        else capitalGains = 0;

        if (report.contains("volumeMoved"))
            volumeMoved = report.getFloat("volumeMoved");
        else volumeMoved = 0;

        if (report.contains("taxesCollected"))
            taxesCollected = report.getFloat("taxesCollected");
        else taxesCollected = 0;
    }

    public void save() {

        Json report = new Json("info-" + playerUUID, Nascraft.getInstance().getDataFolder().getPath() + "/data/players/" + playerUUID);

        report.set("operations", operations);
        report.set("capitalGains", capitalGains);
        report.set("volumeMoved", volumeMoved);
        report.set("taxesCollected", taxesCollected);

        Json historyFile = new Json("history-" + playerUUID, Nascraft.getInstance().getDataFolder().getPath() + "/data/players/" + playerUUID);

        for (Item item : history.keySet()) {

            int operations = historyFile.getInt(item.getMaterial());

            historyFile.set(item.getMaterial(), operations + history.get(item));
        }
    }

    public void update(int operations, float capitalGains, float taxesCollected) {
        this.operations += operations;
        this.capitalGains += capitalGains;
        this.volumeMoved += Math.abs(capitalGains);
        this.taxesCollected += taxesCollected;
    }

    public void setDiscordID(User user) {
        Json report = new Json("info-" + playerUUID, Nascraft.getInstance().getDataFolder().getPath() + "/data/players/" + playerUUID);

        report.set("discordID", user.getId());
    }

    public String getUserDiscordID() {
        Json report = new Json("info-" + playerUUID, Nascraft.getInstance().getDataFolder().getPath() + "/data/players/" + playerUUID);

        if(report.contains("discordID")) {
            return report.getString("discordID");
        }

        return "not-registered";
    }

}
