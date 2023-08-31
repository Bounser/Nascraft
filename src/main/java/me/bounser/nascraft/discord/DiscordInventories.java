package me.bounser.nascraft.discord;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.RoundUtils;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class DiscordInventories {


    public HashMap<String, LinkedHashMap<Item, Integer>> inventories = new HashMap<>();

    public HashMap<String, Integer> capacity = new HashMap<>();

    private static DiscordInventories instance;

    public static DiscordInventories getInstance() { return instance == null ? instance = new DiscordInventories() : instance; }

    public void retrieveInventory(String userID) {

        if (inventories.get(userID) != null) return;

        Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

        LinkedHashMap<Item, Integer> content = new LinkedHashMap<>();

        for (String item : inventoryFile.getSection(userID + ".items").keySet()) {

            content.put(MarketManager.getInstance().getItem(item), inventoryFile.getInt(userID + ".items." + item));
        }

        inventories.put(userID, content);
    }

    public void retrieveCapacity(String userID) {

        if (capacity.get(userID) == null) {
            Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

            if (!inventoryFile.contains(userID + ".capacity")) { inventoryFile.set(userID + ".capacity", Config.getInstance().getDefaultSlots()); }

            capacity.put(userID, inventoryFile.getInt(userID + ".capacity"));
        }
    }

    public void saveInventories() {

        for (String userID : inventories.keySet()) {

            Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

            for (Item item : inventories.get(userID).keySet())
                inventoryFile.set(userID + ".items." + item.getMaterial(), inventories.get(userID).get(item));

            inventoryFile.set(userID + ".capacity", capacity.get(userID));
        }

        inventories.clear();
    }

    public HashMap<Item, Integer> getInventory(String userID) {
        retrieveInventory(userID);
        return inventories.get(userID);
    }

    public int getCapacity(String userID) { retrieveCapacity(userID); return capacity.get(userID); }

    public void increaseCapacity(String userID) { capacity.put(userID, capacity.get(userID) + 1); }

    public boolean hasSpace(String userID, Item item, int amount) {

        retrieveInventory(userID);
        retrieveCapacity(userID);

        return (capacity.get(userID) > inventories.get(userID).keySet().size() && !inventories.get(userID).containsKey(item)) ||
                (inventories.get(userID).containsKey(item) && inventories.get(userID).get(item)+amount <= 999);
    }

    public void addItem(String userID, Item item, int amount) {

        retrieveInventory(userID);

        LinkedHashMap<Item, Integer> content = inventories.get(userID);

        if (content == null) content = new LinkedHashMap<>();

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

        LinkedHashMap<Item, Integer> content = inventories.get(userID);

        if (content != null && content.containsKey(item)) {
            content.put(item, content.get(item)-amount);

            if (content.get(item) == 0) { content.remove(item); }
            inventories.put(userID, content);
        }
    }

    public float getInventoryValue(User user) {

        float value = 0;

        for (Item item : inventories.get(user.getId()).keySet()) {

            value += item.getPrice().getValue()*inventories.get(user.getId()).get(item);
        }

        return RoundUtils.round(value);
    }

    public float getNextSlotPrice(User user) {

        return Config.getInstance().getSlotPriceBase() + Config.getInstance().getSlotPriceFactor()*(capacity.get(user.getId())+1);

    }

}
