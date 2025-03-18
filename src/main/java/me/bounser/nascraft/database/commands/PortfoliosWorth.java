package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.portfolio.PortfoliosManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

public class PortfoliosWorth {

    public static void saveOrUpdateWorth(Connection connection, UUID uuid, int day, double worth) {
        try {
            String sql1 = "SELECT id FROM portfolios_worth WHERE uuid=? AND day=?;";
            PreparedStatement prep1 = connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            prep1.setInt(2, day);
            ResultSet resultSet = prep1.executeQuery();

            if (resultSet.next()) {
                String sql2 = "UPDATE portfolios_worth SET worth=? WHERE uuid=? AND day=?;";
                PreparedStatement prep2 = connection.prepareStatement(sql2);
                prep2.setDouble(1, worth);
                prep2.setString(2, uuid.toString());
                prep2.setInt(3, day);
                prep2.executeUpdate();
            } else {
                String sql2 = "INSERT INTO portfolios_worth (uuid, day, worth) VALUES (?,?,?);";
                PreparedStatement prep2 = connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setInt(2, day);
                prep2.setDouble(3, worth);
                prep2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveOrUpdateWorthToday(Connection connection, UUID uuid, double worth) {
        int today = NormalisedDate.getDays();
        saveOrUpdateWorth(connection, uuid, today, worth);
    }

    public static HashMap<UUID, Portfolio> getTopWorth(Connection connection, int n) {
        LinkedHashMap<UUID, Portfolio> result = new LinkedHashMap<>();
        try {
            String sql = "SELECT uuid, worth FROM portfolios_worth WHERE (uuid, day) IN (SELECT uuid, MAX(day) FROM portfolios_worth GROUP BY uuid) ORDER BY worth DESC LIMIT ?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setInt(1, n);
            ResultSet resultSet = prep.executeQuery();

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                result.put(uuid, PortfoliosManager.getInstance().getPortfolio(uuid));
            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static double getLatestWorth(Connection connection, UUID uuid) {
        try {
            String sql = "SELECT worth FROM portfolios_worth WHERE uuid=? ORDER BY day DESC LIMIT 1;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble("worth");
            } else {
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
