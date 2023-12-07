package me.bounser.nascraft.discord.linking;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class LinkManager {

    private HashMap<String, UUID> userToUUID = new HashMap<>();

    private HashMap<Integer, String> confirmingCodes = new HashMap<>();

    private static LinkManager instance;

    public static LinkManager getInstance() { return instance == null ? instance = new LinkManager() : instance; }

    public String getUserDiscordID(UUID uuid) { return SQLite.getInstance().getUserId(uuid); }

    public UUID getUUID(String userId) {

        if (userToUUID.containsKey(userId)) {

            return userToUUID.get(userId);

        } else {

            UUID uuid = SQLite.getInstance().getUUID(userId);

            if (uuid != null) {
                userToUUID.put(userId, uuid);

                return uuid;
            }
        }

        return null;
    }

    public boolean codeExists(int code) { return confirmingCodes.containsKey(code); }

    public int startLinkingProcess(String userId) {

        int retrievedCode = getCodeFromUser(userId);

        if (retrievedCode != -1) return retrievedCode;

        int randomNumber = new Random().nextInt(100000) + 1000;

        addCode(randomNumber, userId);

        return randomNumber;
    }

    public void addCode(int code, String userId) { confirmingCodes.put(code, userId); }

    public int getCodeFromUser(String userId) {

        for (int code : confirmingCodes.keySet())
            if(confirmingCodes.get(code).equals(userId)) return code;

        return -1;
    }

    public String getUserFromCode(int code) {

        if (confirmingCodes.containsKey(code))
            return confirmingCodes.get(code);

        return "-1";
    }

    public boolean redeemCode(int code, UUID uuid, String nickname) {

        if (confirmingCodes.keySet().contains(code)) {

            userToUUID.put(String.valueOf(confirmingCodes.get(code)), uuid);

            SQLite.getInstance().saveLink(String.valueOf(confirmingCodes.get(code)), uuid, nickname);

            confirmingCodes.remove(code);

            return true;
        }

        return false;
    }

    public boolean unlink(String userId) {

        if (!userToUUID.containsKey(userId)) {
            return false;
        }

        SQLite.getInstance().removeLink(userId);

        Player player = Bukkit.getPlayer(userToUUID.get(userId));
        if (player != null && player.getOpenInventory().getTitle().equals("Discord Inventory"))
            Bukkit.getScheduler().runTask(Nascraft.getInstance(), () -> player.closeInventory());

        userToUUID.remove(userId);

        return true;
    }
}
