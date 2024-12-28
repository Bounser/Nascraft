package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.limitorders.LimitOrder;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
import me.bounser.nascraft.market.limitorders.OrderType;
import me.bounser.nascraft.market.unit.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class LimitOrders {

    public static void addLimitOrder(Connection connection, UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {

        try {
            String sql = "INSERT INTO limit_orders (expiration, uuid, identifier, type, price, to_complete, completed, cost) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, expiration.toString());
            prep.setString(2, uuid.toString());
            prep.setString(3, item.getIdentifier());
            prep.setInt(4, type);
            prep.setDouble(5, price);
            prep.setInt(6, amount);
            prep.setInt(7, 0);
            prep.setDouble(8, 0);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateLimitOrder(Connection connection, UUID uuid, Item item, int completed, double cost) {

        try {
            String sql = "UPDATE limit_orders SET completed=?, cost=? WHERE uuid=? AND identifier=?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setInt(1, completed);
            prep.setDouble(2, cost);
            prep.setString(3, uuid.toString());
            prep.setString(4, item.getIdentifier());
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeLimitOrder(Connection connection, String uuid, String identifier) {

        try {
            String sql = "DELETE FROM limit_orders WHERE uuid = ? AND identifier = ?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid);
            prep.setString(2, identifier);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void retrieveLimitOrders(Connection connection) {

        try {
            String sql = "SELECT expiration, uuid, identifier, type, cost, price, to_complete, completed FROM limit_orders;";
            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet resultSet = prep.executeQuery();

            while (resultSet.next()) {

                LimitOrdersManager.getInstance().registerLimitOrder(
                        new LimitOrder(
                                UUID.fromString(resultSet.getString("uuid")),
                                MarketManager.getInstance().getItem(resultSet.getString("identifier")),
                                LocalDateTime.parse(resultSet.getString("expiration")),
                                resultSet.getInt("to_complete"),
                                resultSet.getInt("completed"),
                                resultSet.getDouble("price"),
                                resultSet.getDouble("cost"),
                                (resultSet.getInt("type") == 1 ? OrderType.LIMIT_BUY : OrderType.LIMIT_SELL)
                        )
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
