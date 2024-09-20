package me.bounser.nascraft.discord.inventories;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.discord.DiscordInventoryInGame;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Consumer;


public class DiscordInventory {

    private int capacity;

    private final UUID uuid;

    private LinkedHashMap<Item, Integer> inventory = new LinkedHashMap<>();

    public DiscordInventory(UUID uuid) {
        this.uuid = uuid;
        retrieveInventory();
        retrieveCapacity();
    }

    public void retrieveInventory() { inventory = DatabaseManager.get().getDatabase().retrieveInventory(uuid); }

    public void retrieveCapacity() { capacity = DatabaseManager.get().getDatabase().retrieveCapacity(uuid); }

    public void increaseCapacity() {
        capacity++;
        DatabaseManager.get().getDatabase().updateCapacity(uuid, capacity);
        updateInventoryInGame();
    }

    public int getCapacity() { return capacity; }

    public boolean hasSpace(Item item, int amount) {
        return (capacity > inventory.keySet().size() && !inventory.containsKey(item)) ||
                (inventory.containsKey(item) && inventory.get(item)+amount <= 999);
    }

    public void addItem(Item item, int amount) {
        inventory.merge(item, amount, Integer::sum);
        DatabaseManager.get().getDatabase().updateItem(uuid, item, inventory.get(item));
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
                DatabaseManager.get().getDatabase().removeItem(uuid, item);
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

    public HashMap<Currency, Float> getInventoryValuePerCurrency() {

        HashMap<Currency, Float> worth = new HashMap<>();

        for (Item item : inventory.keySet()) {

            if (item != null) {

                if (worth.containsKey(item.getCurrency())) {
                    worth.put(item.getCurrency(), worth.get(item.getCurrency()) + item.getPrice().getValue()*inventory.get(item));
                } else {
                    worth.put(item.getCurrency(), item.getPrice().getValue()*inventory.get(item));
                }
            }
        }

        return worth;
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

    public void sellAll(Consumer<Float> callback) {

        Bukkit.getScheduler().runTask(Nascraft.getInstance(), () -> {
            
            float value = 0;
            
            for (Item item : inventory.keySet())
                if (item != null) value += item.sell(inventory.get(item), uuid, false);

            inventory.clear();
            updateInventoryInGame();
            DatabaseManager.get().getDatabase().clearInventory(uuid);

            callback.accept(value);
        });
    }
}
