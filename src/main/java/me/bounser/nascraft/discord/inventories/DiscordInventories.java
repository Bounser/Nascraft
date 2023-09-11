package me.bounser.nascraft.discord.inventories;

import java.util.HashMap;

public class DiscordInventories {


    private final HashMap<String, DiscordInventory> inventories = new HashMap<>();

    private static DiscordInventories instance;

    public static DiscordInventories getInstance() { return instance == null ? instance = new DiscordInventories() : instance; }

    public void saveInventories() {
        for (DiscordInventory discordInventory : inventories.values()) discordInventory.save();
    }

    public DiscordInventory getInventory(String userID) {

        if (inventories.containsKey(userID)) return inventories.get(userID);

        inventories.put(userID, new DiscordInventory(userID));

        return inventories.get(userID);
    }

}
