package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Statistics {

    public static void saveCPI(Connection connection, float value) {

        try {

            String sql = "SELECT day FROM cpi;";

            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();

            int today = NormalisedDate.getDays();

            while (rs.next()) {
                if (rs.getInt("day") == today) return;
            }

            String sqlinsert = "INSERT INTO cpi (day, date, value) VALUES (?,?,?);";

            PreparedStatement insertPrep = connection.prepareStatement(sqlinsert);
            insertPrep.setInt(1, today);
            insertPrep.setString(2, LocalDateTime.now().toString());
            insertPrep.setFloat(3,value);

            insertPrep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<CPIInstant> getAllCPI(Connection connection) {

        try {

            List<CPIInstant> cpiInstants = new ArrayList<>();

            String sql = "SELECT * FROM cpi;";

            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();

            while (rs.next()) {
                cpiInstants.add(new CPIInstant(rs.getFloat("value"), LocalDateTime.parse(rs.getString("date"))));
            }

            return cpiInstants;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Instant> getPriceAgainstCPI(Connection connection, Item item) {

        try {

            String query = "SELECT MIN(day) AS min_value FROM cpi;";

            PreparedStatement prep = connection.prepareStatement(query);
            ResultSet rs = prep.executeQuery();

            int minValue = -1;

            if (rs.next()) {
                minValue = rs.getInt("min_value");
            }

            if (minValue == -1) {
                return Collections.singletonList(new Instant(LocalDateTime.now(), item.getPrice().getValue(), 0));
            }

            if (NormalisedDate.getDays() - 30 < minValue) {
                return HistorialData.getMonthPrices(connection, item);
            }

            return HistorialData.getAllPrices(connection, item);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addTransaction(Connection connection, double newFlow, double effectiveTaxes) {

        try {

            String query = "SELECT flow, operations, taxes FROM flows WHERE day=?;";

            PreparedStatement prep = connection.prepareStatement(query);
            prep.setInt(1, NormalisedDate.getDays());
            ResultSet rs = prep.executeQuery();

            if (rs.next()) {
                double flow = rs.getFloat("flow");
                double taxes = rs.getFloat("taxes");
                int operations = rs.getInt("operations");

                flow += newFlow;
                taxes += Math.abs(effectiveTaxes);
                operations++;

                String sqlreplace = "REPLACE INTO flows(day, flow, taxes, operations) VALUES (?,?,?,?);";

                PreparedStatement replacePrep = connection.prepareStatement(sqlreplace);
                replacePrep.setInt(1, NormalisedDate.getDays());
                replacePrep.setDouble(2, flow);
                replacePrep.setDouble(3, taxes);
                replacePrep.setInt(4, operations);

                replacePrep.executeUpdate();

            } else {

                String sqlinsert = "INSERT INTO flows (day, flow, taxes, operations) VALUES (?,?,?,?);";

                PreparedStatement insertPrep = connection.prepareStatement(sqlinsert);
                insertPrep.setInt(1, NormalisedDate.getDays());
                insertPrep.setDouble(2, newFlow);
                insertPrep.setDouble(3, effectiveTaxes);
                insertPrep.setInt(4, 1);

                insertPrep.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<DayInfo> getDayInfos(Connection connection) {

        try {

            List<DayInfo> dayInfos = new ArrayList<>();

            String query = "SELECT * FROM flows;";

            PreparedStatement prep = connection.prepareStatement(query);
            ResultSet rs = prep.executeQuery();

            while (rs.next()) {
                dayInfos.add(
                        new DayInfo(
                                rs.getInt("day"),
                                rs.getDouble("flow"),
                                rs.getDouble("taxes")
                        )
                );
            }

            return dayInfos;


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static double getAllTaxesCollected(Connection connection) {
        try {
            String sql = "SELECT taxes FROM flows ORDER BY day DESC LIMIT 1;";

            PreparedStatement prep = connection.prepareStatement(sql);
            ResultSet rs = prep.executeQuery();

            if (rs.next())
                return rs.getDouble("taxes");

            return 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
