package me.bounser.nascraft.discord.linking;

import github.scarsz.discordsrv.DiscordSRV;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.sqlite.SQLite;
import me.bounser.nascraft.discord.DiscordBot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class LinkManager {

    private HashMap<String, UUID> userToUUID = new HashMap<>();

    private HashMap<Integer, String> confirmingCodes = new HashMap<>();

    private static LinkManager instance;

    private final LinkingMethod linkingMethod;

    public static LinkManager getInstance() { return instance == null ? instance = new LinkManager() : instance; }

    private LinkManager () { linkingMethod = Config.getInstance().getLinkingMethod(); }

    public String getUserDiscordID(UUID uuid) {

        switch (linkingMethod) {

            case DISCORDSRV:

                if (DiscordSRV.getPlugin().getAccountLinkManager().isInCache(uuid)) {
                    return DiscordSRV.getPlugin().getAccountLinkManager().getDiscordIdFromCache(uuid);
                }
                return DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

            case NATIVE:
                return SQLite.getInstance().getUserId(uuid);

            default: return null;
        }
    }

    public UUID getUUID(String userId) {

        switch (linkingMethod) {

            case DISCORDSRV:

                if (DiscordSRV.getPlugin().getAccountLinkManager().isInCache(userId)) {
                    return DiscordSRV.getPlugin().getAccountLinkManager().getUuidFromCache(userId);
                }
                return DiscordSRV.getPlugin().getAccountLinkManager().getUuid(userId);

            case NATIVE:

                if (userToUUID.containsKey(userId)) {

                    return userToUUID.get(userId);

                } else {

                    UUID uuid = SQLite.getInstance().getUUID(userId);

                    if (uuid != null) {
                        userToUUID.put(userId, uuid);

                        return uuid;
                    }
                }

            default: return null;
        }
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

            if (Config.getInstance().getLogChannelEnabled())
                DiscordBot.getInstance().sendLinkLog(confirmingCodes.get(code), uuid, nickname, true);

            confirmingCodes.remove(code);

            return true;
        }

        return false;
    }

    public boolean unlink(String userId) {

        switch (linkingMethod) {

            case DISCORDSRV:

                if (DiscordSRV.getPlugin().getAccountLinkManager().getUuid(userId) != null) return false;

                DatabaseManager.get().getDatabase().removeAllAlerts(userId);

                DiscordSRV.getPlugin().getAccountLinkManager().unlink(userId);

                return true;

            case NATIVE:

                if (!userToUUID.containsKey(userId)) return false;

                UUID uuid = userToUUID.get(userId);
                userToUUID.remove(userId);

                DiscordBot.getInstance().sendLinkLog(userId, uuid, SQLite.getInstance().getNickname(userId), false);

                DatabaseManager.get().getDatabase().removeLink(userId);
                DatabaseManager.get().getDatabase().removeAllAlerts(userId);

                Player player = Bukkit.getPlayer(uuid);

                if (player == null) return true;

                DiscordBot.getInstance().getJDA().retrieveUserById(userId).queue(user -> {

                    Lang.get().message(player, Message.LINK_UNLINKED, "[USER]", user.getName());

                });

                if (player.getOpenInventory().getTitle().equals(Lang.get().message(Message.PORTFOLIO_TITLE)))
                    Bukkit.getScheduler().runTask(Nascraft.getInstance(), player::closeInventory);
        }

        return true;
    }

    public LinkingMethod getLinkingMethod() { return linkingMethod; }

}
