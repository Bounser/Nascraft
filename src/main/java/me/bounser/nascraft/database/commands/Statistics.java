package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

}
