package me.bounser.nascraft.database;

import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

public interface Database {

    void connect();
    void disconnect();
    boolean isConnected();

    void createTables();

    void saveEverything();

    //

    void saveLink(Connection connection, String userId, UUID uuid, String nickname);
    void removeLink(Connection connection, String userId);
    UUID getUUID(Connection connection, String userId);
    String getNickname(Connection connection, String userId);
    String getUserId(Connection connection, UUID uuid);

    //

    void saveDayPrice(Connection connection, Item item, Instant instant);
    void saveMonthPrice(Connection connection, Item item, Instant instant);
    void saveHistoryPrices(Connection connection, Item item, Instant instant);
    List<Instant> getDayPrices(Connection connection, Item item);
    List<Instant> getMonthPrices(Connection connection, Item item);
    List<Instant> getYearPrices(Connection connection, Item item);
    List<Instant> getAllPrices(Connection connection, Item item);

    //

    void saveItem(Connection connection, Item item);
    void retrieveItem(Connection connection, Item item);
    void savePrices(Connection connection, Item item);
    void retrievePrices(Connection connection, Item item);
    float retrieveLastPrice(Connection connection, Item item);

    //

    void saveTrade(Connection connection, UUID uuid, Item item, int amount, float value, boolean buy, boolean discord);
    List<Trade> retrieveTrades(Connection connection, UUID uuid, int offset);
    void purgeHistory(Connection connection);

}
