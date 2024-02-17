package me.bounser.nascraft.database.mysql;

import com.zaxxer.hikari.HikariDataSource;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class MySQL implements Database {

    private final String HOST;
    private final int PORT;
    private final String DATABASE;
    private final String USERNAME;
    private final String PASSWORD;

    private HikariDataSource hikari;

    public MySQL(String host, int port, String database, String username, String password) {
        this.HOST = host;
        this.PORT = port;
        this.DATABASE = database;
        this.USERNAME = username;
        this.PASSWORD = password;
    }

    public void connect() {
        hikari = new HikariDataSource();
        hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDateSource");

        hikari.addDataSourceProperty("serverName", HOST);
        hikari.addDataSourceProperty("port", PORT);
        hikari.addDataSourceProperty("databaseName", DATABASE);
        hikari.addDataSourceProperty("user", USERNAME);
        hikari.addDataSourceProperty("password", PASSWORD);
    }

    public boolean isConnected() { return hikari != null; }

    @Override
    public void createTables() {

    }

    @Override
    public void saveEverything() {

    }

    @Override
    public void saveLink(Connection connection, String userId, UUID uuid, String nickname) {

    }

    @Override
    public void removeLink(Connection connection, String userId) {

    }

    @Override
    public UUID getUUID(Connection connection, String userId) {
        return null;
    }

    @Override
    public String getNickname(Connection connection, String userId) {
        return null;
    }

    @Override
    public String getUserId(Connection connection, UUID uuid) {
        return null;
    }

    @Override
    public void saveDayPrice(Connection connection, Item item, Instant instant) {

    }

    @Override
    public void saveMonthPrice(Connection connection, Item item, Instant instant) {

    }

    @Override
    public void saveHistoryPrices(Connection connection, Item item, Instant instant) {

    }

    @Override
    public List<Instant> getDayPrices(Connection connection, Item item) {
        return null;
    }

    @Override
    public List<Instant> getMonthPrices(Connection connection, Item item) {
        return null;
    }

    @Override
    public List<Instant> getYearPrices(Connection connection, Item item) {
        return null;
    }

    @Override
    public List<Instant> getAllPrices(Connection connection, Item item) {
        return null;
    }

    @Override
    public void saveItem(Connection connection, Item item) {

    }

    @Override
    public void retrieveItem(Connection connection, Item item) {

    }

    @Override
    public void savePrices(Connection connection, Item item) {

    }

    @Override
    public void retrievePrices(Connection connection, Item item) {

    }

    @Override
    public float retrieveLastPrice(Connection connection, Item item) {
        return 0;
    }

    @Override
    public void saveTrade(Connection connection, UUID uuid, Item item, int amount, float value, boolean buy, boolean discord) {

    }

    @Override
    public List<Trade> retrieveTrades(Connection connection, UUID uuid, int offset) {
        return null;
    }

    @Override
    public void purgeHistory(Connection connection) {

    }

    public HikariDataSource getHikari() { return hikari; }

    public void disconnect() {
        if (!isConnected() || hikari.isClosed()) return;

        hikari.close();
    }

}
