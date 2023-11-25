package me.bounser.nascraft.discord.inventories;

import me.bounser.nascraft.discord.linking.LinkManager;

import java.util.HashMap;
import java.util.UUID;

public class DiscordInventories {


    private final HashMap<UUID, DiscordInventory> inventories = new HashMap<>();

    private static DiscordInventories instance;

    public static DiscordInventories getInstance() { return instance == null ? instance = new DiscordInventories() : instance; }

    public DiscordInventory getInventory(UUID uuid) {

        if (inventories.containsKey(uuid)) return inventories.get(uuid);

        inventories.put(uuid, new DiscordInventory(uuid));

        return inventories.get(uuid);
    }

    public DiscordInventory getInventory(String userid) {

        UUID uuid = LinkManager.getInstance().getUUID(userid);

        if (inventories.containsKey(uuid)) return inventories.get(uuid);

        inventories.put(uuid, new DiscordInventory(uuid));

        return inventories.get(uuid);
    }

}
