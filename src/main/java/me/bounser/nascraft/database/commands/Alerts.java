package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.market.unit.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Alerts {

    public static void addAlert(Connection connection, String userid, Item item, double price) {

        try {
            String sql = "INSERT INTO alerts (day, userid, identifier, price) VALUES (?, ?, ?, ?);";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setInt(1, NormalisedDate.getDays());
            prep.setString(2, userid);
            prep.setString(3, item.getIdentifier());
            prep.setDouble(4, price);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeAlert(Connection connection, String userid, Item item) {

        try {
            String sql = "DELETE FROM alerts WHERE userid = ? AND identifier = ?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userid);
            prep.setString(2, item.getIdentifier());
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void retrieveAlerts(Connection connection) {

        try {
            String sql = "SELECT userid, identifier, price FROM alerts;";
            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet resultSet = prep.executeQuery();

            while (resultSet.next())
                DiscordAlerts.getInstance().setAlert(resultSet.getString("userid"), resultSet.getString("identifier"), resultSet.getDouble("price"));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeAllAlerts(Connection connection, String userId) {

        try {
            String sql = "DELETE FROM alerts WHERE userid=?";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void purgeAlerts(Connection connection) {

        int expiration = Config.getInstance().getAlertsDaysUntilExpired();
        int days = NormalisedDate.getDays();

        try {
            String sql = "DELETE FROM alerts WHERE day < ?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setInt(1, days - expiration);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
