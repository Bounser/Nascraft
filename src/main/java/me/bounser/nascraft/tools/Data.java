package me.bounser.nascraft.tools;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.managers.resources.Category;
import me.bounser.nascraft.market.managers.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Data {

    /*
      Data info:

       Location: Nascraft/data/Price_History_(Material).json
       Price_History_(Material).json: (Simplified)
       Material:
           lastSavedD: 67
           lastSavedH: 14
           stock: 100
           recent:
             1:
               price: 12.2
             2:
               price: 15.3
             3:
               price: 12.3
               ...

             24:
               price: 45.3
           history:
             # Days from 01/01/2023
             86:
               price: 10
               stock: 58
             87:
               price: 10
               stock: 58
             ...
     */

    private static Data instance;

    public static Data getInstance() { return instance == null ? instance = new Data() : instance; }

    public void setupFiles(List<Category> categories) {

        int days = getDay();
        int hour = getHour();

        for (Category cat : categories) {

            for (Item item : cat.getItems()) {
                String mat = item.getMaterial();

                Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

                if (!(json.contains(mat + ".lastSaveD")) && !json.contains(mat + ".lastSaveD")) {

                    float price = Config.getInstance().getInitialPrice(mat);

                    // HISTORY
                    json.set(mat + ".lastSaveD", days);

                    json.set(mat + ".history." + days + ".price", price);
                    json.set(mat + ".history." + days + ".stock", 100);

                    // RECENT
                    json.set(mat + ".lastSaveH", hour);
                    for (int i = 1; i <= 24 ; i++) {
                        json.set(mat + ".recent." + (24 - i) + ".price", price);
                    }
                }
            }
        }
    }

    public void savePrices() {

        int day = getDay();
        int hour = getHour();

        for (Item item : MarketManager.getInstance().getAllItems()) {

            Json json = new Json("Price-History-" + item.getMaterial(), Nascraft.getInstance().getDataFolder().getPath() + "/data");

            // HISTORY
            json.set(item.getMaterial() + ".lastSaveD", day);
            json.set(item.getMaterial() + ".lastSaveH", hour);

            json.set(item.getMaterial() + ".history." + day + ".price", item.getPrice().getValue());
            json.set(item.getMaterial() + ".history." + day + ".stock", item.getPrice().getStock());

            // RECENT
            int x = 0;
            for (float i : item.getPrices(TimeSpan.DAY)) {
                if (hour - x < 0) {
                    json.set(item.getMaterial() + ".recent." + (hour + 23 - x) + ".price", i);
                } else {
                    json.set(item.getMaterial() + ".recent." + (hour - x) + ".price", i);
                }
                x++;
            }
        }
    }

    // Returns the last price repeated 30 times in a list.
    public List<Float> getMPrice(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        if (json.contains(mat + ".lastSaveD") && json.contains(mat + ".lastSaveH")) {
            float price = (float) json.getDouble(mat + ".recent." + json.getInt(mat + ".lastSaveH") + ".price");
            return new ArrayList<>(Collections.nCopies(30, price));
        } else {
            float price = Config.getInstance().getInitialPrice(mat);
            return new ArrayList<>(Collections.nCopies(30, price));
        }
    }

    // Returns a list with the prices of the 24 prev hours
    public List<Float> getHPrice(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (json.contains(mat + ".lastSaveD") && json.contains(mat + ".lastSaveH")) {

            int day = getDay();

            int lastDay = json.getInt(mat + ".lastSaveD");
            int lastHour = json.getInt(mat + ".lastSaveH");

            List<Float> prices = new ArrayList<>();

            if (lastDay == day) {
                if (lastHour == hour) {
                    lastHour++;
                    for (int i = lastHour ; i <= 23 ; i++) {
                        prices.add(json.getFloat(mat + ".recent." + i + ".price"));
                    }
                    for (int i = 0 ; i < lastHour ; i++) {
                        prices.add(json.getFloat(mat + ".recent." + i + ".price"));
                    }
                    return prices;
                }
                if (hour - lastHour > 24) {
                    return new ArrayList<>(Collections.nCopies(24, json.getFloat(mat + ".recent." + lastHour+ ".price")));

                } else {
                    for (int i = 1; i <= hour-lastHour; i++) {
                        prices.add(json.getFloat(mat + ".recent." + lastHour+ ".price"));
                    }
                    for (int i = lastHour ; i > 0 ; i--) {
                        if(prices.size() < 24) prices.add(json.getFloat(mat + ".recent." + i+ ".price"));
                    }
                    if (prices.size() < 24)
                        for(int i = 24 ; i > lastHour ; i--) if(prices.size() < 24) prices.add(json.getFloat(mat + ".recent." + i+ ".price"));
                    return prices;
                }
            }
            return new ArrayList<>(Collections.nCopies(24, json.getFloat(mat + ".recent." + lastHour+ ".price")));
        } else {

            float price = Config.getInstance().getInitialPrice(mat);
            for (int i = 1; i <= 24 ; i++) {
                json.set(mat + ".recent." + (24 - i) + ".price", price);
            }
            json.set(mat + ".lastSaveH", hour);
            return new ArrayList<>(Collections.nCopies(24, price));
        }
    }

    // Returns a list with 30 prices of the last 30 days
    public List<Float> getMMPrice(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        int day = getDay();

        if (json.contains(mat + ".lastSaveD")) {

            List<Float> prices = new ArrayList<>();

            for (int i = 29 ; i >= 0 ; i--) {

                if (json.contains(mat + ".history." + (day - i) + ".price")) {

                    prices.add(json.getFloat(mat + ".history." + (day - i) + ".price"));

                } else {
                    if (!json.contains(mat + ".history." + (day - i -1) + ".price")) {
                            prices.add(Config.getInstance().getInitialPrice(mat));
                    } else {
                        prices.add((prices.get(prices.size()-1)));
                    }
                }
            }
            return prices;

        } else {

            float price = Config.getInstance().getInitialPrice(mat);
            for (int i = 0; i < 30 ; i++) {
                json.set(mat + ".history." + (day - i) + ".price", price);
            }
            json.set(mat + ".lastSaveD", day);
            return new ArrayList<>(Collections.nCopies(30, price));
        }
    }

    // Returns a list with 30 prices of the last 365 days
    public List<Float> getYPrice(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        int day = getDay();

        if (json.contains(mat + ".lastSaveD")) {

            List<Float> prices = new ArrayList<>();

            for (int i = 29 ; i >= 0 ; i--) {

                if (json.contains(mat + ".history." + (day - i*15) + ".price")) {

                    prices.add(json.getFloat(mat + ".history." + (day - i*15) + ".price"));

                } else {
                    if (!json.contains(mat + ".history." + (day - (i-1)*15) + ".price")) {
                        prices.add(Config.getInstance().getInitialPrice(mat));
                    } else {
                        prices.add((prices.get(prices.size()-1)));
                    }
                }
            }
            return prices;

        } else {

            float price = Config.getInstance().getInitialPrice(mat);
            return new ArrayList<>(Collections.nCopies(24, price));
        }
    }

    public int getStock(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        if (json.contains(mat + ".lastSaveD")) {
            return json.getInt(mat + ".history." + json.getInt(mat + ".lastSaveD") + ".stock");
        } else {
            return 0;
        }
    }

    // Returns an int representing the days that passed since the first day of 2023.
    public int getDay() {

        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        LocalDateTime end = LocalDateTime.now();

        Duration diff = Duration.between(start, end);
        return (int) diff.toDays();
    }

    // Returns the hour of the day.
    public int getHour() {

        Calendar calendar = Calendar.getInstance();

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

}
