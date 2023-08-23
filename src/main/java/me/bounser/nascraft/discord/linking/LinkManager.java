package me.bounser.nascraft.discord.linking;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class LinkManager {

    HashMap<String, String> playerToUser;

    HashMap<Integer, UUID> confirmingCodes = new HashMap<>();

    private static LinkManager instance;

    public static LinkManager getInstance() { return instance == null ? instance = new LinkManager() : instance; }

    public LinkManager() {
        Json links = new Json("users-links", Nascraft.getInstance().getDataFolder().getPath() + "/data/links");

        playerToUser = (HashMap<String, String>) links.getMap("linked");
    }

    public void saveLinks() {
        Json links = new Json("users-links", Nascraft.getInstance().getDataFolder().getPath() + "/data/links");

        links.set("linked", playerToUser);
    }

    public String getUserDiscordID(Player player) {
        if (playerToUser.containsKey(String.valueOf(player.getUniqueId()))) {
            return playerToUser.get(String.valueOf(player.getUniqueId()));
        }
        return "-1";
    }

    public String getUUID(User user) {

        if (playerToUser.values().contains(user.getId())) {

            for (String UUIDKey : playerToUser.keySet()) {
                if (playerToUser.get(UUIDKey).equals(user.getId())) {
                    return UUIDKey;
                }
            }
        }
        return "-1";
    }

    public void addCode(int code, UUID uuid) { confirmingCodes.put(code, uuid); }

    public boolean isLinking(Player player) { return confirmingCodes.values().contains(player.getUniqueId()); }

    public int getCodeFromPlayer(Player player) {

        for (int code : confirmingCodes.keySet()) {

            if(confirmingCodes.get(code).equals(player.getUniqueId())) return code;

        }

        return -1;
    }

    public boolean redeemCode(int code, User user) {

        if (confirmingCodes.keySet().contains(code)) {

            playerToUser.put(String.valueOf(confirmingCodes.get(code)), user.getId());

            return true;
        }

        return false;
    }

}
