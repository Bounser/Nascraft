package me.bounser.nascraft.discord;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;

import java.util.HashMap;

public class DiscordInventories {

    public HashMap<String, HashMap<Item, Integer>> inventories = new HashMap<>();

    public HashMap<String, Integer> capacity = new HashMap<>();

    private static DiscordInventories instance;

    public static DiscordInventories getInstance() { return instance == null ? instance = new DiscordInventories() : instance; }

    public void retrieveInventory(String userID) {

        if (inventories.get(userID) != null) return;

        Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

        HashMap<Item, Integer> content = new HashMap<>();

        for (String item : inventoryFile.getSection(userID + ".items").keySet()) {

            content.put(MarketManager.getInstance().getItem(item), inventoryFile.getInt(userID + ".items." + item));
        }

        inventories.put(userID, content);
    }

    public void retrieveCapacity(String userID) {

        if (capacity.get(userID) == null) {
            Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

            if (!inventoryFile.contains(userID + ".capacity")) { inventoryFile.set(userID + ".capacity", 10); }

            capacity.put(userID, inventoryFile.getInt(userID + ".capacity"));
        }
    }

    public void saveInventories() {

        for (String userID : inventories.keySet()) {

            Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

            for (Item item : inventories.get(userID).keySet()) {

                inventoryFile.set(userID + ".items." + item.getMaterial(), inventories.get(userID).get(item));

            }
        }

        inventories.clear();
    }

    public HashMap<Item, Integer> getInventory(String userID) {
        retrieveInventory(userID);
        return inventories.get(userID);
    }

    public int getCapacity(String userID) { retrieveCapacity(userID); return capacity.get(userID); }

    public boolean hasSpace(String userID, Item item) {

        retrieveInventory(userID);
        retrieveCapacity(userID);

        return capacity.get(userID) > inventories.get(userID).keySet().size() || inventories.get(userID).containsKey(item);
    }

    public void addItem(String userID, Item item, int amount) {

        retrieveInventory(userID);

        HashMap<Item, Integer> content = inventories.get(userID);

        if (content == null) content = new HashMap<>();

        if (content.get(item) == null) content.put(item, amount);
        else content.put(item, content.get(item)+amount);

        inventories.put(userID, content);
    }

    public boolean hasItem(String userID, Item item, int amount) {

        retrieveInventory(userID);

        return inventories.get(userID) != null &&
                inventories.get(userID).get(item) != null &&
                inventories.get(userID).get(item) >= amount;
    }


    public void removeItem(String userID, Item item, int amount) {

        retrieveInventory(userID);

        HashMap<Item, Integer> content = inventories.get(userID);

        if (content != null && content.containsKey(item)) {
            content.put(item, content.get(item)-amount);

            if (content.get(item) == 0) { content.remove(item); }
            inventories.put(userID, content);
        }
    }
}
