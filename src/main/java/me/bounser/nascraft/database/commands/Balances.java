package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Balances {

    private static final Logger log = Nascraft.getInstance().getLogger();

    public static void updateBalance(Connection connection, UUID uuid) {

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || player.isOp()) {
            return;
        }

        try {
            double currentBalance = MoneyManager.getInstance().getBalance(player, CurrenciesManager.getInstance().getDefaultCurrency());
            double pastBalance = 0.0;
            boolean playerExistsInDb = false;

            String sqlSelectBalance = "SELECT balance FROM balances WHERE uuid = ?;";
            try (PreparedStatement prepSelect = connection.prepareStatement(sqlSelectBalance)) {
                prepSelect.setString(1, uuid.toString());
                try (ResultSet resultSetPast = prepSelect.executeQuery()) {
                    if (resultSetPast.next()) {
                        pastBalance = resultSetPast.getDouble("balance");
                        playerExistsInDb = true;
                    }
                }
            }

            if (playerExistsInDb) {
                if (currentBalance != pastBalance) {
                    String sqlUpdateBalance = "UPDATE balances SET balance = ? WHERE uuid = ?;";
                    try (PreparedStatement prepUpdate = connection.prepareStatement(sqlUpdateBalance)) {
                        prepUpdate.setDouble(1, currentBalance);
                        prepUpdate.setString(2, uuid.toString());
                        int rowsAffected = prepUpdate.executeUpdate();
                        if (rowsAffected == 0) {
                            log.warning("Failed to update balance for existing player: " + uuid + ". No rows affected.");
                        }
                    }
                } else {
                    return;
                }
            } else {
                String sqlInsertBalance = "INSERT INTO balances (uuid, balance) VALUES (?, ?);";
                try (PreparedStatement prepInsert = connection.prepareStatement(sqlInsertBalance)) {
                    prepInsert.setString(1, uuid.toString());
                    prepInsert.setDouble(2, currentBalance);
                    prepInsert.executeUpdate();
                }
            }

            double balanceDifference = currentBalance - pastBalance;

            if (balanceDifference == 0.0) {
                return;
            }

            int today = NormalisedDate.getDays();
            Integer targetDay = null;
            double currentSupply = 0.0;
            boolean update = false;

            String sqlFindSupply = "SELECT day, supply FROM money_supply WHERE day <= ? ORDER BY day DESC LIMIT 1;";
            try (PreparedStatement prepFindSupply = connection.prepareStatement(sqlFindSupply)) {
                prepFindSupply.setInt(1, today);
                try (ResultSet rsSupply = prepFindSupply.executeQuery()) {
                    if (rsSupply.next()) {
                        targetDay = rsSupply.getInt("day");
                        currentSupply = rsSupply.getDouble("supply");
                        if (targetDay == today) update = true;
                    }
                }
            }

            if (update) {
                double newSupply = currentSupply + balanceDifference;

                String sqlUpdateSupply = "UPDATE money_supply SET supply = ? WHERE day = ?;";
                try (PreparedStatement prepUpdateSupply = connection.prepareStatement(sqlUpdateSupply)) {
                    prepUpdateSupply.setDouble(1, newSupply);
                    prepUpdateSupply.setInt(2, targetDay);
                    prepUpdateSupply.executeUpdate();
                }

            } else {
                String sqlInsertSupply = "INSERT INTO money_supply (day, supply) VALUES (?, ?);";
                try (PreparedStatement prepInsertSupply = connection.prepareStatement(sqlInsertSupply)) {
                    prepInsertSupply.setInt(1, today);
                    prepInsertSupply.setDouble(2, balanceDifference);
                    prepInsertSupply.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database operation failed for player " + uuid, e);
        }
    }

    public static Map<Integer, Double> getMoneySupplyHistory(Connection connection) throws SQLException {
        Map<Integer, Double> supplyHistory = new HashMap<>();
        String sql = "SELECT day, supply FROM money_supply ORDER BY day ASC;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int day = rs.getInt("day");
                double supply = rs.getDouble("supply");
                supplyHistory.put(day, supply); // Add the entry to the map
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to retrieve money supply history from database.", e);
            throw e;
        }

        return supplyHistory;
    }


}
