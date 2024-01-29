package me.bounser.nascraft.database;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.brokers.BrokerType;
import me.bounser.nascraft.market.brokers.BrokersManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Instant;
import me.bounser.nascraft.market.unit.Item;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SQLite {

    private final Connection connection;

    private final String PATH = Nascraft.getInstance().getDataFolder().getPath() + "/data/sqlite.db";

    private static SQLite instance;

    public static SQLite getInstance() { return instance == null ? instance = new SQLite() : instance; }

    private SQLite() {

        createDatabaseIfNotExists();

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + PATH);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        createTable(connection, "items",
                "identifier TEXT PRIMARY KEY, " +
                        "lastprice DOUBLE, " +
                        "lowest DOUBLE, " +
                        "highest DOUBLE, " +
                        "stock DOUBLE DEFAULT 0, " +
                        "taxes DOUBLE");

        createTable(connection, "prices",
                "identifier TEXT PRIMARY KEY," +
                        "date TEXT," +
                        "dayprices TEXT," + // 48
                        "monthprices TEXT," + // 30
                        "yearprices TEXT");

        createTable(connection, "prices_day",
                "id INTEGER PRIMARY KEY, " +
                        "day INT, " +
                        "date TEXT," +
                        "identifier TEXT," +
                        "price DOUBLE," +
                        "volume INT");

        createTable(connection, "prices_month",
                "id INTEGER PRIMARY KEY, " +
                        "day INT, " +
                        "date TEXT," +
                        "identifier TEXT," +
                        "price DOUBLE," +
                        "volume INT");

        createTable(connection, "prices_history",
                "day INT  ," +
                        "identifier INT," +
                        "price DOUBLE," +
                        "volume INT," +
                        "PRIMARY KEY (identifier, day)");

        createTable(connection, "inventories",
                "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT," +
                        "amount INT");

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

//        createTable(connection, "gold_extraction",
//                "id INTEGER PRIMARY KEY, " +
//                        "day INT NOT NULL," +
//                        "quantity INT NOT NULL," +
//                        "source TEXT NOT NULL");


        purgeHistory();
    }

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

    public void saveEverything() {
        for (Item item : MarketManager.getInstance().getAllItems()) {
            saveItem(item);
            savePrices(item);
        }
    }

    public void saveItem(Item item) {
        try {
            String sql = "UPDATE items SET lastprice=?, lowest=?, highest=?, stock=?, taxes=? WHERE identifier=?;";
            PreparedStatement prep = connection.prepareStatement(sql);

            prep.setFloat(1, item.getPrice().getValue());
            prep.setFloat(2, item.getPrice().getHistoricalLow());
            prep.setFloat(3, item.getPrice().getHistoricalHigh());
            prep.setFloat(4, item.getPrice().getStock());
            prep.setFloat(5, item.getCollectedTaxes());

            prep.setString(6, item.getIdentifier());

            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void retrieveItem(Item item) {

        try {
            String sql = "SELECT lowest, highest, stock, taxes FROM items WHERE identifier=?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, item.getIdentifier());
            ResultSet rs = prep.executeQuery();

            if (rs.next()) {
                item.getPrice().setStock(rs.getInt("stock"));
                item.getPrice().setHistoricalHigh(rs.getFloat("highest"));
                item.getPrice().setHistoricalLow(rs.getFloat("lowest"));
                item.setCollectedTaxes(rs.getFloat("taxes"));
            } else {
                String sqlinsert = "INSERT INTO items (identifier, lastprice, lowest, highest, stock, taxes) VALUES (?,?,?,?,?,?);";

                PreparedStatement insertPrep = connection.prepareStatement(sqlinsert);
                insertPrep.setString(1, item.getIdentifier());
                insertPrep.setFloat(2, Config.getInstance().getInitialPrice(item.getIdentifier()));
                insertPrep.setFloat(3, Config.getInstance().getInitialPrice(item.getIdentifier()));
                insertPrep.setFloat(4, Config.getInstance().getInitialPrice(item.getIdentifier()));
                insertPrep.setFloat(5, 0);
                insertPrep.setFloat(6, 0);

                item.getPrice().setStock(0);
                item.getPrice().setHistoricalHigh(Config.getInstance().getInitialPrice(item.getIdentifier()));
                item.getPrice().setHistoricalLow(Config.getInstance().getInitialPrice(item.getIdentifier()));
                item.setCollectedTaxes(0);

                insertPrep.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void savePrices(Item item) {

        try {
            String updateSQL = "UPDATE prices SET date=?, dayprices=?, monthprices=?, yearprices=? WHERE identifier=?;";

            PreparedStatement preparedStatement = connection.prepareStatement(updateSQL);

            preparedStatement.setString(1, formatDateTime(LocalDateTime.now()));
            preparedStatement.setString(2, item.getPrices(TimeSpan.DAY).toString());
            preparedStatement.setString(3, item.getPrices(TimeSpan.MONTH).toString());
            preparedStatement.setString(4, item.getPrices(TimeSpan.YEAR).toString());
            preparedStatement.setString(5, item.getIdentifier());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveDayPrice(Item item, Instant instant) {

        try {
            String insert = "INSERT INTO prices_day (day, identifier, date, price, volume) VALUES (?,?,?,?,?);";

            PreparedStatement insertStatement = connection.prepareStatement(insert);

            insertStatement.setInt(1, getDays());
            insertStatement.setString(2, item.getIdentifier());
            insertStatement.setString(3, instant.getLocalDateTime().toString());
            insertStatement.setDouble(4, instant.getPrice());
            insertStatement.setInt(5, instant.getVolume());

            insertStatement.executeUpdate();

            String deleteQuery = "DELETE FROM prices_day WHERE day < ?;";

            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);

            deleteStatement.setInt(1, getDays()-2);

            deleteStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveMonthPrice(Item item, Instant instant) {

        try {
            String select = "SELECT price FROM prices_month WHERE identifier=? AND day=?;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());
            preparedStatement.setInt(2, getDays());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                String insert = "INSERT INTO prices_month (day, identifier, date, price, volume) VALUES (?,?,?,?,?);";

                PreparedStatement insertStatement = connection.prepareStatement(insert);

                insertStatement.setInt(1, getDays());
                insertStatement.setString(2, item.getIdentifier());
                insertStatement.setString(3, instant.getLocalDateTime().toString());
                insertStatement.setDouble(3, instant.getPrice());
                insertStatement.setInt(4, instant.getVolume());

                insertStatement.executeUpdate();
            } else if (parseDateTime(resultSet.getString("date")).isBefore(LocalDateTime.now().minusHours(9))) {

                String insert = "INSERT INTO prices_month (day, identifier, date, price, volume) VALUES (?,?,?,?,?);";

                PreparedStatement insertStatement = connection.prepareStatement(insert);

                insertStatement.setInt(1, getDays());
                insertStatement.setString(2, item.getIdentifier());
                insertStatement.setString(3, instant.getLocalDateTime().toString());
                insertStatement.setDouble(3, instant.getPrice());
                insertStatement.setInt(4, instant.getVolume());

                insertStatement.executeUpdate();

                String deleteQuery = "DELETE FROM prices_month WHERE day < ?;";

                PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);

                deleteStatement.setInt(1, getDays()-31);

                deleteStatement.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveHistoryPrices(Item item, Instant instant) {

        try {
            String select = "SELECT price FROM prices_history WHERE identifier=? AND day=?;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());
            preparedStatement.setInt(2, getDays());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                String insert = "INSERT INTO prices_history (day, identifier, date, price, volume) VALUES (?,?,?,?,?);";

                PreparedStatement insertStatement = connection.prepareStatement(insert);

                insertStatement.setInt(1, getDays());
                insertStatement.setString(2, item.getIdentifier());
                insertStatement.setString(3, instant.getLocalDateTime().toString());
                insertStatement.setDouble(3, instant.getPrice());
                insertStatement.setInt(4, instant.getVolume());

                insertStatement.executeUpdate();
            } else if (parseDateTime(resultSet.getString("date")).isBefore(LocalDateTime.now().minusHours(32))){

                String insert = "INSERT INTO prices_history (day, identifier, date, price, volume) VALUES (?,?,?,?,?);";

                PreparedStatement insertStatement = connection.prepareStatement(insert);

                insertStatement.setInt(1, getDays());
                insertStatement.setString(2, item.getIdentifier());
                insertStatement.setString(3, instant.getLocalDateTime().toString());
                insertStatement.setDouble(3, instant.getPrice());
                insertStatement.setInt(4, instant.getVolume());

                insertStatement.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Instant> getDayPrices(Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT date FROM prices_day WHERE identifier=? ORDER BY id DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusHours(24), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT date, price, volume FROM prices_day WHERE identifier=? ORDER BY id DESC LIMIT 288;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.parse(resultSet1.getString("date"));

                    if (time.isAfter(LocalDateTime.now().minusHours(24))) {
                        prices.add(new Instant(
                                time,
                                resultSet1.getFloat("price"),
                                resultSet1.getInt("volume")
                        ));

                    }
                }

                prices.add(0, new Instant(LocalDateTime.now().minusHours(24), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public List<Instant> getMonthPrices(Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT date FROM prices_month WHERE identifier=? ORDER BY id DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusDays(30), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT date, price, volume FROM prices_month WHERE identifier=? ORDER BY id DESC LIMIT 279;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.parse(resultSet1.getString("date"));

                    if (time.isAfter(LocalDateTime.now().minusDays(30))) {
                        prices.add(new Instant(
                                time,
                                resultSet1.getFloat("price"),
                                resultSet1.getInt("volume")
                        ));

                    }
                }

                prices.add(0, new Instant(LocalDateTime.now().minusDays(30), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public List<Instant> getYearPrices(Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT day FROM prices_history WHERE identifier=? ORDER BY day DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusDays(365), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT day, price, volume FROM prices_history WHERE identifier=? ORDER BY day DESC LIMIT 385;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.of(2023, 1, 1, 1, 1).plusDays(resultSet1.getInt("day"));

                    if (time.isAfter(LocalDateTime.now().minusDays(365))) {
                        prices.add(new Instant(
                                time,
                                resultSet1.getFloat("price"),
                                resultSet1.getInt("volume")
                        ));

                    }
                }

                prices.add(0, new Instant(LocalDateTime.now().minusDays(365), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public List<Instant> getAllPrices(Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT day FROM prices_history WHERE identifier=? ORDER BY day DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusDays(30), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT day, price, volume FROM prices_history WHERE identifier=? ORDER BY day DESC;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.of(2023, 1, 1, 1, 1);

                    prices.add(new Instant(
                            time.plusDays(resultSet1.getInt("day")),
                            resultSet1.getFloat("price"),
                            resultSet1.getInt("volume")
                    ));
                }

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public void retrievePrices(Item item) {

        try {

            String selectSQL = "SELECT date, dayprices, monthprices, yearprices FROM prices WHERE identifier=?;";
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {

                List<Float> dayprices = parseList(resultSet.getString("dayprices"));
                List<Float> monthprices = parseList(resultSet.getString("monthprices"));
                List<Float> yearprices = parseList(resultSet.getString("yearprices"));

                LocalDateTime date = parseDateTime(resultSet.getString("date"));

                Duration duration = Duration.between(date, LocalDateTime.now());

                if (duration.toHours() > 1) {
                    addToPriceList(dayprices, dayprices.get(dayprices.size()-1), Math.round(duration.toHours()));
                    addToPriceList(monthprices, monthprices.get(monthprices.size()-1), Math.round(duration.toDays()));
                    addToPriceList(yearprices, yearprices.get(yearprices.size()-1), (int) (duration.toDays()/7));
                }

                item.setPrice(TimeSpan.DAY, dayprices);
                item.setPrice(TimeSpan.MONTH, monthprices);
                item.setPrice(TimeSpan.YEAR, yearprices);
            } else {

                String insertSQL = "INSERT INTO prices (identifier, date, dayprices, monthprices, yearprices) VALUES (?,?,?,?,?);";

                PreparedStatement statement = connection.prepareStatement(insertSQL);

                statement.setString(1, item.getIdentifier());
                statement.setString(2, formatDateTime(LocalDateTime.now()));
                statement.setString(3, Collections.nCopies(48, Config.getInstance().getInitialPrice(item.getIdentifier())).toString());
                statement.setString(4, Collections.nCopies(30, Config.getInstance().getInitialPrice(item.getIdentifier())).toString());
                statement.setString(5, Collections.nCopies(51, Config.getInstance().getInitialPrice(item.getIdentifier())).toString());

                statement.executeUpdate();
            }

            resultSet.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public float retrieveLastPrice(Item item) {
        try {
            String selectSQL = "SELECT lastprice FROM items WHERE identifier = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getFloat("lastprice");
            } else {
                return Config.getInstance().getInitialPrice(item.getIdentifier());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addToPriceList(List<Float> prices, Float valueToAdd, int times) {
        for (int i = 0; i < times; i++) {
            prices.add(valueToAdd);
            prices.remove(0);
        }
    }

    private List<Float> parseList(String listString) {
        String[] tokens = listString.substring(1, listString.length() - 1).split(",");
        List<Float> list = new ArrayList<>();
        for (String token : tokens)
            list.add(Float.parseFloat(token.trim()));

        return list;
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    private static LocalDateTime parseDateTime(String dateTimeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    public void saveTrade(UUID uuid, Item item, int amount, float value, boolean buy, boolean discord) {
        try {
            String selectSQL = "INSERT INTO trade_log (uuid, day, date, identifier, amount, value, buy, discord) VALUES (?,?,?,?,?,?,?,?);";
            PreparedStatement statement = connection.prepareStatement(selectSQL);

            statement.setString(1, uuid.toString());
            statement.setInt(2, getDays());
            statement.setString(3, formatDateTime(LocalDateTime.now()));
            statement.setString(4, item.getIdentifier());
            statement.setInt(5, amount);
            statement.setFloat(6, RoundUtils.round(value));
            statement.setBoolean(7, buy);
            statement.setBoolean(8, discord);

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Trade> retrieveTrades(UUID uuid, int offset) {
        try {
            List<Trade> trades = new ArrayList<>();
            String sql = "SELECT * FROM trade_log WHERE uuid = ? ORDER BY id DESC LIMIT 16 OFFSET ?;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, uuid.toString());
            statement.setInt(2, offset);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {

                Trade trade = new Trade(
                        MarketManager.getInstance().getItem(rs.getString("identifier")),
                        parseDateTime(rs.getString("date")),
                        rs.getFloat("value"),
                        rs.getInt("amount"),
                        rs.getBoolean("buy"),
                        rs.getBoolean("discord"));

                trades.add(trade);
            }
            return trades;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveLink(String userId, UUID uuid, String nickname) {
        try {
            String sql = "INSERT INTO discord_links (userid, uuid, nickname) VALUES (?,?,?);";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.setString(2, uuid.toString());
            prep.setString(3, nickname);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeLink(String userId) {
        try {
            String sql = "DELETE FROM discord_links WHERE userid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getUUID(String userId) {
        try {
            String sql = "SELECT uuid FROM discord_links WHERE userid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) { return UUID.fromString(resultSet.getString("uuid")); }
            else { return null; }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getNickname(String userId) {
        try {
            String sql = "SELECT nickname FROM discord_links WHERE userid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) { return resultSet.getString("nickname"); }
            else { return null; }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserId(UUID uuid) {
        try {
            String sql = "SELECT userid FROM discord_links WHERE uuid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("userid");
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateItem(UUID uuid, Item item, int quantity) {
        try {
            String sql1 = "SELECT amount FROM inventories WHERE uuid=? AND identifier=?;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            prep1.setString(2, item.getIdentifier());
            ResultSet resultSet = prep1.executeQuery();

            if(resultSet.next()) {
                String sql2 = "UPDATE inventories SET amount=? WHERE uuid=? AND identifier=?;";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setInt(1, quantity);
                prep2.setString(2, uuid.toString());
                prep2.setString(3, item.getIdentifier());
                prep2.executeUpdate();
            } else {
                String sql2 = "INSERT INTO inventories (uuid, identifier, amount) VALUES (?,?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setString(2, item.getIdentifier());
                prep2.setInt(3, quantity);
                prep2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeItem(UUID uuid, Item item) {
        try {
            String sql = "DELETE FROM inventories WHERE uuid=? AND identifier=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            prep.setString(2, item.getIdentifier());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearInventory(UUID uuid) {
        try {
            String sql = "DELETE FROM inventories WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCapacity(UUID uuid, int capacity) {
        try {
            String sql = "UPDATE capacities SET capacity=? WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setInt(1, capacity);
            prep.setString(2, uuid.toString());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public LinkedHashMap<Item, Integer> retrieveInventory(UUID uuid) {

        LinkedHashMap<Item, Integer> content = new LinkedHashMap<>();

        try {
            String sql = "SELECT identifier, amount FROM inventories WHERE uuid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            while (resultSet.next()) {
                content.put(MarketManager.getInstance().getItem(resultSet.getString("identifier")), resultSet.getInt("amount"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return content;
    }

    public int retrieveCapacity(UUID uuid) {
        try {
            String sql = "SELECT capacity FROM capacities WHERE uuid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("capacity");
            } else {
                String sql2 = "INSERT INTO capacities (uuid, capacity) VALUES (?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setInt(2, Config.getInstance().getDefaultSlots());
                prep2.executeUpdate();

                return Config.getInstance().getDefaultSlots();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public float getBrokerSharePrice(BrokerType brokerType) {

        try {
            String sql = "SELECT lastvalue FROM shares_value WHERE broker=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, brokerType.toString().toLowerCase());
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getFloat("lastvalue");
            } else {
                String sql2 = "INSERT INTO shares_value (broker, day, lastvalue) VALUES (?,?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, brokerType.toString().toLowerCase());
                prep2.setInt(2, getDays());
                prep2.setFloat(3, 1);
                prep2.executeUpdate();

                return 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateSharePrice(BrokerType brokerType, double value) {

        try {
            double price = value;
            if (value <= 0) {
                price = 1;
                BrokersManager.getInstance().getBroker(brokerType).setValue(1);
                try {
                    String delete = "DELETE FROM broker_shares WHERE broker=?;";
                    PreparedStatement deletePrep = connection.prepareStatement(delete);
                    deletePrep.setString(1, brokerType.toString());
                    deletePrep.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            String sql = "UPDATE shares_value SET lastvalue=? WHERE broker=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setDouble(1, price);
            prep.setString(2, brokerType.toString());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public double retrieveShares(BrokerType brokerType, UUID uuid) {
        try {
            String sql = "SELECT quantity FROM broker_shares WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            return resultSet.next() ? resultSet.getDouble("lastvalue") : 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public double retrieveCost(BrokerType brokerType, UUID uuid) {
        try {
            String sql = "SELECT cost FROM broker_shares WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            return resultSet.next() ? resultSet.getDouble("lastvalue") : 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateShares(UUID uuid, double quantity, float cost) {
        try {
            String sql = "UPDATE broker_shares SET quantity=?, cost=? WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setDouble(1, quantity);
            prep.setFloat(2, cost);
            prep.setString(2, uuid.toString());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void purgeHistory() {

        int offset = Config.getInstance().getDatabasePurgeDays();
        if (offset == -1) return;

        try {
            String sql = "DELETE FROM trade_log WHERE day<?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setDouble(1, getDays() - offset);
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        saveEverything();
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getDays() {
        LocalDate startDate = LocalDate.of(2023, 1, 1);

        LocalDate currentDate = LocalDate.now();
        long daysDifference = ChronoUnit.DAYS.between(startDate, currentDate);
        int daysDifferenceInt = (int) daysDifference;

        return daysDifferenceInt;
    }
}