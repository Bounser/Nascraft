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

    public void saveTrade(Connection connection, UUID uuid, Item item, int amount, float value, boolean buy, boolean discord) {
        try {
            String selectSQL = "INSERT INTO trade_log (uuid, day, date, identifier, amount, value, buy, discord) VALUES (?,?,?,?,?,?,?,?);";
            PreparedStatement statement = connection.prepareStatement(selectSQL);

            statement.setString(1, uuid.toString());
            statement.setInt(2, NormalisedDate.getDays());
            statement.setString(3, NormalisedDate.formatDateTime(LocalDateTime.now()));
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

    public List<Trade> retrieveTrades(Connection connection, UUID uuid, int offset) {
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
                        NormalisedDate.parseDateTime(rs.getString("date")),
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

    public void purgeHistory(Connection connection) {

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
