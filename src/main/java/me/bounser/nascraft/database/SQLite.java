package me.bounser.nascraft.database;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.managers.TasksManager;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class SQLite {

    private final Connection connection;

    private final String PATH = Nascraft.getInstance().getDataFolder().getPath() + "/data/sqlite.db";

    private static SQLite instance;

    public static SQLite getInstance() { return instance == null ? instance = new SQLite() : instance; }

    private SQLite() {

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + PATH);

            Statement itemsStatement = connection.createStatement();
            itemsStatement.execute("CREATE TABLE IF NOT EXISTS items ("+
                    "material TEXT PRIMARY KEY," +
                    "lowest DOUBLE, " +
                    "highest DOUBLE, " +
                    "stock DOUBLE DEFAULT 0, " +
                    "taxes DOUBLE" +
                    ");");

            Statement pricesStatement = connection.createStatement();
            pricesStatement.execute("CREATE TABLE IF NOT EXISTS prices (" +
                    "material TEXT PRIMARY KEY," +
                    "date TEXT," +
                    "dayprices TEXT," + // 48
                    "monthprices TEXT," + // 30
                    "yearprices TEXT" + // 51
                    ");");

            Statement inventoriesStatement = connection.createStatement();
            inventoriesStatement.execute("CREATE TABLE IF NOT EXISTS inventories ("+
                    "userid TEXT," +
                    "material TEXT," +
                    "amount INT" +
                    ");");

            Statement capacitySatatement = connection.createStatement();
            capacitySatatement.execute("CREATE TABLE IF NOT EXISTS capacities ("+
                    "userid TEXT PRIMARY KEY," +
                    "capacity INT" +
                    ");");

            Statement linksStatement = connection.createStatement();
            linksStatement.execute("CREATE TABLE IF NOT EXISTS discord_links (" +
                    "userid VARCHAR(18) NOT NULL," +
                    "uuid VARCHAR(36) NOT NULL" +
                    ");");

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
            String sql = "REPLACE INTO items (material, lowest, highest, stock, taxes) "
                    + "VALUES (?,?,?,?,?);";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, item.getMaterial());
            prep.setFloat(2, item.getPrice().getHistoricalLow());
            prep.setFloat(3, item.getPrice().getHistoricalHigh());
            prep.setFloat(4, item.getPrice().getStock());
            prep.setFloat(5, item.getCollectedTaxes());

            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void retrieveItem(Item item) {

        try {
            String sql = "SELECT lowest, highest, stock, taxes FROM items WHERE material=?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, item.getMaterial());
            ResultSet rs = prep.executeQuery();

            item.getPrice().setStock(rs.getInt("stock"));
            item.getPrice().setHistoricalHigh(rs.getFloat("highest"));
            item.getPrice().setHistoricalLow(rs.getFloat("lowest"));
            item.setCollectedTaxes(rs.getFloat("taxes"));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void savePrices(Item item) {

        try {
            String insertSQL = "REPLACE INTO prices (material, date, dayprices, monthprices, yearprices) VALUES (?,?,?,?,?);";

            PreparedStatement preparedStatement = connection.prepareStatement(insertSQL);

            String array48String = Arrays.toString(item.getPrices(TimeSpan.DAY).toArray());
            String array30String = Arrays.toString(item.getPrices(TimeSpan.MONTH).toArray());
            String array40String = Arrays.toString(item.getPrices(TimeSpan.YEAR).toArray());

            preparedStatement.setString(1, item.getMaterial());
            preparedStatement.setString(2, formatDateTime(LocalDateTime.now()));
            preparedStatement.setString(3, array48String);
            preparedStatement.setString(4, array30String);
            preparedStatement.setString(5, array40String);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void retrievePrices(Item item) {

        try {

            String selectSQL = "SELECT date, dayprices, monthprices, yearprices FROM prices WHERE material = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);

            preparedStatement.setString(1, item.getMaterial());

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

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
            }

            resultSet.close();

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

    public void saveLink(String userId, String uuid) {
        try {
            String sql = "REPLACE INTO discord_links (userid, uuid) VALUES (?,?);";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.setString(2, uuid);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUUID(String userId) {
        try {
            String sql = "SELECT uuid FROM discord_links WHERE userid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) { return resultSet.getString("uuid"); }
            else { return null; }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserId(String uuid) {
        try {
            String sql = "SELECT userid FROM discord_links WHERE uuid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid);
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


    public void updateItem(String userId, Item item, int quantity) {
        try {
            String sql1 = "SELECT amount FROM inventories WHERE userid=? AND material=?;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, userId);
            prep1.setString(2, item.getMaterial());
            ResultSet resultSet = prep1.executeQuery();

            if(resultSet.next()) {
                String sql2 = "UPDATE inventories SET amount=? WHERE userid=? AND material=?;";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setInt(1, quantity);
                prep2.setString(2, userId);
                prep2.setString(3, item.getMaterial());
                prep2.executeUpdate();
            } else {
                String sql2 = "INSERT INTO inventories (userid, material, amount) VALUES (?,?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, userId);
                prep2.setString(2, item.getMaterial());
                prep2.setInt(3, quantity);
                prep2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeItem(String userId, Item item) {
        try {
            String sql = "DELETE FROM inventories WHERE userid=? AND material=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.setString(2, item.getMaterial());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCapacity(String userId, int capacity) {
        try {
            String sql = "UPDATE capacities SET capacity=? WHERE userid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setInt(1, capacity);
            prep.setString(2, userId);
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public LinkedHashMap<Item, Integer> retrieveInventory(String userId) {

        LinkedHashMap<Item, Integer> content = new LinkedHashMap<>();

        try {
            String sql = "SELECT material, amount FROM inventories WHERE userid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            while (resultSet.next()) {
                content.put(MarketManager.getInstance().getItem(resultSet.getString("material")), resultSet.getInt("amount"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return content;
    }

    public int retrieveCapacity(String userId) {
        try {
            String sql = "SELECT capacity FROM capacities WHERE userid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("capacity");
            } else {
                String sql2 = "INSERT INTO capacities (userid, capacity) VALUES (?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, userId);
                prep2.setInt(2, Config.getInstance().getDefaultSlots());
                prep2.executeUpdate();

                return Config.getInstance().getDefaultSlots();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {

        TasksManager.getInstance().save();

        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
