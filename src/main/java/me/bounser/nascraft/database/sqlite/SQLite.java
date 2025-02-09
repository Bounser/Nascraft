package me.bounser.nascraft.database.sqlite;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.commands.*;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SQLite implements Database {

    private Connection connection;

    private final String PATH = Nascraft.getInstance().getDataFolder().getPath() + "/data/sqlite.db";

    private static SQLite instance;

    public static SQLite getInstance() { return instance == null ? instance = new SQLite() : instance; }

    private void createDatabaseIfNotExists() {
        File databaseFile = new File(PATH);
        if (!databaseFile.exists()) {
            try {
                File parentDir = databaseFile.getParentFile();
                if (!parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs();
                    if (!dirsCreated) {
                        throw new RuntimeException("Failed to create directories for the database file.");
                    }
                }

                boolean fileCreated = databaseFile.createNewFile();
                if (!fileCreated) {
                    throw new RuntimeException("Failed to create the database file.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void createTable(Connection connection, String tableName, String columns) {
        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ");");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connect() {

        createDatabaseIfNotExists();

        try {
            ensureDatabaseConnectionIsOpen();
            createTables();
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        saveEverything();
        try {
            connection.close();
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        connection = null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void createTables() {

        createTable(connection, "items",
                "identifier TEXT PRIMARY KEY, " +
                        "lastprice DOUBLE, " +
                        "lowest DOUBLE, " +
                        "highest DOUBLE, " +
                        "stock DOUBLE DEFAULT 0, " +
                        "taxes DOUBLE");

        createTable(connection, "prices_day",
                "id INTEGER PRIMARY KEY, " +
                        "day INT, " +
                        "date TEXT," +
                        "identifier TEXT," +
                        "price DOUBLE," +
                        "volume INT");

        createTable(connection, "prices_month",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL, " +
                        "date TEXT NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "price DOUBLE NOT NULL," +
                        "volume INT NOT NULL");

        createTable(connection, "prices_history",
                "id INTEGER PRIMARY KEY, " +
                        "day INT," +
                        "date TEXT NOT NULL," +
                        "identifier INT," +
                        "price DOUBLE," +
                        "volume INT");

        createTable(connection, "portfolios",
                "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT," +
                        "amount INT");

        createTable(connection, "portfolios_log",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "day INT," +
                        "identifier TEXT," +
                        "amount INT," +
                        "contribution DOUBLE");

        createTable(connection, "capacities",
                "uuid VARCHAR(36) PRIMARY KEY," +
                        "capacity INT");

        createTable(connection, "discord_links",
                "userid VARCHAR(18) NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "nickname TEXT NOT NULL");

        createTable(connection, "trade_log",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "amount INT NOT NULL," +
                        "value TEXT NOT NULL," +
                        "buy INT NOT NULL, " +
                        "discord INT NOT NULL");

        createTable(connection, "cpi",
                "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "value DOUBLE NOT NULL");

        createTable(connection, "alerts",
                "day INT NOT NULL," +
                        "userid TEXT NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "price DOUBLE NOT NULL");

        createTable(connection, "flows",
                "day INT PRIMARY KEY," +
                        "flow DOUBLE NOT NULL," +
                        "taxes DOUBLE NOT NULL," +
                        "operations INT NOT NULL," +
                        "UNIQUE(day)");

        createTable(connection, "limit_orders",
                "id INTEGER PRIMARY KEY, " +
                        "expiration TEXT NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "type INT NOT NULL," +
                        "price DOUBLE NOT NULL," +
                        "to_complete INT NOT NULL," +
                        "completed INT NOT NULL," +
                        "cost INT NOT NULL");

        /*

                createTable(connection, "to_deliver",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "amount INT NOT NULL");

        createTable(connection, "broker_shares",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "date TEXT NOT NULL," +
                        "broker TEXT NOT NULL," +
                        "quantity DOUBLE NOT NULL," +
                        "cost DOUBLE NOT NULL");

        createTable(connection, "shares_value",
                "id INTEGER PRIMARY KEY, " +
                        "broker TEXT NOT NULL," +
                        "day INT NOT NULL," +
                        "lastvalue TEXT NOT NULL");

        createTable(connection, "limit_orders",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "expiration TEXT NOT NULL," +
                        "cost DOUBLE NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "price DOUBLE NOT NULL," +
                        "quantity INT NOT NULL");

        createTable(connection, "to_deliver",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "quantity INT NOT NULL");

        createTable(connection, "expired_orders",
                "id INTEGER PRIMARY KEY, " +
                        "day INT NOT NULL," +
                        "date TEXT NOT NULL," +
                        "uuid VARCHAR(36) NOT NULL," +
                        "money DOUBLE NOT NULL," +
                        "identifier TEXT NOT NULL," +
                        "quantity INT NOT NULL");

         */

    }

    @Override
    public void saveEverything() {
        try {
            ensureDatabaseConnectionIsOpen();
            for (Item item : MarketManager.getInstance().getAllParentItems()) {
                ItemProperties.saveItem(connection, item);
            }
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void saveLink(String userId, UUID uuid, String nickname) {
        try {
            ensureDatabaseConnectionIsOpen();
            DiscordLink.saveLink(connection, userId, uuid, nickname);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void removeLink(String userId) {
        try {
            ensureDatabaseConnectionIsOpen();
            DiscordLink.removeLink(connection, userId);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public UUID getUUID(String userId) {
        try {
            ensureDatabaseConnectionIsOpen();
            return DiscordLink.getUUID(connection, userId);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public String getNickname(String userId) {
        try {
            ensureDatabaseConnectionIsOpen();
            return DiscordLink.getNickname(connection, userId);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public String getUserId(UUID uuid) {
        try {
            ensureDatabaseConnectionIsOpen();
            return DiscordLink.getUserId(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public void saveDayPrice(Item item, Instant instant) {
        try {
            ensureDatabaseConnectionIsOpen();
            HistorialData.saveDayPrice(connection, item, instant);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void saveMonthPrice(Item item, Instant instant) {
        try {
            ensureDatabaseConnectionIsOpen();
            HistorialData.saveMonthPrice(connection, item, instant);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void saveHistoryPrices(Item item, Instant instant) {
        try {
            ensureDatabaseConnectionIsOpen();
            HistorialData.saveHistoryPrices(connection, item, instant);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public List<Instant> getDayPrices(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            return HistorialData.getDayPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getMonthPrices(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            return HistorialData.getMonthPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getYearPrices(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            return HistorialData.getYearPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getAllPrices(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            return HistorialData.getAllPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Double getPriceOfDay(String identifier, int day) {
        try {
            ensureDatabaseConnectionIsOpen();
            return HistorialData.getPriceOfDay(connection, identifier, day);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return 0.0;
        }
    }

    @Override
    public void saveItem(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            ItemProperties.saveItem(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveItem(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            ItemProperties.retrieveItem(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveItems() {
        try {
            ensureDatabaseConnectionIsOpen();
            ItemProperties.retrieveItems(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public float retrieveLastPrice(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            return ItemProperties.retrieveLastPrice(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return 0;
        }
    }

    @Override
    public void saveTrade(Trade trade) {
        try {
            ensureDatabaseConnectionIsOpen();
            TradesLog.saveTrade(connection, trade);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, int offset, int limit) {
        try {
            ensureDatabaseConnectionIsOpen();
            return TradesLog.retrieveTrades(connection, uuid, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, Item item, int offset, int limit) {
        try {
            ensureDatabaseConnectionIsOpen();
            return TradesLog.retrieveTrades(connection, uuid, item, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Trade> retrieveTrades(Item item, int offset, int limit) {
        try {
            ensureDatabaseConnectionIsOpen();
            return TradesLog.retrieveTrades(connection, item, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Trade> retrieveTrades(int offset, int limit) {
        try {
            ensureDatabaseConnectionIsOpen();
            return TradesLog.retrieveLastTrades(connection, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void purgeHistory() {
        try {
            ensureDatabaseConnectionIsOpen();
            TradesLog.purgeHistory(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void updateItem(UUID uuid, Item item, int quantity) {
        try {
            ensureDatabaseConnectionIsOpen();
            Portfolios.updateItem(connection, uuid, item, quantity);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void removeItem(UUID uuid, Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            Portfolios.removeItem(connection, uuid, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void clearInventory(UUID uuid) {
        try {
            ensureDatabaseConnectionIsOpen();
            Portfolios.clearInventory(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void updateCapacity(UUID uuid, int capacity) {
        try {
            ensureDatabaseConnectionIsOpen();
            Portfolios.updateCapacity(connection, uuid, capacity);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public LinkedHashMap<Item, Integer> retrievePortfolio(UUID uuid) {
        try {
            ensureDatabaseConnectionIsOpen();
            return Portfolios.retrievePortfolio(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    @Override
    public int retrieveCapacity(UUID uuid) {
        try {
            ensureDatabaseConnectionIsOpen();
            return Portfolios.retrieveCapacity(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return 0;
        }
    }

    @Override
    public void logContribution(UUID uuid, Item item, int amount) {
        try {
            ensureDatabaseConnectionIsOpen();
            PortfoliosLog.logContribution(connection, uuid, item, amount);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void logWithdraw(UUID uuid, Item item, int amount) {
        try {
            ensureDatabaseConnectionIsOpen();
            PortfoliosLog.logWithdraw(connection, uuid, item, amount);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public HashMap<Integer, Double> getContributionChangeEachDay(UUID uuid) {
        try {
            ensureDatabaseConnectionIsOpen();
            return PortfoliosLog.getContributionChangeEachDay(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(UUID uuid) {
        try {
            ensureDatabaseConnectionIsOpen();
            return PortfoliosLog.getCompositionEachDay(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public int getFirstDay(UUID uuid) {
        try {
            ensureDatabaseConnectionIsOpen();
            return PortfoliosLog.getFirstDay(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return NormalisedDate.getDays();
        }
    }


    @Override
    public void saveCPIValue(float indexValue) {
        try {
            ensureDatabaseConnectionIsOpen();
            Statistics.saveCPI(connection, indexValue);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public List<CPIInstant> getCPIHistory() {
        try {
            ensureDatabaseConnectionIsOpen();
            return Statistics.getAllCPI(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getPriceAgainstCPI(Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            return Statistics.getPriceAgainstCPI(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void addTransaction(double newFlow, double effectiveTaxes) {
        try {
            ensureDatabaseConnectionIsOpen();
            Statistics.addTransaction(connection, newFlow, effectiveTaxes);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning("Error while trying to log a transaction");
        }
    }

    @Override
    public List<DayInfo> getDayInfos() {
        try {
            ensureDatabaseConnectionIsOpen();
            return Statistics.getDayInfos(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void addAlert(String userid, Item item, double price) {
        try {
            ensureDatabaseConnectionIsOpen();
            Alerts.addAlert(connection, userid, item, price);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAlert(String userid, Item item) {
        try {
            ensureDatabaseConnectionIsOpen();
            Alerts.removeAlert(connection, userid, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveAlerts() {
        try {
            ensureDatabaseConnectionIsOpen();
            Alerts.retrieveAlerts(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAllAlerts(String userid) {
        try {
            ensureDatabaseConnectionIsOpen();
            Alerts.removeAllAlerts(connection, userid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void purgeAlerts() {
        try {
            ensureDatabaseConnectionIsOpen();
            Alerts.purgeAlerts(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void addLimitOrder(UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {
        try {
            ensureDatabaseConnectionIsOpen();
            LimitOrders.addLimitOrder(connection, uuid, expiration, item, type, price, amount);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void updateLimitOrder(UUID uuid, Item item, int completed, double cost) {
        try {
            ensureDatabaseConnectionIsOpen();
            LimitOrders.updateLimitOrder(connection, uuid, item, completed, cost);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void removeLimitOrder(String uuid, String identifier) {
        try {
            ensureDatabaseConnectionIsOpen();
            LimitOrders.removeLimitOrder(connection, uuid, identifier);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveLimitOrders() {
        try {
            ensureDatabaseConnectionIsOpen();
            LimitOrders.retrieveLimitOrders(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    private void ensureDatabaseConnectionIsOpen() throws SQLException {
        // Thread-safe to guarantee only one attempt at same time
        synchronized (this) {
            // Try to open connection if not set or was closed for some reason
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + PATH);
            }
        }
    }

}
