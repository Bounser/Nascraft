package me.bounser.nascraft.tools;

import de.leonhard.storage.Json;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Calendar;

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

    public void savePrices() {

        int days = getDay();

        for(Item item : MarketManager.getInstance().getAllItems()) {
            Json json = new Json("Price-History-" + item.getMaterial(), Nascraft.getInstance().getDataFolder().getPath() + "/data");

            json.set(item.getMaterial() + ".history.lastSave", days);
            json.set(item.getMaterial() + ".history." + days + ".price", item.getPrice());
            json.set(item.getMaterial() + ".history." + days + ".stock", item.getStock());

        }
    }

    public float[] getPrice(String mat) {

        Json json = new Json("Price-History-" + mat, Nascraft.getInstance().getDataFolder().getPath() + "/data");
        if (json.contains(mat + ".lastSavedD") && json.contains(mat + ".lastSavedH")) {

            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int day = getDay();

            int lastDay = json.getInt(mat + ".lastSavedD");

            return new float[] {(float) json.getDouble(mat + ".recent." + json.getInt(mat + ".lastSavedH")), (hour - json.getInt(mat + ".lastSavedH")) + (day - lastDay)*24};

        } else {
            return new float[] {Config.getInstance().getInitialPrice(mat), -1};
        }
    }

    public int getDay() {

        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);

        LocalDateTime end = LocalDateTime.now();

        Duration diff = Duration.between(start, end);
        return (int) diff.toDays();

    }

}
