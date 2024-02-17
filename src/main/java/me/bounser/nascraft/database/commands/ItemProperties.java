package me.bounser.nascraft.database.commands;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemProperties {

    public void saveItem(Connection connection, Item item) {
        try {
            String sql = "UPDATE items SET lastprice=?, lowest=?, highest=?, stock=?, taxes=? WHERE identifier=?;";
            PreparedStatement prep = connection.prepareStatement(sql);

            prep.setFloat(1, item.getPrice().getValue());
            prep.setFloat(2, item.getPrice().getHistoricalLow());
            prep.setFloat(3, item.getPrice().getHistoricalHigh());
            prep.setFloat(4, item.getPrice().getStock());
            prep.setFloat(5, item.getCollectedTaxes());

            prep.setString(6, item.getIdentifier());

            prep.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void retrieveItem(Connection connection, Item item) {

        try {
            String sql = "SELECT lowest, highest, stock, taxes FROM items WHERE identifier=?;";

            PreparedStatement prep = connection.prepareStatement(sql);
            prep.setString(1, item.getIdentifier());
            ResultSet rs = prep.executeQuery();

            if (rs.next()) {
                item.getPrice().setStock(rs.getInt("stock"));
                item.getPrice().setHistoricalHigh(rs.getFloat("highest"));
                item.getPrice().setHistoricalLow(rs.getFloat("lowest"));
                item.setCollectedTaxes(rs.getFloat("taxes"));
            } else {
                String sqlinsert = "INSERT INTO items (identifier, lastprice, lowest, highest, stock, taxes) VALUES (?,?,?,?,?,?);";

                PreparedStatement insertPrep = connection.prepareStatement(sqlinsert);
                insertPrep.setString(1, item.getIdentifier());
                insertPrep.setFloat(2, Config.getInstance().getInitialPrice(item.getIdentifier()));
                insertPrep.setFloat(3, Config.getInstance().getInitialPrice(item.getIdentifier()));
                insertPrep.setFloat(4, Config.getInstance().getInitialPrice(item.getIdentifier()));
                insertPrep.setFloat(5, 0);
                insertPrep.setFloat(6, 0);

                item.getPrice().setStock(0);
                item.getPrice().setHistoricalHigh(Config.getInstance().getInitialPrice(item.getIdentifier()));
                item.getPrice().setHistoricalLow(Config.getInstance().getInitialPrice(item.getIdentifier()));
                item.setCollectedTaxes(0);

                insertPrep.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void savePrices(Connection connection, Item item) {

        try {
            String updateSQL = "UPDATE prices SET date=?, dayprices=?, monthprices=?, yearprices=? WHERE identifier=?;";

            PreparedStatement preparedStatement = connection.prepareStatement(updateSQL);

            preparedStatement.setString(1, NormalisedDate.formatDateTime(LocalDateTime.now()));
            preparedStatement.setString(2, item.getPrices(TimeSpan.DAY).toString());
            preparedStatement.setString(3, item.getPrices(TimeSpan.MONTH).toString());
            preparedStatement.setString(4, item.getPrices(TimeSpan.YEAR).toString());
            preparedStatement.setString(5, item.getIdentifier());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void retrievePrices(Connection connection, Item item) {

        try {

            String selectSQL = "SELECT date, dayprices, monthprices, yearprices FROM prices WHERE identifier=?;";
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {

                List<Float> dayprices = parseList(resultSet.getString("dayprices"));
                List<Float> monthprices = parseList(resultSet.getString("monthprices"));
                List<Float> yearprices = parseList(resultSet.getString("yearprices"));

                LocalDateTime date = NormalisedDate.parseDateTime(resultSet.getString("date"));

                Duration duration = Duration.between(date, LocalDateTime.now());

                if (duration.toHours() > 1) {
                    addToPriceList(dayprices, dayprices.get(dayprices.size()-1), Math.round(duration.toHours()));
                    addToPriceList(monthprices, monthprices.get(monthprices.size()-1), Math.round(duration.toDays()));
                    addToPriceList(yearprices, yearprices.get(yearprices.size()-1), (int) (duration.toDays()/7));
                }

                item.setPrice(TimeSpan.DAY, dayprices);
                item.setPrice(TimeSpan.MONTH, monthprices);
                item.setPrice(TimeSpan.YEAR, yearprices);
            } else {

                String insertSQL = "INSERT INTO prices (identifier, date, dayprices, monthprices, yearprices) VALUES (?,?,?,?,?);";

                PreparedStatement statement = connection.prepareStatement(insertSQL);

                statement.setString(1, item.getIdentifier());
                statement.setString(2, NormalisedDate.formatDateTime(LocalDateTime.now()));
                statement.setString(3, Collections.nCopies(48, Config.getInstance().getInitialPrice(item.getIdentifier())).toString());
                statement.setString(4, Collections.nCopies(30, Config.getInstance().getInitialPrice(item.getIdentifier())).toString());
                statement.setString(5, Collections.nCopies(51, Config.getInstance().getInitialPrice(item.getIdentifier())).toString());

                statement.executeUpdate();
            }

            resultSet.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public float retrieveLastPrice(Connection connection, Item item) {
        try {
            String selectSQL = "SELECT lastprice FROM items WHERE identifier = ?;";
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);

            preparedStatement.setString(1, item.getIdentifier());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getFloat("lastprice");
            } else {
                return Config.getInstance().getInitialPrice(item.getIdentifier());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addToPriceList(List<Float> prices, Float valueToAdd, int times) {
        for (int i = 0; i < times; i++) {
            prices.add(valueToAdd);
            prices.remove(0);
        }
    }

    private List<Float> parseList(String listString) {
        String[] tokens = listString.substring(1, listString.length() - 1).split(",");
        List<Float> list = new ArrayList<>();
        for (String token : tokens)
            list.add(Float.parseFloat(token.trim()));

        return list;
    }


}
