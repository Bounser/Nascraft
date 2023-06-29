package me.bounser.nascraft.database;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;

public class Data {

    private final DBType dbType = DBType.valueOf(Config.getInstance().mode());

    private SQLManager sqlManager;

    private static Data instance;

    public static Data getInstance() { return instance == null ? instance = new Data() : instance; }

    public void setupDatabase(List<Category> categories) {

        switch (dbType) {
            case JSON:

                JsonManager.getInstance().setupFiles(categories);

                break;
            case MYSQL:

                if(Config.getInstance().mode().equalsIgnoreCase("mysql")) {
                    sqlManager = new SQLManager();
                    try {
                        sqlManager.connect();
                    } catch (SQLException e) {
                        Nascraft.getInstance().getLogger().warning(ChatColor.RED + "Error while trying to connect to the database! (MySQL)");
                        Bukkit.getPluginManager().disablePlugin(Nascraft.getInstance());
                        e.printStackTrace();
                    }
                }
        }
    }

    public void shutdownDatabase() {

        if(Config.getInstance().mode().equalsIgnoreCase("mysql")) {

            sqlManager.disconnect();

        } else if (Config.getInstance().mode().equalsIgnoreCase("json")){

            JsonManager.getInstance().savePrices();
        }
    }

    public void updateData() throws SQLException {

        switch (dbType) {
            case JSON:
                JsonManager.getInstance().savePrices(); break;
            case MYSQL:
                SQLManager.getInstance().savePrices(); break;

        }
    }

    public List<Float> getPrices(String material, TimeSpan timeSpan) {

        switch (timeSpan) {

            case MINUTE:
                switch (dbType) {
                    case JSON: return JsonManager.getInstance().getMMPrice(material);
                    case MYSQL:
                        try {
                            return SQLManager.getInstance().getMMPrice(material);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
            } break;

            case DAY:
                switch (dbType) {
                    case JSON: return JsonManager.getInstance().getHPrice(material);
                    case MYSQL:
                        try {
                            return SQLManager.getInstance().getHPrice(material);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                } break;

            case MONTH:
                switch (dbType) {
                    case JSON: return JsonManager.getInstance().getMPrice(material);
                    case MYSQL:
                        try {
                            return SQLManager.getInstance().getMPrice(material);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                } break;

            case YEAR:
                switch (dbType) {
                    case JSON: return JsonManager.getInstance().getYPrice(material);
                    case MYSQL:
                        try {
                            return SQLManager.getInstance().getYPrice(material);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                } break;
        }
        return null;
    }

    // Returns an int representing the days that passed since the first day of 2023.
    public static int getDay() {

        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        LocalDateTime end = LocalDateTime.now();

        Duration diff = Duration.between(start, end);
        return (int) diff.toDays();
    }

    // Returns the hour of the day.
    public static int getHour() {

        Calendar calendar = Calendar.getInstance();

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

}
