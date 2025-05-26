package me.bounser.nascraft.database.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Discord {

    public static void saveDiscordLink(Connection connection, UUID uuid, String userId, String nickname) {
        try {
            String sql = "INSERT INTO discord (userid, uuid, nickname) VALUES (?,?,?);";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.setString(2, uuid.toString());
            prep.setString(3, nickname);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeLink(Connection connection, UUID uuid) {
        try {
            String sql = "DELETE FROM discord WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static UUID getUUIDFromUserid(Connection connection, String userId) {
        try {
            String sql = "SELECT uuid FROM discord WHERE userid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) { return UUID.fromString(resultSet.getString("uuid")); }
            else { return null; }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDiscordUserId(Connection connection, UUID uuid) {
        try {
            String sql = "SELECT userid FROM discord WHERE uuid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, uuid.toString());
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("userid");
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getNicknameFromUserId(Connection connection, String userId) {
        try {
            String sql = "SELECT nickname FROM discord WHERE userid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) { return resultSet.getString("nickname"); }
            else { return null; }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
