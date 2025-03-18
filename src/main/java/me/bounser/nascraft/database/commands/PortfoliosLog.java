package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.market.unit.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class PortfoliosLog {

    public static void logContribution(Connection connection, UUID uuid, Item item, int amount) {

        try {
            String sql1 = "SELECT contribution, amount, day FROM portfolios_log WHERE uuid=? AND identifier=? ORDER BY day DESC LIMIT 1;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            prep1.setString(2, item.getIdentifier());
            ResultSet resultSet = prep1.executeQuery();

            if (resultSet.next()) {
                if (resultSet.getInt("day") == NormalisedDate.getDays()) {
                    String sql2 = "UPDATE portfolios_log SET contribution=?, amount=? WHERE uuid=? AND identifier=? AND day=?;";
                    PreparedStatement prep2 =  connection.prepareStatement(sql2);
                    prep2.setDouble(1, item.getPrice().getValue()*amount + resultSet.getDouble("contribution"));
                    prep2.setInt(2, amount + resultSet.getInt("amount"));
                    prep2.setString(3, uuid.toString());
                    prep2.setString(4, item.getIdentifier());
                    prep2.setInt(5, NormalisedDate.getDays());
                    prep2.executeUpdate();
                } else {
                    String sql2 = "INSERT INTO portfolios_log (uuid, identifier, amount, contribution, day) VALUES (?,?,?,?,?);";
                    PreparedStatement prep2 = connection.prepareStatement(sql2);
                    prep2.setString(1, uuid.toString());
                    prep2.setString(2, item.getIdentifier());
                    prep2.setInt(3, amount + resultSet.getInt("amount"));
                    prep2.setDouble(4, item.getPrice().getValue()*amount + resultSet.getDouble("contribution"));
                    prep2.setInt(5, NormalisedDate.getDays());
                    prep2.executeUpdate();
                }
            } else {
                String sql2 = "INSERT INTO portfolios_log (uuid, identifier, amount, contribution, day) VALUES (?,?,?,?,?);";
                PreparedStatement prep2 = connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setString(2, item.getIdentifier());
                prep2.setInt(3, amount);
                prep2.setDouble(4, item.getPrice().getValue()*amount);
                prep2.setInt(5, NormalisedDate.getDays());
                prep2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logWithdraw(Connection connection, UUID uuid, Item item, int amount) {

        try {
            String sql1 = "SELECT contribution, amount, day FROM portfolios_log WHERE uuid=? AND identifier=? ORDER BY day DESC LIMIT 1;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            prep1.setString(2, item.getIdentifier());
            ResultSet resultSet = prep1.executeQuery();

            if (!resultSet.next()) return;

            if (resultSet.getInt("amount") == amount || resultSet.getInt("amount") < amount) {
                String sql = "DELETE FROM portfolios_log WHERE uuid=? AND identifier=? AND day=?;";
                PreparedStatement prep = connection.prepareStatement(sql);
                prep.setString(1, uuid.toString());
                prep.setString(2, item.getIdentifier());
                prep.setInt(3, NormalisedDate.getDays());
                prep.executeUpdate();

            } else if (resultSet.getInt("day") == NormalisedDate.getDays()) {

                String sql2 = "UPDATE portfolios_log SET amount=?, contribution=? WHERE uuid=? AND identifier=?;";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setInt(1, resultSet.getInt("amount") - amount);
                prep2.setDouble(2, amount * resultSet.getDouble("contribution") / (float) resultSet.getInt("amount"));
                prep2.setString(3, uuid.toString());
                prep2.setString(4, item.getIdentifier());
                prep2.executeUpdate();

            } else {
                String sql2 = "INSERT INTO portfolios_log (uuid, identifier, amount, contribution) VALUES (?,?,?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setString(2, item.getIdentifier());
                prep2.setInt(3, resultSet.getInt("amount") - amount);
                prep2.setDouble(4, amount * resultSet.getDouble("contribution") / (float) resultSet.getInt("amount"));
                prep2.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<Integer, Double> getContributionChangeEachDay(Connection connection, UUID uuid) {

        try {
            HashMap<Integer, Double> dayAndContribution = new HashMap<>();

            String sql1 = "SELECT contribution, amount, day FROM portfolios_log WHERE uuid=? ORDER BY day DESC;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            ResultSet resultSet = prep1.executeQuery();

            while (resultSet.next()) {
                int day = resultSet.getInt("day");

                if (dayAndContribution.containsKey(day)) dayAndContribution.put(day, dayAndContribution.get(day) + resultSet.getDouble("contribution"));
                else dayAndContribution.put(resultSet.getInt("day"), resultSet.getDouble("contribution"));
            }

            return dayAndContribution;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(Connection connection, UUID uuid) {

        try {
            HashMap<Integer, HashMap<String, Integer>> dayAndComposition = new HashMap<>();

            String sql1 = "SELECT identifier, amount, day FROM portfolios_log WHERE uuid=? ORDER BY day DESC;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            ResultSet resultSet = prep1.executeQuery();

            while (resultSet.next()) {
                String identifier = resultSet.getString("identifier");
                int day = resultSet.getInt("day");
                int amount = resultSet.getInt("amount");

                if (dayAndComposition.containsKey(day)) {

                    HashMap<String, Integer> composition = dayAndComposition.get(day);

                    composition.put(identifier, amount);

                    dayAndComposition.put(day, composition);

                } else {
                    HashMap<String, Integer> composition = new HashMap<>();

                    composition.put(identifier, amount);

                    dayAndComposition.put(day, composition);
                }
            }
            return dayAndComposition;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getFirstDay(Connection connection, UUID uuid) {

        try {
            String sql = "SELECT MIN(day) AS first_day FROM portfolios_log WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("first_day");
            } else {
                return NormalisedDate.getDays();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
