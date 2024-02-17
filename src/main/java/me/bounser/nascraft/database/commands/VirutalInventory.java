package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.UUID;

public class VirutalInventory {


    public VirutalInventory(Connection connection) {

        createTable(connection, "inventories",
                "uuid VARCHAR(36) NOT NULL," +
                        "identifier TEXT," +
                        "amount INT");

        createTable(connection, "capacities",
                "uuid VARCHAR(36) PRIMARY KEY," +
                        "capacity INT");

    }

    public void updateItem(Connection connection, UUID uuid, Item item, int quantity) {
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

    public void removeItem(Connection connection, UUID uuid, Item item) {
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

    public void clearInventory(Connection connection, UUID uuid) {
        try {
            String sql = "DELETE FROM inventories WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCapacity(Connection connection, UUID uuid, int capacity) {
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

    public LinkedHashMap<Item, Integer> retrieveInventory(Connection connection, UUID uuid) {

        LinkedHashMap<Item, Integer> content = new LinkedHashMap<>();

        try {
            String sql = "SELECT identifier, amount FROM inventories WHERE uuid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            while (resultSet.next()) {
                Item item = MarketManager.getInstance().getItem(resultSet.getString("identifier"));

                if (item != null)
                    content.put(item, resultSet.getInt("amount"));

                String sqlDelete = "DELETE FROM inventories WHERE identifier=? AND uuid=?;";
                PreparedStatement prepDelete = connection.prepareStatement(sqlDelete);
                prepDelete.setString(1, resultSet.getString("identifier"));
                prepDelete.setString(2, uuid.toString());
                prepDelete.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return content;
    }

    public int retrieveCapacity(Connection connection, UUID uuid) {
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

}
