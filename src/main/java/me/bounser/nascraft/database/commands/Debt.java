package me.bounser.nascraft.database.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class Debt {

    public static void increaseDebt(Connection connection, UUID uuid, double debt) {

        try {
            String sql1 = "SELECT debt FROM loans WHERE uuid=?;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            ResultSet resultSet = prep1.executeQuery();

            if(resultSet.next()) {
                String sql2 = "UPDATE loans SET debt=? WHERE uuid=?;";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setDouble(1, debt + resultSet.getDouble("debt"));
                prep2.setString(2, uuid.toString());
                prep2.executeUpdate();
            } else {
                String sql2 = "INSERT INTO loans (uuid, debt) VALUES (?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setDouble(2, debt);
                prep2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void decreaseDebt(Connection connection, UUID uuid, double debt) {

        try {
            String sql1 = "SELECT debt FROM loans WHERE uuid=?;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            ResultSet resultSet = prep1.executeQuery();

            if(resultSet.next()) {

                if (resultSet.getDouble("debt") - debt <= 0) {

                    String sql2 = "DELETE FROM loans WHERE uuid=?;";
                    PreparedStatement prep2 =  connection.prepareStatement(sql2);
                    prep2.setString(1, uuid.toString());
                    prep2.executeUpdate();

                } else {
                    String sql2 = "UPDATE loans SET debt=? WHERE uuid=?;";
                    PreparedStatement prep2 =  connection.prepareStatement(sql2);
                    prep2.setDouble(1, resultSet.getDouble("debt") - debt);
                    prep2.setString(2, uuid.toString());
                    prep2.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static double getDebt(Connection connection, UUID uuid) {

        try {
            String sql1 = "SELECT debt FROM loans WHERE uuid=?;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            ResultSet resultSet = prep1.executeQuery();

            if(resultSet.next()) {
                return resultSet.getDouble("debt");
            } else {
                return 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<UUID, Double> getUUIDAndDebt(Connection connection) {
        HashMap<UUID, Double> debtors = new HashMap<>();

        try {
            String sql = "SELECT uuid, debt FROM loans WHERE debt > 0;";

            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();

            while (rs.next()) {
                String uuidString = rs.getString("uuid");
                UUID uuid = UUID.fromString(uuidString);
                Double debt = rs.getDouble("debt");
                debtors.put(uuid, debt);
            }

            return debtors;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addInterestPaid(Connection connection, UUID uuid, Double interest) {

        try {
            String sql1 = "SELECT paid FROM interests WHERE uuid=?;";
            PreparedStatement prep1 =  connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            ResultSet resultSet = prep1.executeQuery();

            if(resultSet.next()) {
                String sql2 = "UPDATE interests SET paid=? WHERE uuid=?;";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setDouble(1, interest + resultSet.getDouble("paid"));
                prep2.setString(2, uuid.toString());
                prep2.executeUpdate();
            } else {
                String sql2 = "INSERT INTO interests (uuid, paid) VALUES (?,?);";
                PreparedStatement prep2 =  connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setDouble(2, interest);
                prep2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static HashMap<UUID, Double> getUUIDAndInterestsPaid(Connection connection) {
        HashMap<UUID, Double> payers = new HashMap<>();

        try {
            String sql = "SELECT uuid, paid FROM interests;";

            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();

            while (rs.next()) {
                String uuidString = rs.getString("uuid");
                UUID uuid = UUID.fromString(uuidString);
                Double debt = rs.getDouble("paid");
                payers.put(uuid, debt);
            }

            return payers;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static double getInterestsPaid(Connection connection, UUID uuid) {

        try {
            String sql = "SELECT paid FROM interests WHERE uuid=?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet rs = prep.executeQuery();

            if (rs.next())
                return rs.getDouble("paid");

            return 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static double getAllOutstandingDebt(Connection connection) {

        try {
            String sql = "SELECT SUM(debt) AS total_debt FROM loans;;";

            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();

            if (rs.next())
                return rs.getDouble("total_debt");

            return 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static double getAllInterestsPaid(Connection connection) {

        try {
            String sql = "SELECT SUM(paid) AS total_paid FROM interests;;";

            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();

            if (rs.next())
                return rs.getDouble("total_paid");

            return 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
