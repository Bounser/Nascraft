package me.bounser.nascraft.portfolio;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.inventorygui.Portfolio.PortfolioInventory;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Consumer;


public class Portfolio {

    private int capacity;

    private final UUID uuid;

    private LinkedHashMap<Item, Integer> inventory = new LinkedHashMap<>();

    public Portfolio(UUID uuid) {
        this.uuid = uuid;
        retrievePortfolio();
        retrieveCapacity();
    }

    public void retrievePortfolio() { inventory = DatabaseManager.get().getDatabase().retrievePortfolio(uuid); }

    public void retrieveCapacity() { capacity = DatabaseManager.get().getDatabase().retrieveCapacity(uuid); }

    public void increaseCapacity() {
        capacity++;
        DatabaseManager.get().getDatabase().updateCapacity(uuid, capacity);
        updateInventoryInGame();
    }

    public int getCapacity() { return capacity; }

    public boolean hasSpace(Item item, int amount) {
        return (capacity > inventory.keySet().size() && !inventory.containsKey(item)) ||
                (inventory.containsKey(item) && inventory.get(item)+amount <= Config.getInstance().getPortfolioMaxStorage());
    }

    public void addItem(Item item, int amount) {
        inventory.merge(item, amount, Integer::sum);
        DatabaseManager.get().getDatabase().updateItemPortfolio(uuid, item, inventory.get(item));
        DatabaseManager.get().getDatabase().logContribution(uuid, item, amount);
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
                DatabaseManager.get().getDatabase().removeItemPortfolio(uuid, item);
            }

            DatabaseManager.get().getDatabase().logWithdraw(uuid, item, amount);

            updateInventoryInGame();
        }
    }

    public double getInventoryValue() {

        double value = 0;

        for (Item item : inventory.keySet()) {
            if (item != null && item.getCurrency().equals(CurrenciesManager.getInstance().getDefaultCurrency()))
                value += item.getPrice().getValue()*inventory.get(item);
        }

        return value;
    }

    public HashMap<Currency, Double> getInventoryValuePerCurrency() {

        HashMap<Currency, Double> worth = new HashMap<>();

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

    public double getValueOfDefaultCurrency() {

        double value = 0;

        for (Item item : inventory.keySet())
            if (item != null && item.getCurrency().equals(CurrenciesManager.getInstance().getDefaultCurrency()))
                value += item.getPrice().getValue()*inventory.get(item);

        return value;
    }

    public float getNextSlotPrice() {
        return Config.getInstance().getSlotPriceBase() + Config.getInstance().getSlotPriceFactor()*(capacity+1);
    }

    public HashMap<Item, Integer> getContent() { return inventory; }

    public UUID getOwnerUUID() {
        return uuid;
    }

    private void updateInventoryInGame() {

        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            PortfolioInventory.getInstance().updatePortfolioInventory(player);
        }

    }

    public void sellAll(Consumer<Double> callback) {

        Bukkit.getScheduler().runTask(Nascraft.getInstance(), () -> {
            
            double value = 0;

            LinkedHashMap<Item, Integer> newInventory = new LinkedHashMap();
            
            for (Item item : inventory.keySet())
                if (item != null)
                    if (item.getPrice().canStockChange(inventory.get(item), false))
                        value += item.sell(inventory.get(item), uuid, false);
                    else
                        newInventory.put(item, inventory.get(item));

            inventory = newInventory;
            updateInventoryInGame();

            DatabaseManager.get().getDatabase().clearPortfolio(uuid);

            for (Item item : inventory.keySet())
                DatabaseManager.get().getDatabase().updateItemPortfolio(uuid, item, inventory.get(item));

            callback.accept(value);
        });
    }

    public void liquidatePerCurrency(Currency currency, Consumer<Double> callback) {

        Bukkit.getScheduler().runTask(Nascraft.getInstance(), () -> {

            double value = 0;

            for (Item item : inventory.keySet()) {

                if (item == null) continue;

                if (!item.getCurrency().equals(currency)) continue;

                if (item.getPrice().canStockChange(inventory.get(item), false)) {
                    value += item.sell(inventory.get(item), uuid, false);
                    DatabaseManager.get().getDatabase().removeItemPortfolio(uuid, item);
                }

            }
            retrievePortfolio();
            updateInventoryInGame();
            callback.accept(value);
        });
    }
}
