package me.bounser.nascraft.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.unit.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SQLManager {

    /*
      MYSQL FORMAT:



     */

    Config config = Config.getInstance();

    // private Connection connection;
    HikariDataSource hikari;

    private final String ADDRESS = config.address();
    private final int PORT = config.port();
    private final String DATABASE = config.database();
    private final String USER = config.user();
    private final String PASSWORD = config.password();

    private static SQLManager instance;

    public static SQLManager getInstance() { return instance == null ? instance = new SQLManager() : instance; }

    public void connect() throws SQLException {
        // connection = DriverManager.getConnection("jdbc:mysql://"+ADDRESS+":"+PORT+"/"+DATABASE+"?useSSL=false", USER, PASSWORD);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" +
                ADDRESS +
                ":" +
                PORT +
                "/" +
                DATABASE +
                "??useSSL=false");
        hikariConfig.setDriverClassName("com.mysql.jdbc.Driver");
        hikariConfig.setUsername(USER);
        hikariConfig.setPassword(PASSWORD);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setMaximumPoolSize(100);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setLeakDetectionThreshold(3000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

    }

    public Connection getConnection() throws SQLException { return hikari.getConnection(); }

    public void disconnect() {
        if(hikari != null) {
            hikari.close();
        }
    }

    public void savePrices() throws SQLException {

        int day = Data.getDay();
        int hour = Data.getHour();

        PreparedStatement ps = hikari.getConnection().prepareStatement("INSERT INTO table (C1,C2,C3) VALUES (?,?,?);");
        ps.setString(1, "value");
        ps.setString(2, "value");
        ps.setString(3, "value");
        ps.executeUpdate();

        for (Item item : MarketManager.getInstance().getAllItems()) {
            // HISTORY
            String historyQuery = "INSERT INTO price_history (material, lastSaveD, lastSaveH, day, price, stock) VALUES (?, ?, ?, ?, ?, ?)";
            ps = hikari.getConnection().prepareStatement(historyQuery);
            ps.setString(1, item.getMaterial());
            ps.setInt(2, day);
            ps.setInt(3, hour);
            ps.setInt(4, day);
            ps.setFloat(5, item.getPrice().getValue());
            ps.setInt(6, item.getPrice().getStock());
            ps.executeUpdate();

            // RECENT
            int x = 0;
            for (float i : item.getPrices(TimeSpan.DAY)) {
                String recentQuery = "INSERT INTO recent_prices (material, hour, price) VALUES (?, ?, ?)";
                ps = hikari.getConnection().prepareStatement(recentQuery);
                ps.setString(1, item.getMaterial());
                int recentHour = (hour - x < 0) ? (hour + 23 - x) : (hour - x);
                ps.setInt(2, recentHour);
                ps.setFloat(3, i);
                ps.executeUpdate();
                x++;
            }
        }
    }

    public List<Float> getMPrice(String mat) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<Float> prices;

        // Verify if the data is present fot that material.
        String query = "SELECT hour, price FROM recent_prices WHERE material = ? ORDER BY hour DESC LIMIT 1";

        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, mat);

        resultSet = statement.executeQuery();

        if (resultSet.next()) {
            float price = resultSet.getFloat("price");
            int lastSaveH = resultSet.getInt("hour");
            prices = new ArrayList<>(Collections.nCopies(30, price));
        } else {
            float price = Config.getInstance().getInitialPrice(mat);
            prices = new ArrayList<>(Collections.nCopies(30, price));
        }
        return prices;
    }

    public List<Float> getHPrice(String mat) throws SQLException {

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<Float> prices = new ArrayList<>(Collections.nCopies(24, 0f));

        String query = "SELECT hour, price FROM recent_prices WHERE material = ? ORDER BY hour DESC LIMIT 24";
        statement = connection.prepareStatement(query);
        statement.setString(1, mat);
        resultSet = statement.executeQuery();

        int index = 0;
        while (resultSet.next()) {
            float price = resultSet.getFloat("price");
            prices.set(index, price);
            index++;
        }

        if (index < 24) {
            float initialPrice = Config.getInstance().getInitialPrice(mat);
            for (int i = index; i < 24; i++) {
                prices.set(i, initialPrice);
            }
        }
        return  prices;
    }

    public List<Float> getMMPrice(String mat) throws SQLException {

        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<Float> prices = new ArrayList<>(Collections.nCopies(30, 0f));

        int day = Data.getDay();

        String query = "SELECT price FROM price_history WHERE material = ? AND day >= ? ORDER BY day DESC LIMIT 30";
        statement = hikari.getConnection().prepareStatement(query);
        statement.setString(1, mat);
        statement.setInt(2, day - 29);
        resultSet = statement.executeQuery();

        int index = 29;
        while (resultSet.next()) {
            float price = resultSet.getFloat("price");
            prices.set(index, price);
            index--;
        }

        if (index >= 0) {
            float initialPrice = Config.getInstance().getInitialPrice(mat);
            for (int i = index; i >= 0; i--) {
                prices.set(i, initialPrice);
            }
        }

        return prices;
    }

    public List<Float> getYPrice(String mat) throws SQLException {

        PreparedStatement statement = null;
        ResultSet resultSet = null;

        List<Float> prices = new ArrayList<>(Collections.nCopies(30, 0f));

        int day = Data.getDay();

        String query = "SELECT price FROM price_history WHERE material = ? AND day >= ? ORDER BY day DESC LIMIT 30";
        statement = hikari.getConnection().prepareStatement(query);
        statement.setString(1, mat);
        statement.setInt(2, day - 29 * 15);
        resultSet = statement.executeQuery();

        int index = 29;
        while (resultSet.next()) {
            float price = resultSet.getFloat("price");
            prices.set(index, price);
            index--;
        }

        if (index >= 0) {
            float initialPrice = Config.getInstance().getInitialPrice(mat);
            for (int i = index; i >= 0; i--) {
                prices.set(i, initialPrice);
            }
        }
        return prices;
    }

}
