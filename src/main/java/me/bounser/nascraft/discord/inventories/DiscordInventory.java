package me.bounser.nascraft.discord.inventories;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.numbers.RoundUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;


public class DiscordInventory {

    private int capacity = 0;

    private final String userID;

    private LinkedHashMap<Item, Integer> inventory = new LinkedHashMap<>();

    public DiscordInventory(String userID) { this.userID = userID; }

    public void save() {

        Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

        for (Item item : inventory.keySet())
            inventoryFile.set(userID + ".items." + item.getMaterial(), inventory.get(item));

        inventoryFile.set(userID + ".capacity", capacity);
    }

    public void retrieveInventory() {

        if (!inventory.isEmpty()) return;

        Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

        LinkedHashMap<Item, Integer> content = new LinkedHashMap<>();

        for (String item : inventoryFile.getSection(userID + ".items").keySet()) {

            content.put(MarketManager.getInstance().getItem(item), inventoryFile.getInt(userID + ".items." + item));
        }

        inventory = content;
    }

    public void retrieveCapacity() {

        if (capacity != 0) return;

        Json inventoryFile = new Json("inventory-" + userID, Nascraft.getInstance().getDataFolder().getPath() + "/data/inventories");

        if (!inventoryFile.contains(userID + ".capacity")) { inventoryFile.set(userID + ".capacity", Config.getInstance().getDefaultSlots()); }

        capacity = inventoryFile.getInt(userID + ".capacity");
    }

    public void increaseCapacity() { capacity++; }

    public int getCapacity() { return capacity; }

    public boolean hasSpace(Item item, int amount) {

        retrieveInventory();
        retrieveCapacity();

        return (capacity > inventory.keySet().size() && !inventory.containsKey(item)) ||
                (inventory.containsKey(item) && inventory.get(item)+amount <= 999);
    }

    public void addItem(Item item, int amount) {

        retrieveInventory();
        inventory.merge(item, amount, Integer::sum);
    }

    public boolean hasItem(Item item, int amount) {

        retrieveInventory();

        return inventory != null &&
               inventory.get(item) >= amount;
    }


    public void removeItem(Item item, int amount) {

        retrieveInventory();

        if (inventory != null && inventory.containsKey(item)) {
            inventory.put(item, inventory.get(item)-amount);

            if (inventory.get(item) == 0) { inventory.remove(item); }
        }
    }

    public float getInventoryValue() {

        float value = 0;

        for (Item item : inventory.keySet()) {

            value += item.getPrice().getValue()*inventory.get(item);
        }

        return RoundUtils.round(value);
    }

    public float getNextSlotPrice() {
        return Config.getInstance().getSlotPriceBase() + Config.getInstance().getSlotPriceFactor()*(capacity+1);
    }

    public HashMap<Item, Integer> getContent() { return inventory; }

}
