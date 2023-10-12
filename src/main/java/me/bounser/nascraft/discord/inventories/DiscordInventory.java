package me.bounser.nascraft.discord.inventories;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.market.unit.Item;

import java.util.HashMap;
import java.util.LinkedHashMap;


public class DiscordInventory {

    private int capacity;

    private final String userId;

    private LinkedHashMap<Item, Integer> inventory = new LinkedHashMap<>();

    public DiscordInventory(String userID) {
        this.userId = userID;
        retrieveInventory();
        retrieveCapacity();
    }

    public void retrieveInventory() { inventory = SQLite.getInstance().retrieveInventory(userId); }

    public void retrieveCapacity() { capacity = SQLite.getInstance().retrieveCapacity(userId); }

    public void increaseCapacity() {
        capacity++; SQLite.getInstance().updateCapacity(userId, capacity);
    }

    public int getCapacity() { return capacity; }

    public boolean hasSpace(Item item, int amount) {
        return (capacity > inventory.keySet().size() && !inventory.containsKey(item)) ||
                (inventory.containsKey(item) && inventory.get(item)+amount <= 999);
    }

    public void addItem(Item item, int amount) {
        inventory.merge(item, amount, Integer::sum);
        SQLite.getInstance().updateItem(userId, item, inventory.get(item));
    }

    public boolean hasItem(Item item, int amount) {
        return inventory.get(item) != null && inventory.get(item) >= amount;
    }


    public void removeItem(Item item, int amount) {

        if (inventory != null && inventory.containsKey(item)) {
            inventory.put(item, inventory.get(item)-amount);

            if (inventory.get(item) == 0) {
                inventory.remove(item);
                SQLite.getInstance().removeItem(userId, item);

            }
        }
    }

    public float getInventoryValue() {

        float value = 0;

        for (Item item : inventory.keySet()) {
            if (item != null)
                value += item.getPrice().getValue()*inventory.get(item);
        }

        return value;
    }

    public float getNextSlotPrice() {
        return Config.getInstance().getSlotPriceBase() + Config.getInstance().getSlotPriceFactor()*(capacity+1);
    }

    public HashMap<Item, Integer> getContent() { return inventory; }

}
