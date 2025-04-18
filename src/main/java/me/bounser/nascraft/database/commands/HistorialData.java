package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class HistorialData {

    public static void saveDayPrice(Connection connection, Item item, Instant instant) {

        try {
            String insert = "INSERT INTO prices_day (day, identifier, date, price, volume) VALUES (?,?,?,?,?);";

            PreparedStatement insertStatement = connection.prepareStatement(insert);

            insertStatement.setInt(1,NormalisedDate. getDays());
            insertStatement.setString(2, item.getIdentifier());
            insertStatement.setString(3, instant.getLocalDateTime().toString());
            insertStatement.setDouble(4, instant.getPrice());
            insertStatement.setInt(5, instant.getVolume());

            insertStatement.executeUpdate();

            String deleteQuery = "DELETE FROM prices_day WHERE day < ?;";

            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);

            deleteStatement.setInt(1, NormalisedDate.getDays()-2);

            deleteStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveMonthPrice(Connection connection, Item item, Instant instant) {

        try {
            String select = "SELECT date FROM prices_month WHERE identifier=? ORDER BY id DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                String insert = "INSERT INTO prices_month (day, date, identifier, price, volume) VALUES (?,?,?,?,?);";

                PreparedStatement insertStatement = connection.prepareStatement(insert);

                insertStatement.setInt(1, NormalisedDate.getDays());
                insertStatement.setString(2, instant.getLocalDateTime().toString());
                insertStatement.setString(3, item.getIdentifier());
                insertStatement.setDouble(4, instant.getPrice());
                insertStatement.setInt(5, instant.getVolume());

                insertStatement.executeUpdate();
            } else if (LocalDateTime.parse(resultSet.getString("date")).isBefore(LocalDateTime.now().minusHours(4))) {

                String selectDay = "SELECT date, price, volume FROM prices_day WHERE identifier=? ORDER BY id DESC LIMIT 48;";

                PreparedStatement preparedStatementDay = connection.prepareStatement(selectDay);

                preparedStatementDay.setString(1, item.getIdentifier());

                ResultSet resultSetDay = preparedStatementDay.executeQuery();

                if (!resultSetDay.next()) {

                    String insert = "INSERT INTO prices_month (day, date, identifier, price, volume) VALUES (?,?,?,?,?);";

                    PreparedStatement insertStatement = connection.prepareStatement(insert);

                    insertStatement.setInt(1, NormalisedDate.getDays());
                    insertStatement.setString(2, instant.getLocalDateTime().toString());
                    insertStatement.setString(3, item.getIdentifier());
                    insertStatement.setDouble(4, instant.getPrice());
                    insertStatement.setInt(5, instant.getVolume());

                    insertStatement.executeUpdate();

                } else {

                    double averagePrice = 0;
                    int totalVolume = 0;

                    int i = 0;

                    while (resultSetDay.next()) {
                        if (LocalDateTime.parse(resultSetDay.getString("date")).isAfter(LocalDateTime.now().minusHours(4))) {
                            i++;
                            averagePrice += resultSetDay.getDouble("price");
                            totalVolume += resultSetDay.getInt("volume");
                        }
                    }

                    if (averagePrice == 0) return;

                    String insert = "INSERT INTO prices_month (day, date, identifier, price, volume) VALUES (?,?,?,?,?);";

                    PreparedStatement insertStatement = connection.prepareStatement(insert);

                    insertStatement.setInt(1, NormalisedDate.getDays());
                    insertStatement.setString(2, LocalDateTime.now().minusHours(2).toString());
                    insertStatement.setString(3, item.getIdentifier());
                    insertStatement.setDouble(4, averagePrice/i);
                    insertStatement.setInt(5, totalVolume);

                    insertStatement.executeUpdate();
                }

                String deleteQuery = "DELETE FROM prices_month WHERE day < ?;";

                PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);

                deleteStatement.setInt(1, NormalisedDate.getDays()-31);
                deleteStatement.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveHistoryPrices(Connection connection, Item item, Instant instant) {

        try {
            String select = "SELECT date FROM prices_history WHERE day=? AND identifier=?;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setInt(1, NormalisedDate.getDays());

            preparedStatement.setString(2, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()){

                String selectMonth = "SELECT date, price, volume FROM prices_month WHERE identifier=? ORDER BY id DESC LIMIT 6;";

                PreparedStatement preparedStatementMonth = connection.prepareStatement(selectMonth);

                preparedStatementMonth.setString(1, item.getIdentifier());

                ResultSet resultSetMonth = preparedStatementMonth.executeQuery();

                if (!resultSetMonth.next()) {

                    String insert = "INSERT INTO prices_history (day, date, identifier, price, volume) VALUES (?,?,?,?,?);";

                    PreparedStatement insertStatement = connection.prepareStatement(insert);

                    insertStatement.setInt(1, NormalisedDate.getDays());
                    insertStatement.setString(2, instant.getLocalDateTime().toString());
                    insertStatement.setString(3, item.getIdentifier());
                    insertStatement.setDouble(4, instant.getPrice());
                    insertStatement.setInt(5, instant.getVolume());

                    insertStatement.executeUpdate();

                } else {
                    double averagePrice = 0;
                    int totalVolume = 0;

                    int i = 0;

                    while (resultSetMonth.next()) {
                        if (LocalDateTime.parse(resultSetMonth.getString("date")).isAfter(LocalDateTime.now().minusHours(24))) {
                            i++;
                            averagePrice += resultSetMonth.getDouble("price");
                            totalVolume += resultSetMonth.getInt("volume");
                        }
                    }

                    String insert = "INSERT INTO prices_history (day, date, identifier, price, volume) VALUES (?,?,?,?,?);";

                    PreparedStatement insertStatement = connection.prepareStatement(insert);

                    insertStatement.setInt(1, NormalisedDate.getDays());
                    insertStatement.setString(2, LocalDateTime.now().minusHours(12).toString());
                    insertStatement.setString(3, item.getIdentifier());
                    insertStatement.setDouble(4, averagePrice/i);
                    insertStatement.setInt(5, totalVolume);

                    insertStatement.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static List<Instant> getDayPrices(Connection connection, Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT date FROM prices_day WHERE identifier=? ORDER BY id DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusHours(24), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT date, price, volume FROM prices_day WHERE identifier=? ORDER BY id DESC LIMIT 288;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.parse(resultSet1.getString("date"));

                    double price = resultSet1.getDouble("price");

                    if (time.isAfter(LocalDateTime.now().minusHours(24)) && price != 0) {
                        prices.add(new Instant(
                                time,
                                resultSet1.getDouble("price"),
                                resultSet1.getInt("volume")
                        ));

                    }
                }

                prices.add(0, new Instant(LocalDateTime.now().minusHours(24), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public static List<Instant> getMonthPrices(Connection connection, Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT date FROM prices_month WHERE identifier=? ORDER BY id DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusDays(30), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT date, price, volume FROM prices_month WHERE identifier=? ORDER BY id DESC LIMIT 400;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.parse(resultSet1.getString("date"));

                    if (time.isAfter(LocalDateTime.now().minusDays(30))) {
                        prices.add(new Instant(
                                time,
                                resultSet1.getDouble("price"),
                                resultSet1.getInt("volume")
                        ));

                    }
                }

                prices.add(0, new Instant(LocalDateTime.now().minusDays(30), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public static List<Instant> getYearPrices(Connection connection, Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT day FROM prices_history WHERE identifier=? ORDER BY day DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusDays(365), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT day, price, volume FROM prices_history WHERE identifier=? ORDER BY day DESC LIMIT 385;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.of(2023, 1, 1, 1, 1).plusDays(resultSet1.getInt("day"));

                    if (time.isAfter(LocalDateTime.now().minusDays(365))) {
                        prices.add(new Instant(
                                time,
                                resultSet1.getDouble("price"),
                                resultSet1.getInt("volume")
                        ));

                    }
                }

                prices.add(new Instant(LocalDateTime.now().minusDays(365), 0, 0));
                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public static List<Instant> getAllPrices(Connection connection, Item item) {

        List<Instant> prices = new LinkedList<>();

        try {
            String select = "SELECT day FROM prices_history WHERE identifier=? ORDER BY day DESC LIMIT 1;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {

                prices.add(new Instant(LocalDateTime.now().minusDays(30), 0, 0));
                prices.add(new Instant(LocalDateTime.now().minusMinutes(5), 0, 0));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

            } else {

                String select288 = "SELECT day, price, volume FROM prices_history WHERE identifier=? ORDER BY day DESC;";

                PreparedStatement preparedStatement288 = connection.prepareStatement(select288);

                preparedStatement288.setString(1, item.getIdentifier());

                ResultSet resultSet1 = preparedStatement288.executeQuery();

                while (resultSet1.next()) {

                    LocalDateTime time = LocalDateTime.of(2023, 1, 1, 1, 1);

                    prices.add(new Instant(
                            time.plusDays(resultSet1.getInt("day")),
                            resultSet1.getDouble("price"),
                            resultSet1.getInt("volume")
                    ));
                }

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));

                prices.add(new Instant(LocalDateTime.now(), item.getPrice().getValue(), item.getVolume()));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prices;
    }

    public static Double getPriceOfDay(Connection connection, String identifier, int day) {

        try {
            String select = "SELECT price FROM prices_history WHERE identifier=? AND day=?;";

            PreparedStatement preparedStatement = connection.prepareStatement(select);

            preparedStatement.setString(1, identifier);
            preparedStatement.setInt(2, day);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) return 0.0;

            return resultSet.getDouble("price");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
