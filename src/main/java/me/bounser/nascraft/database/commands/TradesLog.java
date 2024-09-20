package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradesLog {

    public static void saveTrade(Connection connection, Trade trade) {
        try {
            String selectSQL = "INSERT INTO trade_log (uuid, day, date, identifier, amount, value, buy, discord) VALUES (?,?,?,?,?,?,?,?);";
            PreparedStatement statement = connection.prepareStatement(selectSQL);

            statement.setString(1, trade.getUuid().toString());
            statement.setInt(2, NormalisedDate.getDays());
            statement.setString(3, NormalisedDate.formatDateTime(LocalDateTime.now()));
            statement.setString(4, trade.getItem().getIdentifier());
            statement.setInt(5, trade.getAmount());
            statement.setFloat(6, RoundUtils.round(trade.getValue()));
            statement.setBoolean(7, trade.isBuy());
            statement.setBoolean(8, trade.throughDiscord());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Trade> retrieveTrades(Connection connection, UUID uuid, int offset, int limit) {

        if (uuid == null) return null;

        try {
            List<Trade> trades = new ArrayList<>();
            String sql = "SELECT * FROM trade_log WHERE uuid = ? ORDER BY id DESC LIMIT " + limit + " OFFSET ?;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, uuid.toString());
            statement.setInt(2, offset);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {

                Trade trade = new Trade(
                        MarketManager.getInstance().getItem(rs.getString("identifier")),
                        NormalisedDate.parseDateTime(rs.getString("date")),
                        rs.getFloat("value"),
                        rs.getInt("amount"),
                        rs.getBoolean("buy"),
                        rs.getBoolean("discord"),
                        uuid
                );

                trades.add(trade);
            }
            return trades;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Trade> retrieveTrades(Connection connection, UUID uuid, Item item, int offset, int limit) {

        if (uuid == null) return null;

        try {
            List<Trade> trades = new ArrayList<>();
            String sql = "SELECT * FROM trade_log WHERE uuid = ? AND identifier = ? ORDER BY id DESC LIMIT " + limit + " OFFSET ?;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, uuid.toString());
            statement.setString(2, item.getIdentifier());
            statement.setInt(3, offset);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {

                Trade trade = new Trade(
                        MarketManager.getInstance().getItem(rs.getString("identifier")),
                        NormalisedDate.parseDateTime(rs.getString("date")),
                        rs.getFloat("value"),
                        rs.getInt("amount"),
                        rs.getBoolean("buy"),
                        rs.getBoolean("discord"),
                        uuid
                );

                trades.add(trade);
            }
            return trades;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Trade> retrieveTrades(Connection connection, Item item, int offset, int limit) {
        try {
            List<Trade> trades = new ArrayList<>();
            String sql = "SELECT * FROM trade_log WHERE identifier = ? ORDER BY id DESC LIMIT " + limit + " OFFSET ?;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, item.getIdentifier());
            statement.setInt(2, offset);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {

                Trade trade = new Trade(
                        item,
                        NormalisedDate.parseDateTime(rs.getString("date")),
                        rs.getFloat("value"),
                        rs.getInt("amount"),
                        rs.getBoolean("buy"),
                        rs.getBoolean("discord"),
                        UUID.fromString(rs.getString("uuid"))
                );

                trades.add(trade);
            }
            return trades;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Trade> retrieveLastTrades(Connection connection, int offset, int limit) {
        try {
            List<Trade> trades = new ArrayList<>();
            String sql = "SELECT * FROM trade_log ORDER BY id DESC LIMIT " + limit + " OFFSET ?;";

            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, offset);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {

                Trade trade = new Trade(
                        MarketManager.getInstance().getItem(rs.getString("identifier")),
                        NormalisedDate.parseDateTime(rs.getString("date")),
                        rs.getFloat("value"),
                        rs.getInt("amount"),
                        rs.getBoolean("buy"),
                        rs.getBoolean("discord"),
                        UUID.fromString(rs.getString("uuid"))
                );

                trades.add(trade);
            }
            return trades;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void purgeHistory(Connection connection) {

        int offset = Config.getInstance().getDatabasePurgeDays();
        if (offset == -1) return;

        try {
            String sql = "DELETE FROM trade_log WHERE day<?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setDouble(1, NormalisedDate.getDays() - offset);
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
