package me.bounser.nascraft.database.mysql;

import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class MySQL implements Database {

    private final String HOST;
    private final int PORT;
    private final String DATABASE;
    private final String USERNAME;
    private final String PASSWORD;

    public MySQL(String host, int port, String database, String username, String password) {
        this.HOST = host;
        this.PORT = port;
        this.DATABASE = database;
        this.USERNAME = username;
        this.PASSWORD = password;
    }

    public void connect() {
        /*
        hikari = new HikariDataSource();
        hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDateSource");

        hikari.addDataSourceProperty("serverName", HOST);
        hikari.addDataSourceProperty("port", PORT);
        hikari.addDataSourceProperty("databaseName", DATABASE);
        hikari.addDataSourceProperty("user", USERNAME);
        hikari.addDataSourceProperty("password", PASSWORD);
    }

    public HikariDataSource getHikari() { return hikari; }

    public void disconnect() {
        if (!isConnected() || hikari.isClosed()) return;

        hikari.close();
    }

    public boolean isConnected() { return hikari != null;*/ }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void createTables() {

    }

    @Override
    public void saveEverything() {

    }

    @Override
    public void saveLink(String userId, UUID uuid, String nickname) {

    }

    @Override
    public void removeLink(String userId) {

    }

    @Override
    public UUID getUUID(String userId) {
        return null;
    }

    @Override
    public String getNickname(String userId) {
        return null;
    }

    @Override
    public String getUserId(UUID uuid) {
        return null;
    }

    @Override
    public void saveDayPrice(Item item, Instant instant) {

    }

    @Override
    public void saveMonthPrice(Item item, Instant instant) {

    }

    @Override
    public void saveHistoryPrices(Item item, Instant instant) {

    }

    @Override
    public List<Instant> getDayPrices(Item item) {
        return null;
    }

    @Override
    public List<Instant> getMonthPrices(Item item) {
        return null;
    }

    @Override
    public List<Instant> getYearPrices(Item item) {
        return null;
    }

    @Override
    public List<Instant> getAllPrices(Item item) {
        return null;
    }

    @Override
    public Double getPriceOfDay(String identifier, int day) {
        return 0.0;
    }

    @Override
    public void saveItem(Item item) {

    }

    @Override
    public void retrieveItem(Item item) {

    }

    @Override
    public void retrieveItems() {

    }


    @Override
    public float retrieveLastPrice(Item item) {
        return 0;
    }

    @Override
    public void saveTrade(Trade trade) {

    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, int offset, int limit) {
        return null;
    }

    @Override
    public List<Trade> retrieveTrades(UUID uuid, Item item, int offset, int limit) {
        return null;
    }

    @Override
    public List<Trade> retrieveTrades(Item item, int offset, int limit) {
        return null;
    }

    @Override
    public List<Trade> retrieveTrades(int offset, int limit) {
        return null;
    }

    @Override
    public void purgeHistory() {

    }

    @Override
    public void updateItem(UUID uuid, Item item, int quantity) {

    }

    @Override
    public void removeItem(UUID uuid, Item item) {

    }

    @Override
    public void clearInventory(UUID uuid) {

    }

    @Override
    public void updateCapacity(UUID uuid, int capacity) {

    }

    @Override
    public LinkedHashMap<Item, Integer> retrievePortfolio(UUID uuid) {
        return null;
    }

    @Override
    public int retrieveCapacity(UUID uuid) {
        return 0;
    }

    @Override
    public void logContribution(UUID uuid, Item item, int amount) {

    }

    @Override
    public void logWithdraw(UUID uuid, Item item, int amount) {

    }

    @Override
    public HashMap<Integer, Double> getContributionChangeEachDay(UUID uuid) {
        return null;
    }

    @Override
    public HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(UUID uuid) {
        return null;
    }

    @Override
    public int getFirstDay(UUID uuid) {
        return 0;
    }

    @Override
    public void saveCPIValue(float indexValue) {

    }

    @Override
    public List<CPIInstant> getCPIHistory() {
        return null;
    }

    @Override
    public List<Instant> getPriceAgainstCPI(Item item) {
        return null;
    }

    @Override
    public void addTransaction(double newFlow, double effectiveTaxes) {

    }

    @Override
    public List<DayInfo> getDayInfos() {
        return null;
    }

    @Override
    public void addAlert(String userid, Item item, double price) {

    }

    @Override
    public void removeAlert(String userid, Item item) {

    }

    @Override
    public void retrieveAlerts() {

    }

    @Override
    public void removeAllAlerts(String userid) {

    }

    @Override
    public void purgeAlerts() {

    }

    @Override
    public void addLimitOrder(UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {

    }

    @Override
    public void updateLimitOrder(UUID uuid, Item item, int completed, double cost) {

    }

    @Override
    public void removeLimitOrder(String uuid, String identifier) {

    }

    @Override
    public void retrieveLimitOrders() {

    }


}
