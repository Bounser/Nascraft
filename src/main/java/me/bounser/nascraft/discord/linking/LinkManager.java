package me.bounser.nascraft.discord.linking;

import me.bounser.nascraft.database.SQLite;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class LinkManager {

    private HashMap<String, String> userToUUID = new HashMap<>();

    private HashMap<Integer, String> confirmingCodes = new HashMap<>();

    private static LinkManager instance;

    public static LinkManager getInstance() { return instance == null ? instance = new LinkManager() : instance; }

    public String getUserDiscordID(String uuid) {
        return SQLite.getInstance().getUserId(uuid);
    }

    public String getUUID(User user) {

        if (userToUUID.containsKey(user.getId())) {

            return userToUUID.get(user.getId());

        } else {

            String uuid = SQLite.getInstance().getUUID(user.getId());

            if (uuid != null) {
                userToUUID.put(user.getId(), uuid);

                return uuid;
            }
        }

        return null;
    }

    public boolean codeExists(int code) { return confirmingCodes.containsKey(code); }

    public int startLinkingProcess(String userId) {

        int retrievedCode = getCodeFromUser(userId);

        if (retrievedCode != -1) return retrievedCode;

        int randomNumber = new Random().nextInt(100000) + 100;

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

    public boolean redeemCode(int code, UUID uuid) {

        if (confirmingCodes.keySet().contains(code)) {

            userToUUID.put(String.valueOf(confirmingCodes.get(code)), uuid.toString());

            SQLite.getInstance().saveLink(String.valueOf(confirmingCodes.get(code)), uuid.toString());

            confirmingCodes.remove(code);

            return true;
        }

        return false;
    }
}
