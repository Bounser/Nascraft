package me.bounser.nascraft.discord.inventories;

import me.bounser.nascraft.commands.discord.DiscordInventoryInGame;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;


public class DiscordInventory {

    private int capacity;

    private final UUID uuid;

    private LinkedHashMap<Item, Integer> inventory = new LinkedHashMap<>();

    public DiscordInventory(UUID uuid) {
        this.uuid = uuid;
        retrieveInventory();
        retrieveCapacity();
    }

    public void retrieveInventory() { inventory = SQLite.getInstance().retrieveInventory(uuid); }

    public void retrieveCapacity() { capacity = SQLite.getInstance().retrieveCapacity(uuid); }

    public void increaseCapacity() {
        capacity++;
        SQLite.getInstance().updateCapacity(uuid, capacity);
        updateInventoryInGame();
    }

    public int getCapacity() { return capacity; }

    public boolean hasSpace(Item item, int amount) {
        return (capacity > inventory.keySet().size() && !inventory.containsKey(item)) ||
                (inventory.containsKey(item) && inventory.get(item)+amount <= 999);
    }

    public void addItem(Item item, int amount) {
        inventory.merge(item, amount, Integer::sum);
        SQLite.getInstance().updateItem(uuid, item, inventory.get(item));
        updateInventoryInGame();
    }

    public boolean hasItem(Item item, int amount) {
        return inventory.get(item) != null && inventory.get(item) >= amount;
    }


    public void removeItem(Item item, int amount) {

        if (inventory != null && inventory.containsKey(item)) {
            inventory.put(item, inventory.get(item)-amount);

            if (inventory.get(item) <= 0) {
                inventory.remove(item);
                SQLite.getInstance().removeItem(uuid, item);
            }

            updateInventoryInGame();
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

    private void updateInventoryInGame() {

        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            DiscordInventoryInGame.getInstance().updateDiscordInventory(player);
        }

    }

    public float sellAll() {

        float value = 0;

        for (Item item : inventory.keySet()) {
            if (item != null)
                value += item.sellItem(inventory.get(item), uuid, false, item.getItemStack().getType());
        }

        inventory.clear();
        SQLite.getInstance().clearInventory(uuid);
        return value;
    }

}
