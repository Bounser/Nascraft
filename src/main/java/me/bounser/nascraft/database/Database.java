package me.bounser.nascraft.database;

import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import me.bounser.nascraft.portfolio.Portfolio;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    Double getPriceOfDay(String identifier, int day);

    //

    void saveItem(Item item);
    void retrieveItem(Item item);
    void retrieveItems();
    float retrieveLastPrice(Item item);

    //

    void saveTrade(Trade trade);
    List<Trade> retrieveTrades(UUID uuid, int offset, int limit);
    List<Trade> retrieveTrades(UUID uuid, Item item, int offset, int limit);
    List<Trade> retrieveTrades(Item item, int offset, int limit);
    List<Trade> retrieveTrades(int offset, int limit);
    void purgeHistory();

    //

    void updateItemPortfolio(UUID uuid, Item item, int quantity);
    void removeItemPortfolio(UUID uuid, Item item);
    void clearPortfolio(UUID uuid);
    void updateCapacity(UUID uuid, int capacity);
    LinkedHashMap<Item, Integer> retrievePortfolio(UUID uuid);
    int retrieveCapacity(UUID uuid);

    void increaseDebt(UUID uuid, Double debt);
    void decreaseDebt(UUID uuid, Double debt);
    double getDebt(UUID uuid);
    HashMap<UUID, Double> getUUIDAndDebt();
    void addInterestPaid(UUID uuid, Double interest);
    HashMap<UUID, Double> getUUIDAndInterestsPaid();
    double getInterestsPaid(UUID uuid);
    double getAllOutstandingDebt();
    double getAllInterestsPaid();

    void saveOrUpdateWorth(UUID uuid, int day, double worth);
    void saveOrUpdateWorthToday(UUID uuid, double worth);
    HashMap<UUID, Portfolio> getTopWorth(int n);
    double getLatestWorth(UUID uuid);

    //

    void logContribution(UUID uuid, Item item, int amount);
    void logWithdraw(UUID uuid, Item item, int amount);
    HashMap<Integer, Double> getContributionChangeEachDay(UUID uuid);
    HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(UUID uuid);
    int getFirstDay(UUID uuid);

    //

    void saveCPIValue(float indexValue);

    List<CPIInstant> getCPIHistory();
    List<Instant> getPriceAgainstCPI(Item item);
    void addTransaction(double newFlow, double effectiveTaxes);
    List<DayInfo> getDayInfos();
    double getAllTaxesCollected();

    void addAlert(String userid, Item item, double price);
    void removeAlert(String userid, Item item);
    void retrieveAlerts();
    void removeAllAlerts(String userid);
    void purgeAlerts();

    void addLimitOrder(UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount);
    void updateLimitOrder(UUID uuid, Item item, int completed, double cost);
    void removeLimitOrder(String uuid, String identifier);
    void retrieveLimitOrders();

    String getNameByUUID(UUID uuid);
    void saveOrUpdateName(UUID uuid, String name);

}
