package me.bounser.nascraft.database.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DiscordLink {

    public static void saveLink(Connection connection, String userId, UUID uuid, String nickname) {
        try {
            String sql = "INSERT INTO discord_links (userid, uuid, nickname) VALUES (?,?,?);";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.setString(2, uuid.toString());
            prep.setString(3, nickname);
            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeLink(Connection connection, String userId) {
        try {
            String sql = "DELETE FROM discord_links WHERE userid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            prep.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static UUID getUUID(Connection connection, String userId) {
        try {
            String sql = "SELECT uuid FROM discord_links WHERE userid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) { return UUID.fromString(resultSet.getString("uuid")); }
            else { return null; }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getNickname(Connection connection, String userId) {
        try {
            String sql = "SELECT nickname FROM discord_links WHERE userid=?;";
            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, userId);
            ResultSet resultSet = prep.executeQuery();

            if (resultSet.next()) { return resultSet.getString("nickname"); }
            else { return null; }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUserId(Connection connection, UUID uuid) {
        try {
            String sql = "SELECT userid FROM discord_links WHERE uuid=?;";
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

}
