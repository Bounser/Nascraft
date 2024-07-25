package me.bounser.nascraft.database;

import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.unit.Item;
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
    void retrieveItems();
    float retrieveLastPrice(Item item);

    //

    void saveTrade(Trade trade);
    List<Trade> retrieveTrades(UUID uuid, int offset, int limit);
    List<Trade> retrieveTrades(Item item, int offset, int limit);
    List<Trade> retrieveTrades(int offset, int limit);
    void purgeHistory();

    //

    void updateItem(UUID uuid, Item item, int quantity);
    void removeItem(UUID uuid, Item item);
    void clearInventory(UUID uuid);
    void updateCapacity(UUID uuid, int capacity);
    LinkedHashMap<Item, Integer> retrieveInventory(UUID uuid);
    int retrieveCapacity(UUID uuid);

    //

    void saveCPIValue(float indexValue);

    List<CPIInstant> getCPIHistory();
    List<Instant> getPriceAgainstCPI(Item item);
    void addTransaction(float newFlow, float effectiveTaxes);
    List<DayInfo> getDayInfos();

    void addAlert(String userid, Item item, float price);
    void removeAlert(String userid, Item item);
    void retrieveAlerts();
    void removeAllAlerts(String userid);
    void purgeAlerts();

}
