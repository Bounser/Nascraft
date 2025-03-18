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
import me.bounser.nascraft.portfolio.Portfolio;

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
            connection = DriverManager.getConnection("jdbc:sqlite:" + PATH);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }

        createTables();
    }

    @Override
    public void disconnect() {
        saveEverything();
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

        createTable(connection, "portfolios_worth",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "day INT," +
                        "worth DOUBLE");

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

        createTable(connection, "loans",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "debt DOUBLE NOT NULL");

        createTable(connection, "interests",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "paid DOUBLE NOT NULL");

        createTable(connection, "user_names",
                "id INTEGER PRIMARY KEY, " +
                        "uuid VARCHAR(36) NOT NULL," +
                        "name TEXT NOT NULL");

    }

    @Override
    public void saveEverything() {
        for (Item item : MarketManager.getInstance().getAllParentItems()) {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
                ItemProperties.saveItem(connection, item);
            } catch (SQLException e) {
                Nascraft.getInstance().getLogger().warning(e.getMessage());
            }
        }
    }

    @Override
    public void saveLink(String userId, UUID uuid, String nickname) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            DiscordLink.saveLink(connection, userId, uuid, nickname);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void removeLink(String userId) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            DiscordLink.removeLink(connection, userId);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public UUID getUUID(String userId) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return DiscordLink.getUUID(connection, userId);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public String getNickname(String userId) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return DiscordLink.getNickname(connection, userId);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public String getUserId(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return DiscordLink.getUserId(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public void saveDayPrice(Item item, Instant instant) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            HistorialData.saveDayPrice(connection, item, instant);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void saveMonthPrice(Item item, Instant instant) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            HistorialData.saveMonthPrice(connection, item, instant);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void saveHistoryPrices(Item item, Instant instant) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            HistorialData.saveHistoryPrices(connection, item, instant);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public List<Instant> getDayPrices(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return HistorialData.getDayPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getMonthPrices(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return HistorialData.getMonthPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getYearPrices(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return HistorialData.getYearPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getAllPrices(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return HistorialData.getAllPrices(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Double getPriceOfDay(String identifier, int day) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return HistorialData.getPriceOfDay(connection, identifier, day);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return 0.0;
        }
    }

    @Override
    public void saveItem(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            ItemProperties.saveItem(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveItem(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            ItemProperties.retrieveItem(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveItems() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            ItemProperties.retrieveItems(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public float retrieveLastPrice(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return ItemProperties.retrieveLastPrice(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return 0;
        }
    }

    @Override
    public void saveTrade(Trade trade) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            TradesLog.saveTrade(connection, trade);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, int offset, int limit) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return TradesLog.retrieveTrades(connection, uuid, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, Item item, int offset, int limit) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return TradesLog.retrieveTrades(connection, uuid, item, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Trade> retrieveTrades(Item item, int offset, int limit) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return TradesLog.retrieveTrades(connection, item, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Trade> retrieveTrades(int offset, int limit) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return TradesLog.retrieveLastTrades(connection, offset, limit);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void purgeHistory() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            TradesLog.purgeHistory(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void updateItemPortfolio(UUID uuid, Item item, int quantity) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Portfolios.updateItemPortfolio(connection, uuid, item, quantity);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void removeItemPortfolio(UUID uuid, Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Portfolios.removeItemPortfolio(connection, uuid, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void clearPortfolio(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Portfolios.clearPortfolio(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void updateCapacity(UUID uuid, int capacity) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Portfolios.updateCapacity(connection, uuid, capacity);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public LinkedHashMap<Item, Integer> retrievePortfolio(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Portfolios.retrievePortfolio(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    @Override
    public int retrieveCapacity(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Portfolios.retrieveCapacity(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return 0;
        }
    }

    @Override
    public void logContribution(UUID uuid, Item item, int amount) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            PortfoliosLog.logContribution(connection, uuid, item, amount);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void logWithdraw(UUID uuid, Item item, int amount) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            PortfoliosLog.logWithdraw(connection, uuid, item, amount);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public HashMap<Integer, Double> getContributionChangeEachDay(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return PortfoliosLog.getContributionChangeEachDay(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return PortfoliosLog.getCompositionEachDay(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return null;
        }
    }

    @Override
    public int getFirstDay(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return PortfoliosLog.getFirstDay(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return NormalisedDate.getDays();
        }
    }

    @Override
    public void increaseDebt(UUID uuid, Double debt) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Debt.increaseDebt(connection, uuid, debt);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void decreaseDebt(UUID uuid, Double debt) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Debt.decreaseDebt(connection, uuid, debt);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public double getDebt(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Debt.getDebt(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return 0;
    }

    @Override
    public HashMap<UUID, Double> getUUIDAndDebt() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Debt.getUUIDAndDebt(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return null;
    }

    @Override
    public void addInterestPaid(UUID uuid, Double interest) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Debt.addInterestPaid(connection, uuid, interest);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public HashMap<UUID, Double> getUUIDAndInterestsPaid() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Debt.getUUIDAndInterestsPaid(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return null;
    }

    @Override
    public double getInterestsPaid(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Debt.getInterestsPaid(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return 0;
    }

    @Override
    public double getAllOutstandingDebt() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Debt.getAllOutstandingDebt(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return 0;
    }

    @Override
    public double getAllInterestsPaid() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Debt.getAllInterestsPaid(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return 0;
    }

    @Override
    public void saveOrUpdateWorth(UUID uuid, int day, double worth) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            PortfoliosWorth.saveOrUpdateWorth(connection, uuid, day, worth);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void saveOrUpdateWorthToday(UUID uuid, double worth) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            PortfoliosWorth.saveOrUpdateWorthToday(connection, uuid, worth);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public HashMap<UUID, Portfolio> getTopWorth(int n) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return PortfoliosWorth.getTopWorth(connection, n);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return null;
    }

    @Override
    public double getLatestWorth(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return PortfoliosWorth.getLatestWorth(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return 0;
    }

    @Override
    public void saveCPIValue(float indexValue) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Statistics.saveCPI(connection, indexValue);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public List<CPIInstant> getCPIHistory() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Statistics.getAllCPI(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Instant> getPriceAgainstCPI(Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Statistics.getPriceAgainstCPI(connection, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void addTransaction(double newFlow, double effectiveTaxes) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Statistics.addTransaction(connection, newFlow, effectiveTaxes);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning("Error while trying to log a transaction");
        }
    }

    @Override
    public List<DayInfo> getDayInfos() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Statistics.getDayInfos(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public double getAllTaxesCollected() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return Statistics.getAllTaxesCollected(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return 0;
    }

    @Override
    public void addAlert(String userid, Item item, double price) {
        try {
            if (connection != null && !connection.isClosed()) {
                Alerts.addAlert(connection, userid, item, price);
            } else {
                try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
                    Alerts.addAlert(connection, userid, item, price);
                } catch (SQLException e) {
                    Nascraft.getInstance().getLogger().warning(e.getMessage());
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAlert(String userid, Item item) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Alerts.removeAlert(connection, userid, item);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveAlerts() {

        Alerts.retrieveAlerts(connection);

        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAllAlerts(String userid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Alerts.removeAllAlerts(connection, userid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void purgeAlerts() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            Alerts.purgeAlerts(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void addLimitOrder(UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            LimitOrders.addLimitOrder(connection, uuid, expiration, item, type, price, amount);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void updateLimitOrder(UUID uuid, Item item, int completed, double cost) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            LimitOrders.updateLimitOrder(connection, uuid, item, completed, cost);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void removeLimitOrder(String uuid, String identifier) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            LimitOrders.removeLimitOrder(connection, uuid, identifier);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public void retrieveLimitOrders() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            LimitOrders.retrieveLimitOrders(connection);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

    @Override
    public String getNameByUUID(UUID uuid) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            return UserNames.getNameByUUID(connection, uuid);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
        return " ";
    }

    @Override
    public void saveOrUpdateName(UUID uuid, String name) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + PATH)) {
            UserNames.saveOrUpdateNick(connection, uuid, name);
        } catch (SQLException e) {
            Nascraft.getInstance().getLogger().warning(e.getMessage());
        }
    }

}