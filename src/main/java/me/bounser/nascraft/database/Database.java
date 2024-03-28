package me.bounser.nascraft.database;

import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.Tradable;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public interface Database {

    void connect();
    void disconnect();
    boolean isConnected();

    void createTables();

    void saveEverything();

    //

    void saveLink(String userId, UUID uuid, String nickname);
    void removeLink(String userId);
    UUID getUUID(String userId);
    String getNickname(String userId);
    String getUserId(UUID uuid);

    //

    void saveDayPrice(Item item, Instant instant);
    void saveMonthPrice(Item item, Instant instant);
    void saveHistoryPrices(Item item, Instant instant);
    List<Instant> getDayPrices(Item item);
    List<Instant> getMonthPrices(Item item);
    List<Instant> getYearPrices(Item item);
    List<Instant> getAllPrices(Item item);

    //

    void saveItem(Item item);
    void retrieveItem(Item item);
    float retrieveLastPrice(Item item);

    //

    void saveTrade(UUID uuid, Tradable tradable, int amount, float value, boolean buy, boolean discord);
    List<Trade> retrieveTrades(UUID uuid, int offset);
    void purgeHistory();

    //

    void updateItem(UUID uuid, Item item, int quantity);
    void removeItem(UUID uuid, Item item);
    void clearInventory(UUID uuid);
    void updateCapacity(UUID uuid, int capacity);
    LinkedHashMap<Item, Integer> retrieveInventory(UUID uuid);
    int retrieveCapacity(UUID uuid);


}
