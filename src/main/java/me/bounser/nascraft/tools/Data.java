package me.bounser.nascraft.tools;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Data {

    /*
     * Data structure:
     *
     *  Location: Nascraft/data/Price_History_(Material).json
     *  Price_History_(Material).json: (Simplified)
     *  Material:
     *      lastSavedD: 67
     *      lastSavedH: 14
     *      stock: 100
     *      recent:
     *        1:
     *          price: 12.2
     *        2:
     *          price: 15.3
     *        3:
     *          price: 12.3
     *          ...
     *
     *        24:
     *          price: 45.3
     *      history:
     *        # Days from 01/01/2023
     *        86:
     *          price: 10
     *          stock: 58
     *        87:
     *          price: 10
     *          stock: 58
     *        ...
     *
     *  Location: Nascraft/data/Trading_data/UUID1
     *  UUID1.json:
     *      assets:
     *        num: 3
     *        1:
     *          mat: Stone
     *          q: 100
     *          mp: 11
     *        2:
     *          mat: Diamond
     *          q: 2
     *          mp: 54.2
     */

    private static Data instance;

    public static Data getInstance() { return instance == null ? instance = new Data() : instance; }

    public int getNum(String owneruuid) {

        Json json = new Json(owneruuid, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        return json.getInt(owneruuid + ".num");
    }

    public void setupFiles() {

        int days = getDay();
        int hour = getHour();

        for(String cat : Config.getInstance().getCategories())
        for(String mat : Config.getInstance().getAllMaterials(cat)) {

            boolean first = false;

            Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

            if(!(json.contains(mat + ".lastSaveD")) && !json.contains(mat + ".lastSaveD")) {
                float price = Config.getInstance().getInitialPrice(mat);

                json.set(mat + ".lastSaveD", days);
                json.set(mat + ".lastSaveH", hour);

                json.set(mat + ".history." + days + ".price", price);
                json.set(mat + ".history." + days + ".stock", 100);

                // RECENT
                for(int i = 1; i <= 24 ; i++) {
                        json.set(mat + ".recent." + (25 - i) + ".price", price);

                }
            }
        }
    }

    public void savePrices() {

        int days = getDay();
        int hour = getHour();

        for(Item item : MarketManager.getInstance().getAllItems()) {

            boolean first = false;

            Bukkit.broadcastMessage("guardando " + item.getMaterial());
            Json json = new Json("Price-History-" + item.getMaterial(), Nascraft.getInstance().getDataFolder().getPath() + "/data");

            if(!json.contains(item.getMaterial() + ".lastSaveD")) {
                first = true;
                item.setStock(100);
            }

            // HISTORY
            float price;
            if(first) {
                price = Config.getInstance().getInitialPrice(item.getMaterial());
            } else {
                price = item.getPrice();
            }
            Bukkit.broadcastMessage("First: " + first + " Price:" + item.getPrice() + " stock:" + item.getStock());
            json.set(item.getMaterial() + ".lastSaveD", days);
            json.set(item.getMaterial() + ".lastSaveH", hour);

            json.set(item.getMaterial() + ".history." + days + ".price", price);
            json.set(item.getMaterial() + ".history." + days + ".stock", item.getStock());

            /*
            // RECENT
            if(first){
                for(int i = 1; i <= 24 ; i++) {
                    json.set(item.getMaterial() + ".recent." + (25 - i) + ".price", price);
                }
                return;
            }
            int x = 0;
            for(float i : item.getPricesH()) {
                if(hour - x < 1) {
                    json.set(item.getMaterial() + ".recent." + (hour + 24 - x) + ".price", i);
                } else {
                    json.set(item.getMaterial() + ".recent." + (hour - x) + ".price", i);
                }
                x++;
            }
            */
        }
    }

    // Only called on item creation
    public List<Float> getHPrice(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (json.contains(mat + ".lastSavedD") && json.contains(mat + ".lastSavedH")) {

            int day = getDay();

            int lastDay = json.getInt(mat + ".lastSavedD");
            int lastHour = json.getInt(mat + ".lastSavedH");

            List<Float> prices = new ArrayList<>();

            if(lastDay == day) {
                if(lastHour == hour) {
                    for(int i = lastHour ; i > 0 ; i--) {
                        prices.add(json.getFloat(mat + ".recent." + i + ".price"));
                    }
                    for(int i = 24 ; i > lastHour ; i--) {
                        prices.add(json.getFloat(mat + ".recent." + i + ".price"));
                    }
                }
                if(hour - lastHour > 24) {

                    return new ArrayList<>(Collections.nCopies(24, json.getFloat(mat + ".recent." + lastHour+ ".price")));

                } else {
                    for(int i = 1; i <= hour-lastHour; i++) {
                        prices.add(json.getFloat(mat + ".recent." + lastHour+ ".price"));
                    }
                    for(int i = lastHour ; i > 0 ; i--) {
                        if(prices.size() < 24) prices.add(json.getFloat(mat + ".recent." + i+ ".price"));
                    }
                    if(prices.size() < 24)
                        for(int i = 24 ; i > lastHour ; i--) {
                        if(prices.size() < 24) prices.add(json.getFloat(mat + ".recent." + i+ ".price"));
                        }
                }
                int x = 0;
                for(float i : MarketManager.getInstance().getItem(mat).getPricesH()) {
                    if(hour - x < 1) {
                        json.set(mat + ".recent." + (hour + 24 - x) + ".price", i);
                    } else {
                        json.set(mat + ".recent." + (hour - x) + ".price", i);
                    }
                    x++;
                }
                json.set(mat + ".lastSaveH", hour);
                return prices;
            }
            return new ArrayList<>(Collections.nCopies(24, json.getFloat(mat + ".recent." + lastHour+ ".price")));
        } else {
            for(int i = 1; i <= 24 ; i++) {
                json.set(mat + ".recent." + (25 - i) + ".price", Config.getInstance().getInitialPrice(mat));
            }
            json.set(mat + ".lastSaveH", hour);
            return new ArrayList<>(Collections.nCopies(24, Config.getInstance().getInitialPrice(mat)));
        }
    }

    public List<Float> getMPrice(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");

        if (json.contains(mat + ".lastSavedD") && json.contains(mat + ".lastSavedH")) {
            return new ArrayList<>(Collections.nCopies(24, json.getFloat(mat + ".recent." + json.getInt(mat + ".lastSavedH"))));
        } else {
            return new ArrayList<>(Collections.nCopies(24, Config.getInstance().getInitialPrice(mat)));
        }
    }

    public int getDay() {

        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        LocalDateTime end = LocalDateTime.now();

        Duration diff = Duration.between(start, end);
        return (int) diff.toDays();

    }

    public int getHour() {

        Calendar calendar = Calendar.getInstance();

        return calendar.get(Calendar.HOUR_OF_DAY);

    }

}
