package me.bounser.nascraft.database.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UserNames {

    public static String getNameByUUID(Connection connection, UUID uuid) {
        try {
            String sql = "SELECT name FROM user_names WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("name");
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveOrUpdateNick(Connection connection, UUID uuid, String name) {
        try {
            String sql1 = "SELECT id FROM user_names WHERE uuid=?;";
            PreparedStatement prep1 = connection.prepareStatement(sql1);
            prep1.setString(1, uuid.toString());
            ResultSet resultSet = prep1.executeQuery();

            if (resultSet.next()) {
                String sql2 = "UPDATE user_names SET name=? WHERE uuid=?;";
                PreparedStatement prep2 = connection.prepareStatement(sql2);
                prep2.setString(1, name);
                prep2.setString(2, uuid.toString());
                prep2.executeUpdate();
            } else {
                String sql2 = "INSERT INTO user_names (uuid, name) VALUES (?,?);";
                PreparedStatement prep2 = connection.prepareStatement(sql2);
                prep2.setString(1, uuid.toString());
                prep2.setString(2, name);
                prep2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
